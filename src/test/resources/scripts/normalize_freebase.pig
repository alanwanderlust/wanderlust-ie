-- Include JAR
-- REGISTER 'textmining-feature-extraction-1.0-SNAPSHOT-job.jar'

-- Enable compression for intermediate results
SET mapred.child.java.opts '-Xmx4G';
SET pig.tmpfilecompression 'true';
SET pig.tmpfilecompression.codec 'gz';
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

freebase_deduplicated = DISTINCT freebase_sentences_parsed_and_annotated;
STORE freebase_deduplicated INTO '$testOutputFolder/freebase_deduplicated' USING PigStorage();
