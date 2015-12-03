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

extracted_features = FOREACH freebase_sentences_parsed_and_annotated GENERATE
flatten(
FeatureExtractor(sentence, parse, entity1, entity2, freebaseId1, freebaseId2, freebaseTypes1, freebaseTypes2))
AS (entity1, entity2, feature, freebaseId1, freebaseId2, freebaseTypes1, freebaseTypes2, sentence);


--STORE extracted_features INTO '$testOutputFolder/extracted_features' USING parquet.pig.ParquetStorer();
STORE extracted_features INTO '$testOutputFolder/extracted_features' USING PigStorage();

