package textmining.wanderlust.main;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.junit.Test;
import textmining.wanderlust.feature.extract.BinaryPatternExtractor;
import textmining.wanderlust.feature.extract.EntityFinderUtility;
import textmining.wanderlust.feature.extract.ExtractorType;
import textmining.wanderlust.feature.extract.PatternTuple;
import textmining.wanderlust.nlp.domain.DependencyParse;
import textmining.wanderlust.nlp.domain.Entity;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by alan on 11/26/15.
 */
public class ExtractorMain {

    public static void main(String[] args) {

        ExtractorType extractorType = ExtractorType.EXPLORATORY_IE;
        String pathToInput = null;
        String pathToOutput = null;
        String sentence = null;
        int cores = 1;

        System.out.println("--- Wanderlust Exploratory Information Extractor ---");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                System.out.println("--- Options: ");
            }
            if (arg.equals("-i")) {
                pathToInput = args[i + 1];
            }
            if (arg.equals("-o")) {
                pathToOutput = args[i + 1];
            }
            if (arg.equals("-s")) {
                sentence = args[i + 1];
            }
            if (arg.equals("-mc")) {
                cores = Integer.parseInt(args[i + 1]);
            }
        }

        if (pathToInput == null) {
            System.out.println("Path to input data NOT SET!");
            return;

        }
        System.out.println("----------------------------------------------------");
        System.out.println("Path to input data: " + pathToInput);
        System.out.println("Extraction type: " + extractorType);

        try {
            ExtractorMain.extractFromFile(extractorType, pathToInput, pathToOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void extractFromFile(ExtractorType extractorType, String pathToParsedSentences, String pathToOutputFile) throws IOException {

        BinaryPatternExtractor extractor = new BinaryPatternExtractor(extractorType);

        List<PatternTuple> patternTuples;
        DependencyParse dependencyParse;
        Entity entity1;
        Entity entity2;

        BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutputFile));

        BufferedReader br = new BufferedReader(new FileReader(pathToParsedSentences));
        String line = br.readLine();

        long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();

        int counter = 0;
        while (line != null) {

            if (counter++ % 100 == 0) {

                currentTime = System.currentTimeMillis();
                long s = currentTime - startTime;
                System.out.printf("Loading: %3.2f \t Eclipsed: %s \r", (float) counter, String.format("%d:%02d", s / 60000, (s % 60000) / 1000));

            }

            String[] fields = line.split("\t");
            String entityOneText = fields[1];
            String entityTwoText = fields[2];
            String parseText = fields[7];
            if (parseText.startsWith("(")) parseText = fields[7].substring(1, fields[7].length() - 1);

            dependencyParse = DependencyParse.parseJson(parseText);

            entity1 = EntityFinderUtility.locateEntityInParse(dependencyParse, entityOneText);
            entity2 = EntityFinderUtility.locateEntityInParse(dependencyParse, entityTwoText);

            if (entity1 == null || entity2 == null) {
                line = br.readLine();
                continue;
            }

            String sentence = fields[0];
            //  System.out.println("sentence = " + sentence);
            String freebaseId1 = fields[3];
            String freebaseTypes1 = fields[4];
            String freebaseId2 = fields[5];
            String freebaseTypes2 = fields[6];

            entity1.setUri(freebaseId1);
            entity1.setClasses(freebaseTypes1);

            entity2.setUri(freebaseId2);
            entity2.setClasses(freebaseTypes2);

            patternTuples = extractor.extract(dependencyParse, Lists.newArrayList(entity1, entity2));

            StringBuilder sb = new StringBuilder();
            for (PatternTuple patternTuple : patternTuples) {
                sb.append(patternTuple.getSubject().getText());
                sb.append("\t");
                sb.append(patternTuple.getObject().getText());
                sb.append("\t");
                sb.append(patternTuple.getPattern());
                sb.append("\t");
                sb.append(sentence);
                sb.append("\t");
                sb.append(patternTuple.getSubject().getUri());
                sb.append("\t");
                sb.append(patternTuple.getObject().getUri());
                sb.append("\t");
                sb.append(patternTuple.getSubject().getClasses());
                sb.append("\t");
                sb.append(patternTuple.getObject().getClasses());
                sb.append("\n");
            }
            writer.append(sb.toString());

            line = br.readLine();
        }
        writer.close();

    }
}