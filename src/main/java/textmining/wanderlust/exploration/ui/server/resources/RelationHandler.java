package textmining.wanderlust.exploration.ui.server.resources;


import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import textmining.wanderlust.exploration.ui.model.LuceneInterface;
import textmining.wanderlust.exploration.ui.model.RelationExtractor;
import textmining.wanderlust.exploration.ui.pattern.ExtractorLucene;
import textmining.wanderlust.exploration.ui.pattern.PatternData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles all request to the relation
 * between the entities, namely the patterns.
 * <p/>
 * Note that the subjectList and objectList may encode
 * different information and different actions are performed
 * depending on type of information provided.
 * <p/>
 * The argument may encode a list of entity types
 * like ["person.person", "person.celebrity"]. The methods
 * will then extract patterns/make suggestions that fit those
 * type restrictions.
 * <p/>
 * When the argument contains a list with only one element that
 * starts and ends with "__" an ID of a single entity is encoded.
 * For example ["__02mjmr__"] encodes the entity Barack Obama.
 * The methods will then extract patterns/make suggestions that include
 * that specifig entity.
 * <p/>
 * When the argument contains a list with only one element that
 * starts and ends with "@@" a result of an extractor is encoded.
 * Then, the extractor mention is evaluated and the resulting entities
 * are used as restriction for either the subject entities or the object entities.
 * For example ["__Software supports File Format.subjects"] encodes
 * the extractor called "Software supports File Format" and uses the
 * subjects of the extractor result as the entity restriction.
 * <p/>
 * Searches are handled by the SearchHanlder class
 *
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
@Path("/pattern/")
public class RelationHandler {

    /**
     * The maximum recursion depth used for evaluating the extractors
     */
    public static final int MAX_RECURSION_DEPTH = 5;

    private enum ExtractorType {ET_RELATION, ET_ENTITY}

    ;

    private enum ExtractorEntity {EE_ENTITY, EE_SUBJECTS, EE_OBJECTS}

    ;

    public static final Logger LOG = Logger.getLogger(RelationHandler.class);

    /**
     * Returns a list of all entity types in JSON format
     *
     * @return a list of all entity types
     */
    @GET
    @Path("/alltypes")
    @Produces(MediaType.APPLICATION_JSON)
    public String allTypes() {
        //Object types and subject types are the same.
        List<ExtractorLucene.ExtractedType> allTypes = LuceneInterface.getInstance().getAllTypes();
        return new Gson().toJson(allTypes);
    }

    /**
     * Returns a list of patterns in JSON format given a sentence,
     * a subject in that sentence and an object in that sentence
     * @param sentence The sentence from which patterns should be extracted
     * @param subject The subject entity of the sentence
     * @param object The object entity of the sentence
     * @return A list of patterns in JSON format
     */
 /*   @GET
    @Path("/{sentence}/{subject}/{object}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPatterns(@PathParam("sentence") String sentence, @PathParam("subject") String subject, @PathParam("object") String object) {

        List<String> patterns = RelationExtractor.getInstance().getPatterns(sentence, subject, object);
        System.out.println("patterns: " + patterns);
        return new Gson().toJson(patterns);
    }*/

    /**
     * Returns a list of sentences in JSON format that include
     * the pattern, one of the subjects and one of the objects given
     *
     * @param pattern     A pattern that should be included in the sentence
     * @param subjectList A list of entity types restrictions
     * @param objectList  A list of entity types restrictions
     * @param limit       A limit for the number of sentences returned
     * @return A list of sentences in JSON format
     */
    @GET
    @Path("/sentence/{pattern}/{subjectList}/{objectList}/limit/{limit}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSentences(@PathParam("pattern") String pattern, @PathParam("subjectList") String subjectList, @PathParam("objectList") String objectList, @PathParam("limit") int limit) {
        List<String> subjects = new Gson().fromJson(subjectList, new TypeToken<List<String>>() {
        }.getType());
        List<String> objects = new Gson().fromJson(objectList, new TypeToken<List<String>>() {
        }.getType());

        pattern = decodePattern(pattern);

        List<List<String>> sentences = recursiveSentence(pattern, subjects, objects, limit, 0);
        return new Gson().toJson(sentences);
    }

