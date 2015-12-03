package textmining.wanderlust.exploration.ui.pattern;


import com.google.common.collect.Lists;

import java.util.List;

public class PatternData {

    private String pattern;
    private String subject;
    private String object;


    private String pair = null;
    private String idPair = null;
    private String sentence;
    private List<String> subjectTypes = Lists.newArrayList();

    public List<String> getObjectTypes() {
        return objectTypes;
    }

    public List<String> getSubjectTypes() {
        return subjectTypes;
    }

    private List<String> objectTypes = Lists.newArrayList();
    private String label;


    public PatternData(String pattern, String subject, String object, String pair, String idPair,
                       String sentence, String label, List<String> subjectTypes, List<String> objectTypes) {
        super();
        this.pattern = pattern;
        this.subject = subject;
        this.object = object;
        this.pair = pair;
        this.idPair = idPair;
        this.sentence = sentence;
        this.label = label;

        this.subjectTypes = subjectTypes;
        this.objectTypes = objectTypes;
    }

    public PatternData(String pattern, String subject, String object,
                       String sentence, String label) {
        super();
        this.pattern = pattern;
        this.subject = subject;
        this.object = object;
        this.sentence = sentence;
        this.label = label;
    }

    public String getPattern() {
        return pattern;
    }

    public String getSubject() {

        return subject;
    }

    public String getObject() {
        return object;
    }

    public String getSentence() {
        return sentence;
    }

    public String getLabel() {
        return label;
    }



    public String getIdPair() {
        return idPair;
    }

    public String getPair() {
        if (pair == null) return getSubject() + " + " + getObject();

        return pair;
    }

    @Override
    public String toString() {
        return "PatternData{" +
                "pattern='" + pattern + '\'' +
                ", subject='" + subject + '\'' +
                ", object='" + object + '\'' +
                ", sentence='" + sentence + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
