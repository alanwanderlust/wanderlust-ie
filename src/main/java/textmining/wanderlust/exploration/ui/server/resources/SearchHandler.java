package textmining.wanderlust.exploration.ui.server.resources;

import com.google.gson.Gson;
import textmining.wanderlust.exploration.ui.model.LuceneInterface;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * This class allows the access to searching
 * relations and entities
 *
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
@Path("/search/")
public class SearchHandler {

    public static final int DEFAULT_PATTERN_LIMIT = 100;
    public static final int DEFAULT_TYPE_LIMIT = 300;

    /**
     * Takes a string and returns all
     * patterns that include the string
     * (partial matching)
     *
     * @param query The partial to search for
     * @return A json-List of all the patterns that match
     */
    @GET
    @Path("pattern/{pattern}")
    @Produces(MediaType.APPLICATION_JSON)
    public String searchPattern(@PathParam("pattern") String query) {
        query = RelationHandler.decodePattern(query);
        List<String> patterns = LuceneInterface.getInstance().searchPatterns(query, DEFAULT_PATTERN_LIMIT);
        return new Gson().toJson(patterns);
    }

    @GET
    @Path("subject/{subject}")
    @Produces(MediaType.APPLICATION_JSON)
    public String searchSubject(@PathParam("subject") String query) {
        List<String> subjects = LuceneInterface.getInstance().searchSubjects(query, DEFAULT_TYPE_LIMIT);
        return new Gson().toJson(subjects);
    }

    @GET
    @Path("object/{object}")
    @Produces(MediaType.APPLICATION_JSON)
    public String searchObject(@PathParam("object") String query) {
        List<String> objects = LuceneInterface.getInstance().searchObjects(query, DEFAULT_TYPE_LIMIT);
        return new Gson().toJson(objects);
    }

}
