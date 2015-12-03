package textmining.wanderlust.exploration.ui;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import textmining.wanderlust.exploration.ui.pattern.ExtractorLucene;
import textmining.wanderlust.exploration.ui.server.WebServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * This is the main class that starts the web server or
 * initializes the lucene indexes depending on which parameter is given
 *
 * @author Thilo Michael (thilo.michael@gmail.com)
 */
public class Main {

    /**
     * The logger for the main class
     */
    public static final Logger LOG = Logger.getLogger(Main.class);

    /**
     * The path from which the config should be loaded
     */
    public static final String CONFIG_PATH = "config";

    /**
     * The config of the program
     */
    public static Properties config;

    static {
        //Load properties file
        config = new Properties();
        try {
            config.load(new FileInputStream(new File("src/main/resources/config/schnaepper-config")));
        } catch (IOException e) {
            LOG.fatal("Couldn't find the confing", e);
            System.exit(-1);
        }
    }

    /**
     * If argument 'init-relation' is given, the lucene index for
     * the relation extraction is being created.
     * If argument 'init-entity' is given, the lucene index for
     * the entity extraction is being created.
     * If no argument is given, the web server is started.
     * If an unknown argument is given, the help is printed.
     *
     * @param args Arguments for the program
     */
    public static void main(String[] args) {
        //Initialize the logger
        BasicConfigurator.configure();

        if (args.length == 0) {
            runServer();
        } else if (args[0].equalsIgnoreCase("init-relation")) {
            initializeRelation();
            runServer();
        } else {
            printHelp();
        }


    }

    /**
     * This method runs the server on the port specified in the config
     */
    private static void runServer() {
        int port;

        try {
            port = new Integer(config.getProperty("port")).intValue();
        } catch (NumberFormatException e) {
            port = 8000;
        }

        //Startup web server
        WebServer server = new WebServer(port);
    }

    /**
     * This method initializes the lucene index for relation extraction
     */
    private static void initializeRelation() {
        LOG.info("INITIALIZE RELATION");

        String source = config.getProperty("pattern.source");
        String lucene = config.getProperty("lucene");

        LOG.info("Source folder:      " + source);
        LOG.info("Destination folder: " + lucene);

        ExtractorLucene luc = new ExtractorLucene(lucene);
        try {
            luc.indexFolder(source, true);
        } catch (IOException e) {
            LOG.error("Could not create relation index!", e);
        }

    }

    /**
     * This method prints the short help
     */
    private static void printHelp() {
        LOG.info("Run without parameters to start server on port specified in the config file.");
        LOG.info("Run with param 'init-relation' to initialize the relation extraction lucene index");
        LOG.info("Run with param 'init-entity' to initialize the entity extraction lucene index");
    }

}
