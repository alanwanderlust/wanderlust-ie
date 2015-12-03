package textmining.wanderlust.exploration.ui.server.resources;

import org.apache.log4j.Logger;
import textmining.wanderlust.exploration.ui.server.WebServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.*;

/**
 * This class allows the access to
 * static files of a specified folder.
 * <p/>
 * The supported file formats are
 * - HTML
 * - CSS
 * - Javascript
 * - GIF
 * - PNG
 * <p/>
 * If a file could not be found an empty string is returned.
 * The files are always read from disk and will not be cached.
 */
@Path("/")
public class StaticHandler {
    /**
     * The logger for the file handler class
     */
    public static final Logger LOG = Logger.getLogger(StaticHandler.class);

    /**
     * Returns the index page
     *
     * @return The index page
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public String indexPage(@Context UriInfo uriInfo) {
        System.out.println("/ requested");
        String index = html("index");
        return index;
    }

    /**
     * Opens an HTML file inside the
     * static folder and returns its contents
     *
     * @param file The file to open
     * @return The contents of the HTML file
     */
    @GET
    @Path("{file}.html")
    @Produces(MediaType.TEXT_HTML)
    public String html(@PathParam("file") String file) {
        try {
            System.out.println(WebServer.WEB_DIRECTORY + "/" + file + ".html");
            BufferedReader br = new BufferedReader(new FileReader(WebServer.WEB_DIRECTORY + "/" + file + ".html"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            br.close();

            return sb.toString();
        } catch (FileNotFoundException e) {
            LOG.trace("HTML file " + file + ".html requested but not found!");
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Opens an HTML file inside the
     * static folder and returns its contents
     *
     * @param file The file to open
     * @return The contents of the HTML file
     */
    @GET
    @Path("{file}.css")
    @Produces("text/css")
    public String css(@PathParam("file") String file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(WebServer.WEB_DIRECTORY + "/" + file + ".css"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            br.close();

            return sb.toString();
        } catch (FileNotFoundException e) {
            LOG.trace("CSS file " + file + ".css requested but not found!");
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Opens a Java Script file inside the
     * static folder and returns its contents
     *
     * @param file The file to open
     * @return The contents of the Java Script file
     */
    @GET
    @Path("{file}.js")
    @Produces("text/javascript")
    public String javascript(@PathParam("file") String file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(WebServer.WEB_DIRECTORY + "/" + file + ".js"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            br.close();

            return sb.toString();
        } catch (FileNotFoundException e) {
            LOG.trace("Java Script file " + file + ".js requested but not found!");
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Opens a GIF image file inside the static
     * folder and returns its contents
     *
     * @param filename The file to open
     * @return A byte stream containing the image
     */
    @GET
    @Path("{file}.gif")
    @Produces("image/gif")
    public byte[] getGIF(@PathParam("file") String filename) {
        // Make a file object from the path name
        File file = new File(WebServer.WEB_DIRECTORY + "/" + filename + ".gif");
        // Find the size
        int size = (int) file.length();
        // Create a buffer big enough to hold the file
        byte[] contents = new byte[size];
        // Create an input stream from the file object
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            // Read it all
            in.read(contents);
            // Close the file
            in.close();
        } catch (FileNotFoundException e) {
            LOG.trace("GIF file " + file + ".gif requested but not found!");
        } catch (IOException e) {
            //
        }
        return contents;
    }

    /**
     * Opens a PNG image file inside the static
     * folder and returns its contents
     *
     * @param filename The file to open
     * @return A byte stream containing the image
     */
    @GET
    @Path("{file}.png")
    @Produces("image/png")
    public byte[] getPNG(@PathParam("file") String filename) {
        System.out.println(filename);
        // Make a file object from the path name
        File file = new File(WebServer.WEB_DIRECTORY + "/" + filename + ".png");
        // Find the size
        int size = (int) file.length();
        System.out.println(file.length());
        // Create a buffer big enough to hold the file
        byte[] contents = new byte[size];
        // Create an input stream from the file object
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            // Read it all
            in.read(contents);
            // Close the file
            in.close();
        } catch (FileNotFoundException e) {
            LOG.trace("PNG file " + file + ".png requested but not found!");
        } catch (IOException e) {
            //
        }
        return contents;
    }

    /**
     * A method to log requests that are not
     * handled by the web sever
     * This method always returns an empty string
     *
     * @param file The file to be requested
     * @return An empty string
     */
    @GET
    @Path("{file}")
    public Response file(@PathParam("file") String file) {
        LOG.trace("file " + file + " requested but not found!");
        return Response.status(404).entity("Could not find '" + file + "'!").build();
    }

}
