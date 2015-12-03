package textmining.wanderlust.exploration.ui.model;

import com.google.common.collect.Lists;
import textmining.wanderlust.exploration.ui.Main;
import textmining.wanderlust.exploration.ui.pattern.ExtractorLucene;
import textmining.wanderlust.exploration.ui.pattern.PatternData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class interfaces with the previously built
 * lucene index to access the stored data.
 *
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
public class LuceneInterface {

    private static LuceneInterface instance;

    private ExtractorLucene lucene;

    private LuceneInterface() {
        lucene = new ExtractorLucene(Main.config.getProperty("lucene"));
    }

    /**
     * Returns the singleton instance that gives
     * access to the Lucene database
     * @return The singleton instance of the LuceneInterface
     */
    public static LuceneInterface getInstance() {
        if (instance == null)
            instance = new LuceneInterface();
        return instance;
    }

    //========================================
    // Relation Extractor Lucene Interface
    //========================================

    /**
     * Returns example sentences given a selected pattern, a subject and object id that was set
     * or alternatively a list of subject type restrictions or object type restrictions and a limit.
     * Recursive references to other extractors should be evaluated by now.
     *
     * @param pattern The pattern that should be included in the sentence
     * @param subject A subject id that should be present
     * @param object An object id that should be present
     * @param subjects A list of subject type restrictions
     * @param objects A list of object type restrictions
     * @param limit The limit for the example sentences
     * @return A list of example sentences including the used subject and object
     */
    public List<List<String>> getSentences(String pattern, String subject, String object, List<String> subjects, List<String> objects, int limit) {

        System.out.println("getSentences() pattern = " + pattern);
        List<PatternData> data = lucene.queryIndex(pattern.trim(), subject, object, subjects, objects, limit);
        List<List<String>> result = Lists.newArrayList();
        for (PatternData patternData : data) {
            List<String> tmpList = new ArrayList<String>();
            tmpList.add(patternData.getSentence());
            tmpList.add(patternData.getSubject());
            tmpList.add(patternData.getObject());
            result.add(tmpList);
        }
        return result;
    }

    /**
     * Executes the extractor with the given pattern subject and object id,
     * alternatively subject and object type restrictions and the given limit.
     *
     * @param pattern The pattern that should be included in the extracted relations
     * @param subject The subject id for the pattern
     * @param object The object id for the pattern
     * @param subjects A list of subject entity type restrictions
     * @param objects A list of object entity type restrictions
     * @param limit A limit for the extracted results
     * @return A list of extraction results given the subject, object and pattern restrictions
     */
    public List<PatternData> getPatterns(String pattern, String subject, String object, List<String> subjects, List<String> objects, int limit) {
        System.out.println("getPatterns() pattern = " + pattern);
        double t1 = System.currentTimeMillis();
        List<PatternData> ret = lucene.queryIndex(pattern.trim(), subject, object, subjects, objects, limit);
        System.out.println("getPatterns() took " + (System.currentTimeMillis()-t1) + "ms");
        return ret;
    }


    /**
     * Retrieves a list of extractions results given a set of
     * entity id pairs
     * @param entitySet A set of entity id pairs
     * @param limit A limit for the extracted results
     * @return A list of extraction results
     */
    public List<PatternData> queryEntityPairs(Set<String> entitySet, int limit) {

        System.out.println("Query entity set");
        double t1 = System.currentTimeMillis();
        //System.out.println("queryEntityPairs() entitySet: " + entitySet);
        List<PatternData> patternDatas = lucene.queryEntityPairs(entitySet, limit);
        System.out.println("Query entity set took " + (System.currentTimeMillis()-t1) + "ms");
        return patternDatas;
    }


    /**
     * Searches patterns given a partial of that pattern
     * @param pattern A partial to search for
     * @param limit The limit for the returned patterns
     * @return A list of patterns that match the search
     */
    public List<String> searchPatterns(String pattern, int limit) {
        return lucene.searchPatterns(pattern, limit);
    }

    /**
     * Searches subject types given a partial
     * @param subject A partial to search for
     * @param limit The limit for the returned subject types
     * @return A list of subject types that match the search
     */
    public List<String> searchSubjects(String subject, int limit) {
        return lucene.searchSubjects(subject, limit);
    }

    /**
     * Searches object types given a partial
     * @param object A partial to search for
     * @param limit The limit for the returned object types
     * @return A list of object types that match the search
     */
    public List<String> searchObjects(String object, int limit) {
        return lucene.searchObjects(object, limit);
    }

