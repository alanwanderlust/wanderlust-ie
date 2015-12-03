package textmining.wanderlust.nlp.domain;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class DepWord.
 */
public class DepWord implements Comparable<DepWord>, Serializable {

    public static final String NULL_NER_TYPE = "O";
    /**
     *
     */
    private static final long serialVersionUID = -753411031733880379L;

    /**
     * The word pos id.
     */
    private int wordPosId;

    /**
     * The pos tag.
     */
    private String posTag;

    /**
     * The word name.
     */
    private String wordName;

    /**
     * The word name.
     */
    private String wordLemma;

    /**
     * The ner type.
     */
    private String nerType = "";

    private boolean isVerb = false;

    private boolean isCopularNoun = false;

    /**
     * The dep links.
     */
    private List<DepLink> upLinks = new ArrayList<DepLink>();

    /**
     * The dep links.
     */
    private List<DepLink> downLinks = new ArrayList<DepLink>();

    public List<DepLink> getIncomingLinks() {
        return upLinks;
    }

    public void setUpLinks(List<DepLink> upLinks) {
        this.upLinks = upLinks;
    }

    public List<DepLink> getOutgoingLinks() {
        return downLinks;
    }

    public void setDownLinks(List<DepLink> downLinks) {
        this.downLinks = downLinks;
    }

    public DepWord getIncomingWordWithLabel(String label) {

            for (DepLink downLink : upLinks) {
                if (downLink.getLinkLabel().equals(label)) return downLink.getTargetWord();
            }
            return null;
        }

    public DepWord getOutgoingWordWithLabel(String label) {

        for (DepLink downLink : downLinks) {
            if (downLink.getLinkLabel().equals(label)) return downLink.getTargetWord();
        }
        return null;
    }

    /**
     * Instantiates a new dep word.
     *
     * @param wordPosId the word pos id
     * @param wordName  the word name
     * @param posTag    the pos tag
     */
    public DepWord(int wordPosId, String wordName, String posTag) {
        super();
        this.wordPosId = wordPosId;
        this.wordName = wordName;
        this.posTag = posTag;
    }

    public DepWord(int wordPosId, String wordName, String posTag,
                   String nerType, String wordLemma) {
        super();
        this.wordPosId = wordPosId;
        this.wordName = wordName;
        this.posTag = posTag;
        this.nerType = nerType;
        this.wordLemma = wordLemma;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof DepWord) {
            if (this.getWordPosId() == ((DepWord) obj).getWordPosId())
                return true;
        }
        // TODO Auto-generated method stub
        return super.equals(obj);
    }

    public String getDownLinkLabels() {
        String ret = " ";
        for (DepLink link : this.getOutgoingLinks()) {
            ret += link.getLinkLabel() + " ";

        }
        return ret;
    }

    public String getUpLinkLabels() {
        String ret = " ";
        for (DepLink link : this.getIncomingLinks()) {
            ret += link.getLinkLabel() + " ";

        }
        return ret;
    }

    @Override
    public int hashCode() {

        return this.getWordPosId();
        // TODO Auto-generated method stub
        // return super.hashCode();
    }

    public String toStringShort() {
        return this.getWordName() + " (" + this.getPosTag() + ")";
    }

    public String toString() {

        return this.toStringShort();

//		String str = "Id: " + wordPosId + " , Name: " + wordName + ", POS: "
//				+ posTag + ", ner: " + nerType;
//
//		str += ", Links: " + this.downLinks.size();
//
//		for (DepLink depLink : downLinks) {
//			str += "\n to " + depLink.getTargetWord().getWordName();
//
//		}
//		for (DepLink depLink : upLinks) {
//			str += "\n from " + depLink.getOriginWord().getWordName();
//
//		}

        //	return str;
    }

    /**
     * Gets the word pos id.
     *
     * @return the word pos id
     */
    public int getWordPosId() {
        return wordPosId;
    }

    /**
     * Sets the word pos id.
     *
     * @param wordPosId the new word pos id
     */
    public void setWordPosId(int wordPosId) {
        this.wordPosId = wordPosId;
    }

    /**
     * Gets the word name.
     *
     * @return the word name
     */
    public String getWordName() {
        return wordName;
    }

    /**
     * Sets the word name.
     *
     * @param wordName the new word name
     */
    public void setWordName(String wordName) {
        this.wordName = wordName;
    }

    /**
     * Gets the pos tag.
     *
     * @return the pos tag
     */
    public String getPosTag() {
        return posTag;
    }

    /**
     * Sets the pos tag.
     *
     * @param posTag the new pos tag
     */
    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public boolean isVerb() {
        return isVerb;
    }

    public void setVerb(boolean isVerb) {
        this.isVerb = isVerb;
    }

    public boolean isCopularNoun() {
        return isCopularNoun;
    }

    public void setCopularNoun(boolean isCopularNoun) {
        this.isCopularNoun = isCopularNoun;
    }

    @Override
    public int compareTo(DepWord o) {
        if (this.getWordPosId() < o.getWordPosId())
            return -1;
        return 1;
    }

    public String getNerType() {
        return nerType;
    }

    public void setNerType(String nerType) {
        this.nerType = nerType;
    }

    public String getWordLemma() {
        return wordLemma;
    }

    public void setWordLemma(String wordLemma) {
        this.wordLemma = wordLemma;
    }

    public JSONObject toJson() {

        try {

            JSONObject argJson = new JSONObject();

            argJson.put("wordPosId", this.getWordPosId());
            argJson.put("wordName", this.getWordName());
            argJson.put("wordLemma", this.getWordLemma());
            argJson.put("posTag", this.getPosTag());
            argJson.put("nerType", this.getNerType());
            argJson.put("isVerb", this.isVerb());
            argJson.put("isCopularNoun", this.isCopularNoun());

            JSONArray downlinks = new JSONArray();
            for (DepLink downLink : this.getOutgoingLinks()) {
                JSONObject link = new JSONObject();
                link.put("label", downLink.getLinkLabel());
                link.put("targetId", downLink.getTargetWord().getWordPosId());
                downlinks.put(link);
            }

            JSONArray uplinks = new JSONArray();
            for (DepLink upLink : this.getIncomingLinks()) {
                JSONObject link = new JSONObject();
                link.put("label", upLink.getLinkLabel());
                link.put("targetId", upLink.getOriginWord().getWordPosId());
                uplinks.put(link);
            }

            argJson.put("downlinks", downlinks);
            argJson.put("uplinks", uplinks);

            return argJson;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public DepLink getLinkTo(DepWord depWord) {

        for (DepLink link : this.getOutgoingLinks()) {

            if (link.getTargetWord().equals(depWord))
                return link;

        }
        for (DepLink link : this.getIncomingLinks()) {

            if (link.getOriginWord().equals(depWord))
                return link;

        }
        return null;
    }
}
