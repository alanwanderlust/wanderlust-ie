package textmining.wanderlust.feature.extract.pig.udfs;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.pigunit.PigTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Created by alan on 11/24/15.
 */
public class TestPigFeatureExtraction {

    private File tempDir;
    private static final Log LOG = LogFactory.getLog(TestPigFeatureExtraction.class);


    @Test
    public void testFreebaseFeatureScaping() throws Exception {

        String parsedSentenceFile = Resources.getResource("sentences/annotated_sentences_freebase").getPath();

        String pigFile = Resources.getResource("scripts/feature_scaper_freebase.pig").getPath();

        String location = tempDir.getPath().replaceAll("\\\\", "/");  // make it work on windows

        PigTest test = new PigTest(pigFile, new String[]{
                "parsedSentenceFolder=" + parsedSentenceFile,
                "testOutputFolder=" + location});
        test.unoverride("STORE");
        test.runScript();
    }


    @Test
    public void testFreebaseFeatureExtraction() throws Exception {

        String parsedSentenceFile = Resources.getResource("sentences/annotated_sentences_freebase").getPath();

        String pigFile = Resources.getResource("scripts/feature_extract_freebase.pig").getPath();

        String location = tempDir.getPath().replaceAll("\\\\", "/");  // make it work on windows

        PigTest test = new PigTest(pigFile, new String[]{
                "parsedSentenceFolder=" + parsedSentenceFile,
                "testOutputFolder=" + location});
        test.unoverride("STORE");
        test.runScript();
    }

    @Before
    public void setUp() throws Exception {
        // create a random file location
        tempDir = Files.createTempDir();
        LOG.info("Output can be found in " + tempDir.getPath());
    }

    @After
    public void tearDown() throws Exception {
        // cleanup
        // FileUtils.deleteRecursive(tempDir);
    }

}
