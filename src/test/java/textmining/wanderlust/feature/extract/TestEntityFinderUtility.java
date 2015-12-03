package textmining.wanderlust.feature.extract;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;
import textmining.wanderlust.nlp.domain.DependencyParse;
import textmining.wanderlust.nlp.domain.Entity;

import java.util.List;

/**
 * Created by alanwanderlust on 11/24/15.
 */
public class TestEntityFinderUtility {

    @Test
    public void testNumericEntityInSentence() {

        DependencyParse dependencyParse = DependencyParse.parseJson("{\"words\":[{\"wordLemma\":\"einstein\",\"nerType\":\"O\",\"wordPosId\":1,\"downlinks\":[{\"targetId\":2,\"label\":\"punct\"},{\"targetId\":3,\"label\":\"partmod\"},{\"targetId\":5,\"label\":\"punct\"}],\"isVerb\":false,\"posTag\":\"NNP\",\"isCopularNoun\":false,\"wordName\":\"Einstein\",\"uplinks\":[{\"targetId\":6,\"label\":\"nsubj\"}]},{\"wordLemma\":\",\",\"nerType\":\"O\",\"wordPosId\":2,\"downlinks\":[],\"isVerb\":false,\"posTag\":\",\",\"isCopularNoun\":false,\"wordName\":\",\",\"uplinks\":[{\"targetId\":1,\"label\":\"punct\"}]},{\"wordLemma\":\"bear\",\"nerType\":\"O\",\"wordPosId\":3,\"downlinks\":[{\"targetId\":4,\"label\":\"dobj\"}],\"isVerb\":false,\"posTag\":\"VBN\",\"isCopularNoun\":false,\"wordName\":\"born\",\"uplinks\":[{\"targetId\":1,\"label\":\"partmod\"}]},{\"wordLemma\":\"0\",\"nerType\":\"O\",\"wordPosId\":4,\"downlinks\":[],\"isVerb\":false,\"posTag\":\"CD\",\"isCopularNoun\":false,\"wordName\":\"1800\",\"uplinks\":[{\"targetId\":3,\"label\":\"dobj\"}]},{\"wordLemma\":\",\",\"nerType\":\"O\",\"wordPosId\":5,\"downlinks\":[],\"isVerb\":false,\"posTag\":\",\",\"isCopularNoun\":false,\"wordName\":\",\",\"uplinks\":[{\"targetId\":1,\"label\":\"punct\"}]},{\"wordLemma\":\"have\",\"nerType\":\"O\",\"wordPosId\":6,\"downlinks\":[{\"targetId\":1,\"label\":\"nsubj\"},{\"targetId\":8,\"label\":\"dobj\"},{\"targetId\":9,\"label\":\"punct\"}],\"isVerb\":false,\"posTag\":\"VBD\",\"isCopularNoun\":false,\"wordName\":\"had\",\"uplinks\":[]},{\"wordLemma\":\"#crd#\",\"nerType\":\"O\",\"wordPosId\":7,\"downlinks\":[],\"isVerb\":false,\"posTag\":\"CD\",\"isCopularNoun\":false,\"wordName\":\"two\",\"uplinks\":[{\"targetId\":8,\"label\":\"num\"}]},{\"wordLemma\":\"child\",\"nerType\":\"O\",\"wordPosId\":8,\"downlinks\":[{\"targetId\":7,\"label\":\"num\"}],\"isVerb\":false,\"posTag\":\"NNS\",\"isCopularNoun\":false,\"wordName\":\"children\",\"uplinks\":[{\"targetId\":6,\"label\":\"dobj\"}]},{\"wordLemma\":\".\",\"nerType\":\"O\",\"wordPosId\":9,\"downlinks\":[],\"isVerb\":false,\"posTag\":\".\",\"isCopularNoun\":false,\"wordName\":\".\",\"uplinks\":[{\"targetId\":6,\"label\":\"punct\"}]}]}");

        List<String> expectedNumericEntities = Lists.newArrayList("two", "1800");

        List<Entity> entities = EntityFinderUtility.locateNumericEntities(dependencyParse);

        verifyEntities(expectedNumericEntities, entities);

        Entity entity = EntityFinderUtility.locateEntityInParse(dependencyParse, "Einstein");
        verifyEntity("Einstein", entity);

    }


    @Test
    public void testEntityInSentence() {

        DependencyParse dependencyParse = DependencyParse.parseJson("{\"words\":[{\"wordLemma\":\"einstein\",\"nerType\":\"O\",\"wordPosId\":1,\"downlinks\":[],\"isVerb\":false,\"posTag\":\"NNP\",\"isCopularNoun\":false,\"wordName\":\"Einstein\",\"uplinks\":[{\"targetId\":3,\"label\":\"nsubjpass\"}]},{\"wordLemma\":\"be\",\"nerType\":\"O\",\"wordPosId\":2,\"downlinks\":[],\"isVerb\":false,\"posTag\":\"VBD\",\"isCopularNoun\":false,\"wordName\":\"was\",\"uplinks\":[{\"targetId\":3,\"label\":\"auxpass\"}]},{\"wordLemma\":\"bear\",\"nerType\":\"O\",\"wordPosId\":3,\"downlinks\":[{\"targetId\":1,\"label\":\"nsubjpass\"},{\"targetId\":2,\"label\":\"auxpass\"},{\"targetId\":4,\"label\":\"prep\"},{\"targetId\":6,\"label\":\"punct\"}],\"isVerb\":false,\"posTag\":\"VBN\",\"isCopularNoun\":false,\"wordName\":\"born\",\"uplinks\":[]},{\"wordLemma\":\"in\",\"nerType\":\"O\",\"wordPosId\":4,\"downlinks\":[{\"targetId\":5,\"label\":\"pobj\"}],\"isVerb\":false,\"posTag\":\"IN\",\"isCopularNoun\":false,\"wordName\":\"in\",\"uplinks\":[{\"targetId\":3,\"label\":\"prep\"}]},{\"wordLemma\":\"berlin\",\"nerType\":\"O\",\"wordPosId\":5,\"downlinks\":[],\"isVerb\":false,\"posTag\":\"NNP\",\"isCopularNoun\":false,\"wordName\":\"Berlin\",\"uplinks\":[{\"targetId\":4,\"label\":\"pobj\"}]},{\"wordLemma\":\".\",\"nerType\":\"O\",\"wordPosId\":6,\"downlinks\":[],\"isVerb\":false,\"posTag\":\".\",\"isCopularNoun\":false,\"wordName\":\".\",\"uplinks\":[{\"targetId\":3,\"label\":\"punct\"}]}]}");

        Entity entity = EntityFinderUtility.locateEntityInParse(dependencyParse, "Einstein");
        verifyEntity("Einstein", entity);

    }

    private void verifyEntity(String expected, Entity found) {

        List<String> expectedList = Lists.newArrayList(expected);
        List<Entity> foundList = Lists.newArrayList(found);
        verifyEntities(expectedList, foundList);
    }

    private void verifyEntities(List<String> expectedNumericEntities, List<Entity> entities) {
        for (String expectedNumericEntity : expectedNumericEntities) {

            boolean expectedEntityFound = false;
            for (Entity entity : entities) {
                if (entity.getText().equals(expectedNumericEntity)) {
                    System.out.println(expectedNumericEntity + " found!");
                    expectedEntityFound = true;
                }
            }
            if (!expectedEntityFound) System.out.println(expectedNumericEntity + " NOT found!");
            Assert.assertTrue(expectedEntityFound);
        }
    }

}
