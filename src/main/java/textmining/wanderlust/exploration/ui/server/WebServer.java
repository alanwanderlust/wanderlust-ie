package textmining.wanderlust.exploration.ui.server;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.Logger;
import textmining.wanderlust.exploration.ui.server.resources.*;

import java.io.IOException;

/**
 * The WebServer class that opens up a web server on the given port
 * and serves the information needed for the service
 *
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
public class WebServer {

    /**
     * The logger of the web server class
     */
    public static final Logger LOG = Logger.getLogger(WebServer.class);
    /**
     * The directory from which the files should be server
     */
    public static final String WEB_DIRECTORY = "C:/Users/IBM_ADMIN/Documents/Data/svn_backup/staff-alan-akbik/textmining/patternreader-ui/static/";

    /**
     * The classes that should handle the requests coming in
     */
    public static final Class[] WEB_RESOURCES = {
            RelationHandler.class,
            SearchHandler.class,
            StaticHandler.class
    };

    private HttpServer server;

    /**
     * Instantiates a new web server that listens
     * at the given port
     *
     * @param port The port the web server should listen on
     */
    public WebServer(int port) {
        //Log only on errors
        java.util.logging.Logger.getLogger("com.sun.jersey").setLevel(java.util.logging.Level.SEVERE);

        //Point the server to the classes that should handle requests
        ResourceConfig rc = new ClassNamesResourceConfig(WEB_RESOURCES);

        try {
            LOG.info("Starting server on port " + port);
            server = HttpServerFactory.create("http://127.0.0.1:" + port + "/", rc);
            server.start();
        } catch (IOException e) {
            server = null;
            LOG.error("Could not initialize web server on port " + port);
            LOG.error(e);
        }
    }


}
