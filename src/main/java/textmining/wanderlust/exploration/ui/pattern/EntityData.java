package textmining.wanderlust.exploration.ui.pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents one entry in the lucene index.
 * It contains a sentence with an annotated entity (including
 * freebase id and freebase types) and
 * all patterns of the entity that were extracted from
 * the sentence.
 *
 * Created by thilo on 25/06/14.
 */
public class EntityData {

    private final String entity;
    private final String id;
    private final String sentence;
    private final String patternsString;
    private final String typesString;

    private final List<String> patterns;
    private final List<String> readablePatterns;

    private final Set<String> types;

    public EntityData(String entity, String id, String sentence, String patternsString, String typesString) {
        this.entity         = entity;
        this.id             = id;
        this.sentence       = sentence;
        this.patternsString = patternsString;
        this.typesString    = typesString;

        patterns = Lists.newArrayList(patternsString.split(" "));

        readablePatterns = Lists.newArrayList();
        for (String pattern : patterns) {
            String singlePattern = pattern.replaceAll("_", " ")
                                          .replaceAll("\\.n", "")
                                          .replaceAll("\\.v", "")
                                          .replaceAll("\\[", "")
                                          .replaceAll("\\]", "")
                                          .replaceAll("#", "");
            readablePatterns.add(singlePattern);
        }

        types = Sets.newHashSet(typesString.split(" "));
    }

    /**
     * Returns the entity of this EntityData
     * @return The entity
     */
    public String getEntity() {
        return entity;
    }

    /**
     * Returns the freebase ID
     * of the sentence without  the '/m/'
     * prefix.
     * @return The freebase ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the sentence of the Entity Data
     * @return The sentence of the Entity Data
     */
    public String getSentence() {
        return sentence;
    }

    /**
     * Returns a string with all patterns of
     * this EntityData.
     * @return A string with all pattterns
     */
    public String getPatternsString() {
        return patternsString;
    }

    /**
     * The string of all freebase types of
     * the entity. Types are separated with
     * a space.
     * @return String of al freebase types
     */
    public String getTypesString() {
        return typesString;
    }

    /**
     * Returns a list of patterns with
     * annotations and underscores instead
     * of spaces
     * @return a list of patterns
     */
    public List<String> getPatterns() {
        return patterns;
    }

    /**
     * Returns patterns in a readable format.
     * The words are space separated and not
     * annotated.
     * @return Patterns in a readable format.
     */
    public List<String> getReadablePatterns() {
        return readablePatterns;
    }

    /**
     * Returns the freebase id of the sentence d
     * including the '/m/' prefix
     * @return The freebase id
     */
    public String getFreebaseId() {
        return "/m/"+getId();
    }

    /**
     * Returns a HashMap that maps from a patterns string
     * containing underscores and annotations to beautified
     * patterns.
     * @return a Map from Patterns to ReadablePatterns
     */
    public Map<String,String> getPatternMap() {

        // Sanity check
        if (patterns.size() != readablePatterns.size())
            return null;

        Map<String,String> map = Maps.newHashMap();

        for (int i = 0; i < patterns.size(); i++) {
            map.put(patterns.get(i), readablePatterns.get(i));
        }

        return map;
    }

    /**
     * Returns a list of all freebase types
     * of the entity
     * @return A list of all freebase types
     */
    public Set<String> getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return "EntityData{" +
                "pattern='" + patternsString + '\'' +
                ", entity='" + entity + '\'' +
                ", sentence='" + sentence + '\'' +
                ", types='" + typesString + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
