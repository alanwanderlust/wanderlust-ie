package textmining.wanderlust.feature.extract;


import com.google.common.collect.Maps;
import textmining.wanderlust.nlp.domain.Entity;

import java.util.Map;

/**
 * This is a helper class that is basically an integer-string map.
 * This is used to mediate between feature extraction methods and pig functions. Fields will
 * typically be filled with datapoint-feature tuples plus extra information.
 * Earlier classes had explicit fields but for easier experimentation it was converted to a map.
 * <p/>
 * Created by alan on 12/7/13.
 */
public class PatternTuple {

    private Entity subject = null;
    private Entity object = null;
    private String pattern = null;


    private String origin = null;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    private String signature = null;

    public PatternTuple(Entity subject, Entity object, String pattern) {
        this.subject = subject;
        this.object = object;
        this.pattern = pattern;
    }

    public Entity getSubject() {
        return subject;
    }

    public Entity getObject() {
        return object;
    }

    public String getPattern() {
        return pattern;
    }

    Map<Integer, String> internalMap = Maps.newHashMap();

    public void set(int i, String s) {
        internalMap.put(i, s);
    }

    public String get(int i) {
        return internalMap.get(i);
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Override
    public String toString() {
        return subject + " --- " + pattern + " ---> " + object;
    }
}
