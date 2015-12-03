package textmining.wanderlust.nlp.domain;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;


public class Entity {

    private String text;
    private String uri;
    private String classes;
    private List<DepWord> tokens = Lists.newArrayList();

    public Entity() {
    }

    public Entity(String text) {
        this.text = text;
    }

    public Entity(String text, String uri) {
        this.text = text;
        this.uri = uri;
    }

    public Entity(String text, String uri, String classes) {
        this.text = text;
        this.uri = uri;
        this.classes = classes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }


    public void addToken(DepWord token) {
        if (this.tokens != null) {
            this.tokens.add(token);
        }
    }

    public List<DepWord> getTokens() {
        return tokens;
    }

    public void setTokens(List<DepWord> tokens) {
        this.tokens = tokens;
    }

    public int size() {
        return this.tokens.size();
    }

    public DepWord getLastToken() {
        return this.tokens.get(size() - 1);
    }

    public DepWord getFirstToken() {
        return this.tokens.get(0);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DepWord token : getTokens()) {
            builder.append(" ").append(token.getWordName());
        }
        return builder.toString().trim();
    }

    public String toStringLemma() {
        StringBuilder builder = new StringBuilder();
        for (DepWord token : getTokens()) {
            builder.append(" ").append(token.getWordLemma());
        }
        return builder.toString().trim();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof Entity) {

            if (((Entity) obj).getTokens().size() == this.getTokens().size()) {

                for (int i = 0; i < this.getTokens().size(); i++) {

                    if (!((Entity) obj).getTokens().get(i)
                            .equals(this.getTokens().get(i))) {
                        return false;
                    }

                }

                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {

        int hash = 0;
        for (int i = 0; i < this.getTokens().size(); i++) {

            hash += this.getTokens().get(i).hashCode();

        }
        return hash;
    }

}