    /**
     * Returns suggestions for patterns, subject entity types and
     * object entity types given already selected patterns, subject types
     * and object types.
     * The suggestions are returns as a JSON list with the first element
     * being a JSON list of pattern suggestions, the second element being
     * a list of suggested subject entity types and the third element being
     * a list of object entity types. The fourth element of the list is a set
     * of relations that match the current restrictions.
     * <p/>
     * This method can be called to fully reevaluate the state of the extractor.
     * <p/>
     * Note that the subject and object list may encode types, single entities or
     * relation/entity extraction results.
     *
     * @param patternList A list of patterns encoded as json
     * @param subjectList A list of subject entity types, a single entity or an extractor result
     * @param objectList  A list of object entity types, a single entity or an extractor result
     * @return A JSON formatted array with suggested patterns, subjects, objects and the resulting extracted entities
     */
    @GET
    @Path("/suggestion/{patternList}/{subjectList}/{objectList}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSuggestion(@PathParam("patternList") String patternList, @PathParam("subjectList") String subjectList, @PathParam("objectList") String objectList) {

        System.out.println("Pattern suggestion");
        double t1 = System.currentTimeMillis();

        //Convert inputs
        List<String> patterns = new Gson().fromJson(patternList, new TypeToken<List<String>>() {
        }.getType());
        List<String> subjects = new Gson().fromJson(subjectList, new TypeToken<List<String>>() {
        }.getType());
        List<String> objects = new Gson().fromJson(objectList, new TypeToken<List<String>>() {
        }.getType());

        for (int i = 0; i < patterns.size(); i++) {
            patterns.set(i, decodePattern(patterns.get(i)));
        }

        List<List<String>> result = recursiveSuggestion(patterns, subjects, objects, 0);

        System.out.println("Finished: " + ((System.currentTimeMillis() - t1) / 1000) + " seconds");

        return new Gson().toJson(result);
    }

    /**
     * Extracts all matching relations given a list of patterns,
     * subject entity types and object entity types.
     * <p/>
     * The result is returned as a downloadable txt file.
     *
     * @param patternList A list of patterns
     * @param subjectList A list of subject entity types
     * @param objectList  A list of object entity types
     * @return A JSON list of extracted results
     */
    @GET
    @Path("/output/{patternList}/{subjectList}/{objectList}/results.txt")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String getOutput(@PathParam("patternList") String patternList, @PathParam("subjectList") String subjectList, @PathParam("objectList") String objectList) {

        LOG.info("Generating output!");

        //Convert inputs
        List<String> patterns = new Gson().fromJson(patternList, new TypeToken<List<String>>() {
        }.getType());
        List<String> subjects = new Gson().fromJson(subjectList, new TypeToken<List<String>>() {
        }.getType());
        List<String> objects = new Gson().fromJson(objectList, new TypeToken<List<String>>() {
        }.getType());

        for (int i = 0; i < patterns.size(); i++) {
            patterns.set(i, decodePattern(patterns.get(i)));
        }

        List<String> list = recursiveOutput(patterns, subjects, objects, 0);
        System.out.println("Raw output size: " + list.size());

        StringBuilder sb = new StringBuilder();
        sb.append("# Patterns: ");
        sb.append(new Gson().toJson(patterns)).append("\n");
        sb.append("# Subject restrictions: ");
        sb.append(new Gson().toJson(subjects)).append("\n");
        sb.append("# Object restrictions: ");
        sb.append(new Gson().toJson(objects)).append("\n");
        sb.append("# Number of instances: ");
        sb.append(list.size()).append("\n");
        for (String idp : list) {
            sb.append(idp).append("\n");
        }

        return sb.toString();
    }

    /**
     * This helper method decodes a pattern, i.e.
     * replaces the :octothorp: and :qmark: with their
     * respective symbols.
     *
     * @param pattern A pattern received from the javascript frontent
     * @return The decoded pattern
     */
    public static String decodePattern(String pattern) {
        return pattern
                .replaceAll(":octothorp:", "#")
                .replaceAll(":qmark:", "?");
    }

