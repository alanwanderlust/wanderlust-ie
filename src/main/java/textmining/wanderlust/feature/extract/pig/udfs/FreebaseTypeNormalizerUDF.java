package textmining.wanderlust.feature.extract.pig.udfs;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by alan on 11/25/15.
 */
public class FreebaseTypeNormalizerUDF extends EvalFunc<Tuple> {

    Map<String, String> typeMapping = Maps.newHashMap();


    public FreebaseTypeNormalizerUDF(){

        final URL mappingFile = Resources.getResource("mapping/figer-mapping");

        try {
            final List<String> lines = Resources.readLines(mappingFile, Charsets.UTF_8);
            for (String line : lines) {

                typeMapping.put(line.split("\t")[0], line.split("\t")[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Tuple exec(Tuple input) throws IOException {


        String stringTypes = input.get(0).toString();
        List<String> types = Lists.newArrayList(stringTypes.substring(2, stringTypes.length() - 2).split("\\),\\("));

        Set<String> typesNormalized = Sets.newHashSet();
        for (String type : types) {
            if (typeMapping.containsKey(type)) typesNormalized.add(typeMapping.get(type));
        }

        Tuple normalizedString = TupleFactory.getInstance().newTuple(1);
        normalizedString.set(0, typesNormalized.toString());
        return normalizedString;
    }
}
