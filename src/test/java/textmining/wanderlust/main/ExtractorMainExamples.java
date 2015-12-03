package textmining.wanderlust.main;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Created by tempid on 12/3/2015.
 */
public class ExtractorMainExamples {

    @Test
    public void exampleExecution(){

        String parsedSentenceFile = "sentences/annotated_sentences_freebase";
        parsedSentenceFile = "C:/Users/IBM_ADMIN/Documents/Data/LocalIndex/";

        parsedSentenceFile = "C:\\Users\\IBM_ADMIN\\Documents\\Data\\LocalIndex\\2000_sentences";
        String outputFile = "C:\\Users\\IBM_ADMIN\\Documents\\Data\\LocalIndex\\testoutput";
     //   parsedSentenceFile = "C:\\Users\\IBM_ADMIN\\Documents\\Data\\LocalIndex\\freebase_normalized_big";
        System.out.println("resource = " + parsedSentenceFile);
        String commandLine = "-h -i " + parsedSentenceFile + " -o " + outputFile;
        ExtractorMain.main(commandLine.split(" "));
    }
}
