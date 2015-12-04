package textmining.wanderlust.nlp.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.emory.clir.clearnlp.component.AbstractComponent;
import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration;
import edu.emory.clir.clearnlp.component.mode.srl.SRLConfiguration;
import edu.emory.clir.clearnlp.component.utils.GlobalLexica;
import edu.emory.clir.clearnlp.component.utils.NLPUtils;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer;
import edu.emory.clir.clearnlp.util.lang.TLanguage;
import textmining.wanderlust.feature.extract.BinaryPatternExtractor;
import textmining.wanderlust.feature.extract.EntityFinderUtility;
import textmining.wanderlust.feature.extract.ExtractorType;
import textmining.wanderlust.feature.extract.PatternTuple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Created by alan on 2/4/14.
 */
public class ClearNLPParser {

    final TLanguage language = TLanguage.ENGLISH;

    private AbstractComponent[] components;
    private AbstractTokenizer tokenizer;


    public ClearNLPParser() throws IOException {

        tokenizer = NLPUtils.getTokenizer(language);
        components = getGeneralModels(language);

    }


    private AbstractComponent[] getGeneralModels(TLanguage language) {
        // initialize global lexicons
        List<String> paths = Lists.newArrayList();
        paths.add("brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz");

        GlobalLexica.initDistributionalSemanticsWords(paths);

        // initialize statistical models
        AbstractComponent morph = NLPUtils.getMPAnalyzer(language);
        AbstractComponent pos = NLPUtils.getPOSTagger(language, "general-en-pos.xz");
        AbstractComponent dep = NLPUtils.getDEPParser(language, "general-en-dep.xz", new DEPConfiguration("root"));
        AbstractComponent srl = NLPUtils.getSRLabeler(language, "general-en-srl.xz", new SRLConfiguration(4, 3));

        GlobalLexica.initNamedEntityDictionary("general-en-ner-gazetteer.xz");
        AbstractComponent ner = NLPUtils.getNERecognizer(language, "general-en-ner.xz");

        return new AbstractComponent[]{pos, morph, dep, ner};
    }

    private DEPTree toDEPTree(String line) {
        List<String> tokens = tokenizer.tokenize(line);
        edu.emory.clir.clearnlp.dependency.DEPTree tree = new edu.emory.clir.clearnlp.dependency.DEPTree(tokens);

        for (edu.emory.clir.clearnlp.component.AbstractComponent component : components)
            component.process(tree);

        return tree;
    }

    public DependencyParse parse(String sentence) {
        edu.emory.clir.clearnlp.dependency.DEPTree tree = toDEPTree(sentence);

        Map<Integer, DepWord> idWordMap = Maps.newHashMap();
        List<DepWord> words = Lists.newArrayList();

        for (edu.emory.clir.clearnlp.dependency.DEPNode depNode : tree) {
            if (depNode.getLabel() == null) continue;
            DepWord word = new DepWord(depNode.getID(), depNode.getWordForm(), depNode.getPOSTag(), depNode.getNamedEntityTag(), depNode.getLemma());
            idWordMap.put(depNode.getID(), word);
            words.add(word);
        }

        for (edu.emory.clir.clearnlp.dependency.DEPNode depNode : tree) {

            if (depNode.getLabel() == null) continue;


            DepWord to = idWordMap.get(depNode.getID());


            DepWord from = idWordMap.get(depNode.getHead().getID());
            if (from == null) continue;
            DepLink link = new DepLink(from, depNode.getLabel(), to);

            from.getOutgoingLinks().add(link);
            to.getIncomingLinks().add(link);

        }

        DependencyParse parse = new DependencyParse(words);
        return parse;
    }


    public static void main(String[] args) {

        try {

            ClearNLPParser parser = new ClearNLPParser();
            DependencyParse parse = parser.parse("Albert Einstein was born in Berlin.");
            System.out.println(parse.toJson());

            List<Entity> entities = Lists.newArrayList();
            entities.add(EntityFinderUtility.locateEntityInParse(parse, new Entity("Albert Einstein", "id1")));
            entities.add(EntityFinderUtility.locateEntityInParse(parse, new Entity("Berlin", "id1")));

            // determine entity pairs in the sentence - the strategy depends on the entityType that is passed here
            //     List<Entity> entities = EntityFinderUtility.findEntitiesFrom(parse, entityType);

            BinaryPatternExtractor b = new BinaryPatternExtractor(ExtractorType.EXPLORATORY_IE);

            List<PatternTuple> extract = b.extract(parse, entities);

            List<String> foundPattern = Lists.newArrayList();

            for (PatternTuple patternTuple : extract) {
                System.out.println("patternTuple.get(3) = " + patternTuple);
                System.out.println("patternTuple.get(3) = " + patternTuple);

                //      }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
