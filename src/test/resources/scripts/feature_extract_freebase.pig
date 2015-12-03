-- Include JAR
-- REGISTER 'textmining-feature-extraction-1.0-SNAPSHOT-job.jar'

-- Enable compression for intermediate results
SET mapreduce.map.java.opts '-Xmx4G';
SET mapreduce.reduce.java.opts '-Xmx4G';
SET mapred.child.java.opts '-Xmx4G';

--SET pig.tmpfilecompression 'true';
--SET pig.tmpfilecompression.codec 'gz';

SET mapred.job.queue.name 'processing';

-- REGISTER FUNCTIONS
DEFINE FeatureExtractor textmining.wanderlust.feature.extract.pig.udfs.RelationExtractorUDF();
DEFINE FreebaseTypeNormalizer textmining.wanderlust.feature.extract.pig.udfs.FreebaseTypeNormalizerUDF();

-- WE LOAD THE PARSED AND ANNOTATED SENTENCES */
freebase_sentences_parsed_and_annotated = LOAD '$parsedSentenceFolder'
--USING parquet.pig.ParquetLoader()
 AS (
sentence:chararray,
entity1:chararray, entity2:chararray,
freebaseId1:chararray, freebaseTypes1:chararray,
freebaseId2:chararray, freebaseTypes2:chararray,
parse:chararray
);

--WE EXTRACT SUBTREE FEATURES and (for debugging) meterialize/store */
--extracted_features = FOREACH freebase_sentences_parsed_and_annotated GENERATE flatten(FeatureExtractor(sentence, parse, entity1, entity2, freebaseTypes1, freebaseTypes2, '')) AS (entity1 ,entity2, feature, signature, sentence), freebaseId1, FreebaseTypeNormalizer(freebaseTypes1) as freebaseTypes1, freebaseId2, FreebaseTypeNormalizer(freebaseTypes2) as freebaseTypes2;

extracted_features = FOREACH freebase_sentences_parsed_and_annotated GENERATE flatten(FeatureExtractor(sentence, parse, entity1, entity2, freebaseTypes1, freebaseTypes2, '')) AS (entity1 ,entity2, feature, signature, sentence), freebaseId1, freebaseTypes1, freebaseId2, freebaseTypes2;


--STORE extracted_features INTO '$testOutputFolder/extracted_features' USING parquet.pig.ParquetStorer();
STORE extracted_features INTO '$testOutputFolder/extracted_features' USING PigStorage();

-- HERE, WE CREATE A FEATURE DICTIONARY - for each feature, we generate its total count and filter our all features that
-- are not seen at least 2 times
grouped_observations = GROUP extracted_features BY (feature, signature);

feature_dict = FOREACH grouped_observations GENERATE
                   group.feature as feature_value,
                   group.signature as signature_value,
                   COUNT(extracted_features) as total_feature_count;

STORE feature_dict INTO '$testOutputFolder/feature_dict' USING PigStorage();

feature_dict_min = FILTER feature_dict BY total_feature_count > 2;

STORE feature_dict_min INTO '$testOutputFolder/feature_dict_min' USING PigStorage();

--feature_dict_sorted = ORDER feature_dict_min BY total_feature_count DESC;

--STORE feature_dict_sorted INTO '$testOutputFolder/feature_dict_sorted_min' USING PigStorage();

-- WE JOIN THE FEATURE DICTIONARY TO THE EXTRACTED FEATURES - thereby we filter out all features that are not seen
-- at least 2 times - also features are now observed with count
joined = JOIN extracted_features BY feature, feature_dict_min BY feature_value;

extracted_features_with_count = FOREACH joined GENERATE
                		entity1, entity2,
				feature,
				sentence,
				freebaseId1,
				freebaseId2,
				freebaseTypes1,
				freebaseTypes2,
				total_feature_count;

-- USING parquet.pig.ParquetStorer();
STORE extracted_features_with_count INTO '$testOutputFolder/extracted_features_with_count' USING PigStorage();

-- WE NOW GROUP THE EXTRACTION IN SUCH A FORM AS THE INDEX NEEDS

extracted_features_group = GROUP extracted_features_with_count BY (entity1, entity2, sentence, freebaseId1, freebaseId2, freebaseTypes1, freebaseTypes2);

extracted_feature_sentence_wise = FOREACH extracted_features_group GENERATE
				group.entity1,
				group.entity2,
				extracted_features_with_count.feature as features,
				group.sentence,
				group.freebaseId1,
				group.freebaseId2,
				group.freebaseTypes1,
				group.freebaseTypes2;

STORE extracted_feature_sentence_wise INTO '$testOutputFolder/extracted_features_grouped_minimum_2' USING PigStorage();
