package textmining.wanderlust.exploration.ui.model;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import textmining.wanderlust.exploration.ui.helper.ItemCounter;
import textmining.wanderlust.exploration.ui.helper.MapSorter;
import textmining.wanderlust.exploration.ui.pattern.PatternData;

import java.util.*;

/**
 * This class takes care of extracting the patterns from
 * a sentence given the subject and the object of it.
 * This class is a singleton and thus can only be
 * instantiated once.
 *
 * @author Alan Akbik (alan.akbik@tu-berlin.de)
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
public class RelationExtractor {

    private static RelationExtractor instance;

    protected RelationExtractor(boolean instanciate) {

    }

    /**
     * Returns the singleton instance of the RelationExtractor
     *
     * @return The singleton instance of the RelationExtractor
     */
    public static synchronized RelationExtractor getInstance() {
        if (instance == null)
            instance = new RelationExtractor(true);
        return instance;
    }


    /**
     * This method returns a list of patterns given a sentence and
     * the object and subject of the sentence.
     * This method returns null if the extraction of the patterns failed.
     *
     * @param sentence The sentence to extract the pattern from
     * @param subject  The subject of the sentence
     * @param object   The object of the sentence
     * @return A list of extracted patterns
     */
 /*   public List<String> getPatterns(String sentence, String subject, String object) {
        DependencyParse dependencyParse = parser.parse(sentence);

        EntityPair entityPair = EntityFinderUtility.findEntityPairFrom(dependencyParse, subject, object);
        FeatureExtractorEntityPair berserk = new FeatureExtractorEntityPair("NONE", EntityType.ALL, "false", 4);

        try {
            List<PatternTuple> extract = berserk.extract(new LabeledSentence(dependencyParse.getSentence(), dependencyParse));
            List<String> foundPattern = Lists.newArrayList();

            for (PatternTuple patternTuple : extract) {
                foundPattern.add(patternTuple.get(1).replaceAll(" \\[\\[.*", ""));
            }

            return foundPattern;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }*/

    /**
     * This method executes the pattern suggestion given a list of patterns,
     * a subject and object id or alternatively a list of subject and object entity types.
     *
     * @param patterns The pattern restrictions for the extractor
     * @param subject  The subject ids that should be required to match
     * @param object   The object ids that should be required to match
     * @param subjects A list of subject entity type restrictions
     * @param objects  A list of object entity type restrictions
     * @return A list of pattern, subject and object suggestions and an extractor output based on the restrictions
     */
    public List<List<String>> patternSuggestion(List<String> patterns, String subject, String object, List<String> subjects, List<String> objects) {
        return patternSuggestion(patterns, subject, object, subjects, objects, false);
    }

    /**
     * This method executes the extractor with the given selected patterns, a set subject or object id or -
     * alternatively - a list of subject and object entity type restrictions.
     *
     * @param selectedPatterns A list of pattern restrictions for the extractor
     * @param subject          The subject id of the extractor
     * @param object           The object id of the extractor
     * @param subjects         A list of subject entity type restrictions
     * @param objects          A list of object entity type restrictions
     * @return A list of extracted relation results
     */
    public List<String> executeExtractor(List<String> selectedPatterns, String subject, String object, List<String> subjects, List<String> objects) {

        //get all patterns and entity pairs found with current selection
        List<PatternData> allExtractions = Lists.newArrayList();
        if (selectedPatterns.size() == 0)
            selectedPatterns.add("");
        for (String pat : selectedPatterns) {
            List<PatternData> patternDatas = LuceneInterface.getInstance().getPatterns(pat, subject, object, subjects, objects, 50000);
            allExtractions.addAll(patternDatas);
        }

        List<String> output = new ArrayList<String>();
        for (PatternData patternData : allExtractions) {
            List<String> values = new ArrayList<String>();
            values.add("\"" + patternData.getSubject() + "\"");
            values.add("\"" + patternData.getObject()+ "\"");
            values.add("\"" + patternData.getIdPair()+ "\"");
            values.add("\"" + patternData.getSentence()+ "\"");
            String patternDataString = Joiner.on(",").join(values);
           // String patternDataString = new Gson().toJson(values);
            output.add(patternDataString.toString());
        }

        return output;
    }

    /**
     * This method executes the pattern suggestion given a list of patterns,
     * a subject and object id or alternatively a list of subject and object entity types.
     *
     * @param selectedPatterns The pattern restrictions for the extractor
     * @param subject          The subject ids that should be required to match
     * @param object           The object ids that should be required to match
     * @param subjects         A list of subject entity type restrictions
     * @param objects          A list of object entity type restrictions
     * @return A list of pattern, subject and object suggestions and an extractor output based on the restrictions
     */
    public List<List<String>> patternSuggestion(List<String> selectedPatterns, String subject, String object, List<String> subjects, List<String> objects, List<String> patternsMarkedAsGood, boolean favorVerbNouns) {
        return patternSuggestion(selectedPatterns, subject, object, subjects, objects, favorVerbNouns);
    }

    /**
     * This method executes the pattern suggestion given a list of patterns,
     * a subject and object id or alternatively a list of subject and object entity types.
     *
     * @param selectedPatterns The pattern restrictions for the extractor
     * @param subject          The subject ids that should be required to match
     * @param object           The object ids that should be required to match
     * @param subjects         A list of subject entity type restrictions
     * @param objects          A list of object entity type restrictions
     * @return A list of pattern, subject and object suggestions and an extractor output based on the restrictions
     */
    public List<List<String>> patternSuggestion(List<String> selectedPatterns, String subject, String object, List<String> subjects, List<String> objects, boolean favorVerbNouns) {

        ItemCounter patternCounter = new ItemCounter();

        int limit = 2000;

        if (selectedPatterns.size() > 0) {
            Long patLim = Math.round(1000. / selectedPatterns.size());
            limit = patLim.intValue();
        }

        /*
        get all patterns and entity pairs found with current selection
         */
        Set<String> entitySet = Sets.newHashSet();

        Set<String> mainWords = Sets.newHashSet();

        if (selectedPatterns.size() == 0) {
            selectedPatterns.add("");
        }

        Set<PatternData> allExtractions = Sets.newHashSet();
        for (String pat : selectedPatterns) {

            System.out.println("limit = " + limit);

            List<PatternData> patternDatas = LuceneInterface.getInstance().getPatterns(pat, subject, object, subjects, objects, limit);
            allExtractions.addAll(patternDatas);

            for (PatternData patternData : patternDatas) {
                entitySet.add(patternData.getIdPair());
            }

            for (String token : pat.split(" ")) {
                if (token.contains(".")) mainWords.add(token);
            }
        }

        /*
         get other patterns that hold for found entity pairs (these patterns may be suggestions)
         */
        Set<String> epPatternMemory = Sets.newHashSet();

        if (entitySet.size() > 0) {

            List<PatternData> patternsForEntities = Lists.newArrayList();

            List<String> entityList = Lists.newArrayList(entitySet);

            List<List<String>> partition = Lists.partition(entityList, 1000);
            for (List<String> part : partition) {
                patternsForEntities.addAll(LuceneInterface.getInstance().queryEntityPairs(Sets.newHashSet(part), 2000));

            }


            System.out.println("retrieved data for suggestions: " + patternsForEntities.size());

            for (PatternData patternForEntity : patternsForEntities) {
                Set<String> patternSet = Sets.newHashSet();
                for (String pat : patternForEntity.getPattern().split(" ")) {
                    patternSet.add(pat.replaceAll("_", " "));
                }
                for (String pat : patternSet) {
                    String sig = pat + "-" + patternForEntity.getPair();
                    if (epPatternMemory.contains(sig)) continue;
                    epPatternMemory.add(sig);
                    patternCounter.put(pat, 1f);
                }

            }

        }


        /*
        filter subsumed and already selected patterns from suggestions
         */
        Map<String, Float> patternsWeighted = patternCounter.getAll();

        Map<String, Float> patternsReWeighted = Maps.newHashMap();

        System.out.println("reweighPattern");
        double t1 = System.currentTimeMillis();
        for (String pattern : patternsWeighted.keySet()) {

            try {
                float weight = reweighPattern(patternsWeighted, pattern, favorVerbNouns, mainWords);

                patternsReWeighted.put(pattern, weight);

            } catch (Exception e) {
                System.err.println("pattern problemas = " + pattern);
            }
        }

        patternsWeighted = patternsReWeighted;

        System.out.println("reweighPatterns took " + (System.currentTimeMillis() - t1) + "ms");


        List<String> similarPatterns = Lists.newArrayList();
        SortedSet<Map.Entry<String, Float>> entries = MapSorter.entriesSortedByValues(patternsWeighted);


        // check if pattern subsumed
        for (Map.Entry<String, Float> entry : entries) {

            if (selectedPatterns.contains(entry.getKey())) continue;

            // TODO: we have no bad patterns yet
            // if (patternsMarkedAsBad.contains(entry.getKey())) continue;

            boolean subsumed = true;
            List<String> tokens = Lists.newArrayList(entry.getKey().split(" "));
            for (String goodPattern : selectedPatterns) {

                subsumed = true;
                List<String> goodTokens = Lists.newArrayList(goodPattern.split(" "));
                for (String token : goodTokens) {
                    if (!tokens.contains(token)) subsumed = false;
                }
                if (subsumed) {
                    break;
                }

            }

            if (subsumed) continue;
            similarPatterns.add(entry.getKey());

        }

        //
        // ---- FINISHED similarPatterns -----
        //

        ItemCounter subjectCounter = new ItemCounter();
        ItemCounter objectCounter = new ItemCounter();

        for (PatternData extraction : allExtractions) {
            for (String type : extraction.getSubjectTypes()) {
                if (isGood(type))
                    subjectCounter.put(type, 1f);
            }

            for (String type : extraction.getObjectTypes()) {
                if (isGood(type))
                    objectCounter.put(type, 1f);
            }
        }

        List<String> possibleX = getTypeSuggestions(subjectCounter, subjects);
        List<String> possibleY = getTypeSuggestions(objectCounter, objects);

        //
        // ------ Finished subjects and objects
        //

        List<String> output = Lists.newArrayList();
        int i = 0;
        for (PatternData patternData : allExtractions) {
            i++;
            if (i > 200) break;
            String paternDataString = new Gson().toJson(patternData);
            output.add(paternDataString);
        }

        List<List<String>> result = Lists.newArrayList();
        result.add(similarPatterns);
        result.add(possibleX);
        result.add(possibleY);
        result.add(output);

        return result;

    }

    /**
     * This method executes the extractor for each pattern that is given,
     * collects the output from each extraction and returns a list of
     * extraction results.
     *
     * @param patterns The patterns to restrict the extractor to
     * @param subject  A subject id for the extractor
     * @param object   An object id for the extractor
     * @param subjects A list of subject entity type restrictions
     * @param objects  A list of object entity type restrictions
     * @return A list of PatternData that match the extractor
     */
    private List<PatternData> patternOutput(List<String> patterns, String subject, String object, List<String> subjects, List<String> objects) {

        List<PatternData> allExtractions = Lists.newArrayList();
        for (String pat : patterns) {

            List<PatternData> patternDatas = LuceneInterface.getInstance().getPatterns(pat, subject, object, subjects, objects, 10000);
            allExtractions.addAll(patternDatas);

        }

        return allExtractions;

    }

    /**
     * Returns if an entity type is good or not.
     * "Bad" types are defined by base. user. or topic
     * included into them.
     * <p/>
     * This method is specifically designed for freebase-based
     * data collection
     *
     * @param type The type to test
     * @return Whether the entity type is "good"
     */
    public static boolean isGood(String type) {
        if (type.contains("topic")) return false;
        if (type.contains("base.")) return false;
        if (type.contains("user.")) return false;
        return true;
    }

    /**
     * Reweights a given pattern depending on how similiar it is
     * to the current extractor state.
     *
     * @param patternsWeighted A map of pattern names to their weights
     * @param pattern          The current pattern
     * @param favorVerbNouns   Whether or not patterns with verbs and nouns should be favored
     * @param mainTokens       The main tokens
     * @return A new weight for the given pattern
     */
    public static float reweighPattern(Map<String, Float> patternsWeighted, String pattern, boolean favorVerbNouns, Set<String> mainTokens) {

        if (patternsWeighted.get(pattern.trim()) == null) return 0;
        if (patternsWeighted.get(pattern.trim()) < 4.) return patternsWeighted.get(pattern.trim());

        float mod = (50f - pattern.length()) / 30f;

        float weight = patternsWeighted.get(pattern.trim()) + mod;

        if (!pattern.contains(".v") && !pattern.contains(".n")) {
            weight = weight / 10;
        }

        if (pattern.contains(".v")) {
            weight = weight + 5;
        }
        if (pattern.contains("be.v") && !(pattern.contains(".j") || pattern.contains(".n"))) weight = weight - 40;

        if (pattern.contains(".n")) {
            weight = weight + 2;
        }
        if (favorVerbNouns && pattern.contains(".v") && pattern.contains(".n")) weight = weight + 5;

        if (pattern.contains(".j")) {
            weight = weight + 1;
        }

        if (pattern.contains("(")) {
            weight = weight - 50;
        }
        if (pattern.contains(")")) {
            weight = weight - 50;
        }

        if (pattern.contains(":")) {
            weight = weight - 50;
        }

        if (pattern.contains(" .") || pattern.contains("..")) {
            weight = weight - 50;
        }

        if (pattern.contains(",")) {
            weight = weight - 50;
        }

        if (pattern.contains("0"))
            weight = weight - 50;

        for (String token : pattern.split(" ")) {
            if (mainTokens.contains(token)) weight = weight + 2;
        }

        return weight;
    }

    public static List<String> getTypeSuggestions(ItemCounter patternCounter, List<String> good) {
        List<String> similarPatterns = Lists.newArrayList();
        SortedSet<Map.Entry<String, Float>> sortedSet = MapSorter.entriesSortedByValues(patternCounter.getAll());
        int c = 0;
        for (Map.Entry<String, Float> entry : sortedSet) {

            if (good.contains(entry.getKey())) continue;

            similarPatterns.add(entry.getKey());
            c++;
            if (c == 50) break;
        }
        return similarPatterns;
    }

}