    /**
     * Executes a pattern and entity suggestion.
     * If a recursion is set, the specified extractor
     * is loaded from the database and executed beforehand.
     *
     * @param patterns The pattern restriction of the extractor
     * @param subjects The subject entity restrictions of the extractor
     * @param objects  The object entity restrictions of the extractor
     * @param depth    The current recursion depth (for loop detection purposes)
     * @return A list of pattern, subject and object suggestions and the output of the extractor
     */
    public static List<List<String>> recursiveSuggestion(List<String> patterns, List<String> subjects, List<String> objects, int depth) {

        String subject = "";
        String object = "";
        String subjectExtractor = "";
        String objectExtractor = "";

        //Check for explicit subject/object
        if (subjects.size() == 1 && subjects.get(0).startsWith("__") && subjects.get(0).endsWith("__")) {
            System.out.println("subject was set!");
            subject = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            System.out.println(subject);
            subjects = Lists.newArrayList();
        } else if (subjects.size() == 1 && subjects.get(0).startsWith("@@") && subjects.get(0).endsWith("@@")) {
            System.out.println("subject extractor was set!");
            subjectExtractor = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            subject = "__extractor__";
            System.out.println(subjectExtractor);
            subjects = evaluate(subjectExtractor, depth);
        }

        if (objects.size() == 1 && objects.get(0).startsWith("__") && objects.get(0).endsWith("__")) {
            System.out.println("object was set!");
            object = objects.get(0).substring(2, objects.get(0).length() - 2);
            System.out.println(object);
            objects =  Lists.newArrayList();
        } else if (objects.size() == 1 && objects.get(0).startsWith("@@") && objects.get(0).endsWith("@@")) {
            System.out.println("object extractor was set!");
            objectExtractor = objects.get(0).substring(2, objects.get(0).length() - 2);
            object = "__extractor__";
            System.out.println(objectExtractor);
            objects = evaluate(objectExtractor, depth);
        }

        List<String> patternsMarkedAsGood = Lists.newArrayList();

        return RelationExtractor.getInstance().patternSuggestion(patterns, subject, object, subjects, objects);
    }

    /**
     * Recursively extracts all the matching relations given
     * the pattern, subject and object restrictions.
     * <p/>
     * If an extractor is set for the subject or object entity
     * restriction, the referenced extractor is loaded from the
     * database and is evaluated recursively.
     *
     * @param patterns A list of pattern restrictions
     * @param subjects The restrictions for the subject entities
     * @param objects  The restrictions for the object entities
     * @param depth    The current recursion depth (for loop detection purposes)
     * @return Returns a list of extracted relations in JSON format
     */
    public static List<String> recursiveOutput(List<String> patterns, List<String> subjects, List<String> objects, int depth) {

        String subject = "";
        String object = "";
        String subjectExtractor = "";
        String objectExtractor = "";

        //Check for explicit subject/object
        if (subjects.size() == 1 && subjects.get(0).startsWith("__") && subjects.get(0).endsWith("__")) {
            System.out.println("subject was set!");
            subject = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            System.out.println(subject);
            subjects =  Lists.newArrayList();
        } else if (subjects.size() == 1 && subjects.get(0).startsWith("@@") && subjects.get(0).endsWith("@@")) {
            System.out.println("subject extractor was set!");
            subjectExtractor = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            subject = "__extractor__";
            System.out.println(subjectExtractor);
            subjects = evaluate(subjectExtractor, depth);
        }

        if (objects.size() == 1 && objects.get(0).startsWith("__") && objects.get(0).endsWith("__")) {
            System.out.println("object was set!");
            object = objects.get(0).substring(2, objects.get(0).length() - 2);
            System.out.println(object);
            objects =  Lists.newArrayList();
        } else if (objects.size() == 1 && objects.get(0).startsWith("@@") && objects.get(0).endsWith("@@")) {
            System.out.println("object extractor was set!");
            objectExtractor = objects.get(0).substring(2, objects.get(0).length() - 2);
            object = "__extractor__";
            System.out.println(objectExtractor);
            objects = evaluate(objectExtractor, depth);
        }

        List<String> patternsMarkedAsGood = Lists.newArrayList();

        return RelationExtractor.getInstance().executeExtractor(patterns, subject, object, subjects, objects);
    }

    /**
     * Recursively evaluates the extractor to return sentences that match
     * the given restrictions.
     * <p/>
     * Returns a JSON list containing the the subject, object and sentence for
     * each extracted result found.
     *
     * @param pattern  The pattern that should be matched in a sentence
     * @param subjects A list of subject entity restrictions
     * @param objects  A list of object entity restriction
     * @param limit    The maximum number of sentences that should be extracted
     * @param depth    The current recursion depth (for loop prevention purposes)
     * @return A JSON list of sentences that match the restrictions
     */
    public static List<List<String>> recursiveSentence(String pattern, List<String> subjects, List<String> objects, int limit, int depth) {

        String subject = "";
        String object = "";
        String subjectExtractor = "";
        String objectExtractor = "";

        //Check for explicit subject/object
        if (subjects.size() == 1 && subjects.get(0).startsWith("__") && subjects.get(0).endsWith("__")) {
            System.out.println("subject was set!");
            subject = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            System.out.println(subject);
            subjects =  Lists.newArrayList();
        } else if (subjects.size() == 1 && subjects.get(0).startsWith("@@") && subjects.get(0).endsWith("@@")) {
            System.out.println("subject extractor was set!");
            subjectExtractor = subjects.get(0).substring(2, subjects.get(0).length() - 2);
            subject = "__extractor__";
            System.out.println(subjectExtractor);
            subjects = evaluate(subjectExtractor, depth);
        }

        if (objects.size() == 1 && objects.get(0).startsWith("__") && objects.get(0).endsWith("__")) {
            System.out.println("object was set!");
            object = objects.get(0).substring(2, objects.get(0).length() - 2);
            System.out.println(object);
            objects =  Lists.newArrayList();
        } else if (objects.size() == 1 && objects.get(0).startsWith("@@") && objects.get(0).endsWith("@@")) {
            System.out.println("object extractor was set!");
            objectExtractor = objects.get(0).substring(2, objects.get(0).length() - 2);
            object = "__extractor__";
            System.out.println(objectExtractor);
            objects = evaluate(objectExtractor, depth);
        }

        List<String> patternsMarkedAsGood = Lists.newArrayList();

        return LuceneInterface.getInstance().getSentences(pattern, subject, object, subjects, objects, limit);
    }

