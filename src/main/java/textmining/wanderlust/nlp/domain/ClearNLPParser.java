package textmining.wanderlust.nlp.domain;

import com.clearnlp.component.AbstractComponent;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.nlp.NLPMode;
import com.clearnlp.reader.AbstractReader;
import com.clearnlp.segmentation.AbstractSegmenter;
import com.clearnlp.tokenization.AbstractTokenizer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

    final String language = AbstractReader.LANG_EN;

    AbstractTokenizer tokenizer = NLPGetter.getTokenizer(language);
    AbstractComponent tagger;
    AbstractComponent parser;
    AbstractComponent identifier;
    AbstractComponent classifier;
    AbstractComponent labeler;


    public ClearNLPParser() throws IOException {

        tagger = NLPGetter.getComponent("general-en", language, NLPMode.MODE_POS);
        parser = NLPGetter.getComponent("general-en", language, NLPMode.MODE_DEP);
      //  identifier = NLPGetter.getComponent("general-en", language, NLPMode.MODE_PRED);
      //  classifier = NLPGetter.getComponent("general-en", language, NLPMode.MODE_ROLE);
      //  labeler = NLPGetter.getComponent("general-en", language, NLPMode.MODE_SRL);

    }


    public DependencyParse parse(String sentence) {
        DEPTree tree = NLPGetter.toDEPTree(tokenizer.getTokens(sentence));
        List<AbstractComponent> components = Lists.newArrayList(tagger, parser);

        for (AbstractComponent component : components)
            component.process(tree);


        Map<Integer, DepWord> idWordMap = Maps.newHashMap();
        List<DepWord> words = Lists.newArrayList();

        for (DEPNode depNode : tree) {
            if (depNode.getLabel() == null) continue;
            DepWord word = new DepWord(depNode.id, depNode.form, depNode.pos, DepWord.NULL_NER_TYPE, depNode.lemma);
            idWordMap.put(depNode.id, word);
            words.add(word);
        }

        for (DEPNode depNode : tree) {

            if (depNode.getLabel() == null) continue;


            DepWord to = idWordMap.get(depNode.id);
            DepWord from = idWordMap.get(depNode.getHeadArc().getNode().id);
            if (from == null) continue;


            DepLink link = new DepLink(from, depNode.getHeadArc().getLabel(), to);

            from.getOutgoingLinks().add(link);
            to.getIncomingLinks().add(link);

        }

        DependencyParse parse = new DependencyParse(words);

        return parse;
    }

    public void process(AbstractTokenizer tokenizer, AbstractComponent[] components, BufferedReader reader, PrintStream fout) {
        AbstractSegmenter segmenter = NLPGetter.getSegmenter(language, tokenizer);
        DEPTree tree;

        for (List<String> tokens : segmenter.getSentences(reader)) {
            tree = NLPGetter.toDEPTree(tokens);

            for (AbstractComponent component : components)
                component.process(tree);

            fout.println(tree.toStringSRL() + "\n");
        }

        fout.close();
    }

    public static void main(String[] args) {

        try {

            ClearNLPParser parser = new ClearNLPParser();
            DependencyParse parse = parser.parse("As far as I know , Russians would much prefer to hate Chinese than to hate the Europeans .");
            System.out.println(parse.toJson());

            List<Entity> entities = Lists.newArrayList();
            entities.add(EntityFinderUtility.locateEntityInParse(parse, new Entity("Chinese", "id1")));
            entities.add(EntityFinderUtility.locateEntityInParse(parse, new Entity("Europeans", "id1")));

            // determine entity pairs in the sentence - the strategy depends on the entityType that is passed here
       //     List<Entity> entities = EntityFinderUtility.findEntitiesFrom(parse, entityType);

            BinaryPatternExtractor b = new BinaryPatternExtractor(ExtractorType.EXPLORATORY_IE);

            List<PatternTuple> extract = b.extract(parse, entities);

            List<String> foundPattern = Lists.newArrayList();

            for (PatternTuple patternTuple : extract) {
                System.out.println("patternTuple.get(3) = " + patternTuple);
                System.out.println("patternTuple.get(3) = " + patternTuple.get(1));
                foundPattern.add(patternTuple.get(1));
                //      }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
