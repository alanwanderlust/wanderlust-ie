package textmining.wanderlust.feature.extract.pig.udfs;

import com.google.common.collect.Lists;
import org.apache.pig.EvalFunc;
import org.apache.pig.builtin.OutputSchema;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import textmining.wanderlust.feature.extract.BinaryPatternExtractor;
import textmining.wanderlust.feature.extract.EntityFinderUtility;
import textmining.wanderlust.feature.extract.ExtractorType;
import textmining.wanderlust.feature.extract.PatternTuple;
import textmining.wanderlust.nlp.domain.DependencyParse;
import textmining.wanderlust.nlp.domain.Entity;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by alan on 11/24/15.
 */

//@OutputSchema("features:bag {feature:tuple (entity1:chararray, entity2:chararray, feature:chararray, signature:chararray, sentence:chararray)}")
public class RelationExtractorUDF extends EvalFunc<DataBag> {

    private BagFactory bagFactory = BagFactory.getInstance();
    private TupleFactory tupleFactory = TupleFactory.getInstance();

    BinaryPatternExtractor extractor = new BinaryPatternExtractor(ExtractorType.EXPLORATORY_IE);


    @Override
    public DataBag exec(Tuple input) throws IOException {

        if (input == null) {
            return null;
        }

        // In this case, freebase id and type are passed
        if (input.size() == 8) {

            String sentence = input.get(0).toString();

            String parse = input.get(1).toString();
            if (parse.startsWith("(")) {
                parse = parse.substring(1, parse.length() - 1);
            }

            String entityOneText = input.get(2).toString();
            String entityTwoText = input.get(3).toString();

            DependencyParse dependencyParse = DependencyParse.parseJson(parse);
            Entity entity1 = EntityFinderUtility.locateEntityInParse(dependencyParse, entityOneText);
            Entity entity2 = EntityFinderUtility.locateEntityInParse(dependencyParse, entityTwoText);


            if (entity1 == null || entity2 == null) {
                return null;
            }

            entity1.setUri(input.get(4).toString());
            entity1.setClasses(input.get(6).toString());

            entity2.setUri(input.get(5).toString());
            entity2.setClasses(input.get(7).toString());

            List<PatternTuple> patternTuples = extractor.extract(dependencyParse, Lists.newArrayList(entity1, entity2));

            DataBag bag = bagFactory.newDefaultBag();
            for (PatternTuple patternTuple : patternTuples) {
                Tuple tuple = tupleFactory.newTuple(8);
                tuple.set(0, patternTuple.getSubject().getText());
                tuple.set(1, patternTuple.getObject().getText());
                tuple.set(2, patternTuple.getPattern());
                tuple.set(3, patternTuple.getSubject().getUri());
                tuple.set(4, patternTuple.getObject().getUri());
                tuple.set(5, patternTuple.getSubject().getClasses());
                tuple.set(6, patternTuple.getObject().getClasses());
                tuple.set(7, sentence);
                bag.add(tuple);
            }

            return bag;

        } else return null;
    }

}