    /**
     * Evaluates an extractor string with the format
     * "[Exatractor name]:[subjects/objects/entities]".
     * and returns a list of [subejct/object/entity] IDs that resulted
     * from the extractor. The extractor to be evaluated has to be available
     * in the database.
     * If the recursion depth limit (MAX_RECURSION_DEPTH) is reached, an empty
     * list is returned.
     * <p/>
     * The extractor given may either be a relation or an entity extractor.
     *
     * @param extractorString The string representation of a stored extractor.
     * @param depth           The current recursion depth (for loop prevention purposes)
     * @return A list of extracted entity IDs
     */
    public static List<String> evaluate(String extractorString, int depth) {
        List<String> ids =  Lists.newArrayList();

        if (depth == MAX_RECURSION_DEPTH)
            return ids;

        System.out.println("Evaluate extractor: " + extractorString + " - with recursion depth: " + depth);

        String extractorName;
        ExtractorType extractorType;
        ExtractorEntity extractorEntity;

        if (extractorString.substring(extractorString.length() - 9, extractorString.length()).equalsIgnoreCase(".subjects")) {
            extractorName = extractorString.substring(0, extractorString.length() - 9);
            extractorType = ExtractorType.ET_RELATION;
            extractorEntity = ExtractorEntity.EE_SUBJECTS;
        } else if (extractorString.substring(extractorString.length() - 8, extractorString.length()).equalsIgnoreCase(".objects")) {
            extractorName = extractorString.substring(0, extractorString.length() - 8);
            extractorType = ExtractorType.ET_RELATION;
            extractorEntity = ExtractorEntity.EE_OBJECTS;
        } else if (extractorString.substring(extractorString.length() - 9, extractorString.length()).equalsIgnoreCase(".entities")) {
            extractorName = extractorString.substring(0, extractorString.length() - 9);
            extractorType = ExtractorType.ET_ENTITY;
            extractorEntity = ExtractorEntity.EE_ENTITY;
        } else {
            System.out.println("NO CORRECT EXTRACTOR STRING GIVEN");
            return ids;
        }

        System.out.println(extractorName + " " + extractorEntity + " (" + extractorType + ")");

        switch (extractorType) {
         /*   case ET_RELATION:
                RelationExtractorModel model = DataStore.getInstance().getRelationExtractor(extractorName);
                if (model == null) return ids;
                System.out.println((depth + 1) + "RELATION RECURSION ----------");
                List<List<String>> eval = recursiveSuggestion(model.getPatterns(), model.getXTypes(), model.getYTypes(), depth + 1);
                System.out.println("---------- RECURSION " + (depth + 1));

                int num = extractorEntity.equals(ExtractorEntity.EE_SUBJECTS) ? 0 : 1;
                for (String output : eval.get(3)) {
                    PatternData patData = new Gson().fromJson(output, new TypeToken<PatternData>() {
                    }.getType());
                    ids.add(patData.getIdPair().split(" \\+ ")[num].replace("/m/", ""));
                }
                break;
            case ET_ENTITY:
                EntityExtractorModel entityModel = DataStore.getInstance().getEntityExtractor(extractorName);
                if (entityModel == null) return ids;
                System.out.println((depth + 1) + "ENTITY RECURSION ----------");
                List<List<String>> eval2 = EntityHandler.recursiveSuggestion(entityModel.getPatterns(), entityModel.getTypes(), depth + 1);
                System.out.println("---------- RECURSION " + (depth + 1));

                for (String output : eval2.get(2)) {
                    EntityData ed = new Gson().fromJson(output, new TypeToken<EntityData>() {
                    }.getType());
                    ids.add(ed.getId());
                }
                break;*/
        }

        return ids;
    }

}