    /**
     * Returns a list of all types available in the lucene database
     * @return A list of all types available in the lucene database
     */
    public List<ExtractorLucene.ExtractedType> getAllTypes() {
        return lucene.getAllTypes();
    }

    //========================================
    // Entity Extractor Lucene Interface
    //========================================

    /**
     * Returns a list of all entity types available in the lucene database
     * @return A list of all entity types available in the lucene database
     */
//    public List<EntityLucene.ExtractedType> getAllEntityTypes() { return entityLucene.getAllTypes(); }

    /**
     * Returns example sentences given a selected pattern, and entity id that was set
     * or alternatively a list of entity type restrictions and a limit.
     * Recursive references to other extractors should be evaluated by now.
     * @param pattern The pattern that should be included in the sentence
     * @param entity The entity id that should be present in the sentence
     * @param entities Entity types that should be present in the sentence
     * @param limit The limit for the extracted sentences
     * @return A list of example sentences matching the restrictions
     */
 /*   public List<List<String>> getEntitySentences(String pattern, String entity, List<String> entities, int limit) {
        System.out.println("getEntitySentences() pattern = " + pattern);
        List<EntityData> data = entityLucene.queryIndex(pattern.trim(), entity, entities, limit);
        List<List<String>> result = Lists.newArrayList();
        for (EntityData entityData : data) {
            List<String> tmpList = new ArrayList<>();
            tmpList.add(entityData.getSentence());
            tmpList.add(entityData.getEntity());
//            tmpList.add(entityData.getFreebaseId());
            result.add(tmpList);
        }
        return result;
    }*/

    /**
     * Executes the extractor with the given pattern and entity id,
     * alternatively entity type restrictions and the given limit.
     *
     * @param pattern The pattern that should be included
     * @param entity The entity id that should be included
     * @param entities A list of entity type restrictions that should be matched
     * @param limit The limit for the extractions
     * @return A list of extracted results that match the restrictions
     */
  /*  public List<EntityData> getEntityPatterns(String pattern, String entity, List<String> entities, int limit) {
        System.out.println("getEntityPatterns pattern = " + pattern);
        double t1 = System.currentTimeMillis();
        List<EntityData> resultList = entityLucene.queryIndex(pattern, entity, entities, limit);
        System.out.println("getEntityPatterns took " + (System.currentTimeMillis()-t1) + "ms");
        return resultList;
    }*/

    /**
     * Extracts all entity extractions that contain the given entities.
     *
     * @param entities A set of entity type restrictions
     * @param limit A limit for the extraction
     * @return A list of entity extractions results matching the restriction
     */
 /*   public List<EntityData> queryEntity(Set<String> entities, int limit) {
        System.out.println("Query entity set");
        double t1 = System.currentTimeMillis();
        List<EntityData> entityData = entityLucene.queryEntities(entities, limit);
        System.out.println("Query entities took " + (System.currentTimeMillis()-t1) + "ms");
        return entityData;
    }*/

    /**
     * Returns a list of entity types that match the search query
     *
     * @param type A partial to search for
     * @param limit A limit for the returned results
     * @return A list of entity types that match the partial
     */
  /*  public List<String> searchEntityTypes(String type, int limit) {
        return entityLucene.searchTypes(type, limit);
    }*/

    /**
     * Returns a list of entity patterns that match the search query
     *
     * @param pattern A partial to search for
     * @param limit A limit for the returned results
     * @return A list of entity patterns that match the partial
     */
/*    public List<String> searchEntityPatterns(String pattern, int limit) {
        return entityLucene.searchPatterns(pattern, limit);
    }*/

  /*  public static void main(String[] args) {
        System.out.println(LuceneInterface.getInstance().searchEntityPatterns("fruit", 100));
        String fruitPattern = "[X] be.v fruit.n";
        List<String> patterns = new ArrayList<>();
        patterns.add(fruitPattern);
        List<List<String>> blah = EntityExtractor.getInstance().patternSuggestion(patterns, "", new ArrayList<String>(), false);
        System.out.println("Similar Patterns:");
        for (String pattern : blah.get(0)) {
            System.out.println("  " + pattern);
        }
        System.out.println("Possible Types:");
        for (String type : blah.get(1)) {
            System.out.println("  " + type);
        }
        System.out.println("Output entity data:");
        for (String jsonoutput : blah.get(2)) {
            System.out.println("  " + jsonoutput);
        }
    }*/


}
