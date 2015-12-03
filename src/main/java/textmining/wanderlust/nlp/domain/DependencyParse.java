package textmining.wanderlust.nlp.domain;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

public class DependencyParse implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5352592485673555414L;

    private List<DepWord> words;

    private List<DepWord> pathBetweenWords = null;

    /**
     * Lazily create bimap.
     *
     * @return the positionWordMap
     */
    public BiMap<Integer, DepWord> getPositionWordMap() {
        if (positionWordMap == null) {
            positionWordMap = HashBiMap.create(words.size());
            for (DepWord depWord : words) {
                positionWordMap.put(depWord.getWordPosId(), depWord);
            }
        }
        return positionWordMap;
    }

    private BiMap<Integer, DepWord> positionWordMap;

    public DependencyParse(List<DepWord> words) {
        this.words = words;
    }

    public String makeFeatureVector() {
        StringBuilder vector = new StringBuilder();
        String origin;
        String target;
        for (DepWord word : words) {

            for (DepLink link : word.getOutgoingLinks()) {

                if (link.getOriginWord().getNerType().equals("O")) {
                    origin = link.getOriginWord().getWordLemma();
                } else
                    origin = link.getOriginWord().getNerType();

                if (link.getTargetWord().getNerType().equals("O")) {
                    target = link.getTargetWord().getWordLemma();
                } else
                    target = link.getTargetWord().getNerType();

                vector.append(origin + "\t" + link.getLinkLabel() + "\t"
                        + target + "\n");

            }

        }

        return vector.toString();
    }

    /**
     * Finds the root element of the parse.
     *
     * @return the word that is the root element
     */
    public DepWord getRoot() {
        // TODO improve if necessary
        // TODO check for correctness
        for (DepWord word : words) {
            if (word.getPosTag().equals("none"))
                return word;
        }

        return null;
    }

    public String getSentence() {

        String sentence = "";
        for (DepWord word : getOrderedWords()) {
            sentence += word.getWordName() + " ";
        }
        return sentence.trim();
    }

    public List<DepWord> getAfterNoun(DepWord end) {

        List<DepWord> beforeNoun = new ArrayList<DepWord>();

        boolean read = false;
        for (DepWord depWord : this.getOrderedWords()) {

            if (read) {
                if (depWord.getPosTag().startsWith("N"))
                    beforeNoun.add(depWord);
                else
                    break;
            }

            if (depWord.getWordPosId() == end.getWordPosId())
                read = true;

        }

        return beforeNoun;

    }

    public List<DepWord> getBeforeNoun(DepWord begin) {

        List<DepWord> beforeNoun = new ArrayList<DepWord>();

        for (DepWord depWord : this.getOrderedWords()) {

            if (depWord.getWordPosId() == begin.getWordPosId())
                break;

            if (depWord.getPosTag().startsWith("N"))
                beforeNoun.add(depWord);
            else
                beforeNoun = new ArrayList<DepWord>();

        }

        return beforeNoun;

    }


    public List<DepWord> getBetweenWordsPrePost(DepWord beginFirst, DepWord beginLast, DepWord endFirst, DepWord endLast) {

        List<DepWord> betweenWords = new ArrayList<DepWord>();


        int beginId = beginFirst.getWordPosId();
        int endId = endLast.getWordPosId();

        boolean read = false;
        for (DepWord depWord : this.getOrderedWords()) {

            if (depWord.getWordPosId() == beginId - 1) betweenWords.add(depWord);
            if (depWord.getWordPosId() == beginId - 2) betweenWords.add(depWord);
            if (depWord.getWordPosId() == endId + 1) betweenWords.add(depWord);
            if (depWord.getWordPosId() == endId + 2) betweenWords.add(depWord);

            if (depWord.getWordPosId() == endFirst.getWordPosId())
                read = false;

            if (read)
                betweenWords.add(depWord);

            if (depWord.getWordPosId() == beginLast.getWordPosId())
                read = true;
        }

        return betweenWords;

    }

    public List<DepWord> getBetweenWords(DepWord begin, DepWord end) {

        List<DepWord> betweenWords = new ArrayList<DepWord>();

        boolean read = false;
        for (DepWord depWord : this.getOrderedWords()) {

            if (depWord.getWordPosId() == end.getWordPosId())
                break;

            if (read)
                betweenWords.add(depWord);

            if (depWord.getWordPosId() == begin.getWordPosId())
                read = true;
        }

        return betweenWords;

    }

    public List<DepWord> getOrderedWords() {

		/* ordered list with posId of depWords, without the ROOT element */

        List<DepWord> orderedWords = new ArrayList<DepWord>();

        for (int i = 1; i <= words.size(); i++) {
            for (DepWord depWord : words) {
                int pos = depWord.getWordPosId();
                if (pos == i) {
                    orderedWords.add(depWord);
                }
            }
        }

        return orderedWords;
    }

    public List<DepWord> getWords() {
        return words;
    }

    public void setWords(List<DepWord> words) {
        this.words = words;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String separator = " + ";

        for (DepWord token : this.getOrderedWords()) {
            sb.append(token.toStringShort() + token.getNerType() + separator);
        }
        return sb.length() > separator.length() ? sb.substring(0, sb.length() - separator.length() - 1) : "";
    }

    public JSONObject toJson() {

        try {

            JSONObject argJson = new JSONObject();

            JSONArray argArray = new JSONArray();

            for (DepWord word : this.words) {
                argArray.put(word.toJson());
            }

            argJson.put("words", argArray);

            return argJson;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static DependencyParse parseJson(String json) throws JSONException {

        JSONObject obj;

        obj = new JSONObject(json.trim());

        JSONArray words = obj.getJSONArray("words");

        HashMap<Integer, DepWord> wordMap = new HashMap<Integer, DepWord>();
        List<DepWord> depWords = new ArrayList<DepWord>();

        for (int i = 0; i < words.length(); i++) {
            JSONObject arg = words.getJSONObject(i);
            DepWord word = new DepWord(arg.getInt("wordPosId"),
                    arg.getString("wordName"), arg.getString("posTag"),
                    arg.getString("nerType"), arg.getString("wordLemma"));
            word.setCopularNoun(arg.getBoolean("isCopularNoun"));
            word.setVerb(arg.getBoolean("isVerb"));

            wordMap.put(arg.getInt("wordPosId"), word);
            depWords.add(word);
        }

		/*
         * all ids have been established
		 */
        for (int i = 0; i < words.length(); i++) {

            JSONObject arg = words.getJSONObject(i);

            int originId = arg.getInt("wordPosId");

            List<DepLink> downlinksArray = new ArrayList<DepLink>();
            List<DepLink> uplinksArray = new ArrayList<DepLink>();

			/*
             * downlinks
			 */
            JSONArray downlinks = arg.getJSONArray("downlinks");

            for (int j = 0; j < downlinks.length(); j++) {

                JSONObject link = downlinks.getJSONObject(j);
                DepLink downlink = new DepLink(wordMap.get(originId),
                        link.getString("label"), wordMap.get(link
                        .getInt("targetId")));
                downlinksArray.add(downlink);
            }
            wordMap.get(originId).setDownLinks(downlinksArray);

			/*
             * uplinks
			 */
            JSONArray uplinks = arg.getJSONArray("uplinks");

            for (int j = 0; j < uplinks.length(); j++) {

                JSONObject link = uplinks.getJSONObject(j);
                DepLink uplink = new DepLink(wordMap.get(link
                        .getInt("targetId")), link.getString("label"),
                        wordMap.get(originId));
                uplinksArray.add(uplink);
            }
            wordMap.get(originId).setUpLinks(uplinksArray);
        }

        DependencyParse parse = new DependencyParse(depWords);

        return parse;

    }

    private void getPath(List<DepWord> pathSoFar, DepWord currentWord,
                         DepWord objectWord) {

        for (DepLink link : currentWord.getOutgoingLinks()) {

            DepWord targetWord = link.getTargetWord();
            if (pathSoFar.contains(targetWord))
                continue;

            List<DepWord> newPath = Lists.newArrayList(pathSoFar);
            newPath.add(targetWord);

            if (targetWord.equals(objectWord)) {

                pathBetweenWords = newPath;
            } else {
                getPath(newPath, targetWord, objectWord);

            }
        }

        for (DepLink link : currentWord.getIncomingLinks()) {

            DepWord originWord = link.getOriginWord();
            if (pathSoFar.contains(originWord))
                continue;

            List<DepWord> newPath = Lists.newArrayList(pathSoFar);
            newPath.add(originWord);

            if (originWord.equals(objectWord)) {

                pathBetweenWords = newPath;
            } else {
                getPath(newPath, originWord, objectWord);

            }
        }

    }

    private Set<DepWord> getExtraWordsOnPath(Collection<DepWord> path,
                                             Collection<DepWord> exclude, List<String> extraLinkLabelsDown,
                                             List<String> extraLinkLabelsUp) {

        Set<DepWord> extraWords = new TreeSet<DepWord>();

        for (DepWord depWord : path) {

            List<DepLink> outgoingLinks = depWord.getOutgoingLinks();
            for (DepLink depLink : outgoingLinks) {
                if (extraLinkLabelsDown.contains(depLink.getLinkLabel())) {

                    if (!path.contains(depLink.getTargetWord())
                            && !exclude.contains(depLink.getTargetWord())) {

                        extraWords.add(depLink.getTargetWord());
                    }
                }
            }

            List<DepLink> incomingLinks = depWord.getIncomingLinks();
            for (DepLink depLink : incomingLinks) {
                if (extraLinkLabelsUp.contains(depLink.getLinkLabel())) {

                    if (!path.contains(depLink.getOriginWord())
                            && !exclude.contains(depLink.getOriginWord())) {

                        if (!depLink.getOriginWord().getPosTag()
                                .startsWith("NNP"))
                            extraWords.add(depLink.getOriginWord());
                    }
                }
            }

        }

        return extraWords;
    }

    public Set<DepWord> getMandatoryExtraWordsOnPath(Set<DepWord> path,
                                                     Collection<DepWord> exclude) {

        List<String> extraLinkLabelsDown = new ArrayList<String>();
        extraLinkLabelsDown.add("neg");
        extraLinkLabelsDown.add("prt");
        extraLinkLabelsDown.add("possessive");
        // extraLinkLabelsDown.add("nn");
        // extraLinkLabelsDown.add("poss");

        List<String> extraLinkLabelsUp = new ArrayList<String>();

        Set<DepWord> extraWordsOnPath = this.getExtraWordsOnPath(path, exclude,
                extraLinkLabelsDown, extraLinkLabelsUp);

        Set<DepWord> filtered = new HashSet<DepWord>();

        for (DepWord depWord : extraWordsOnPath) {
            if ((depWord.getPosTag().equals("NN") || (depWord.getPosTag()
                    .equals("NNS"))))
                continue;
            filtered.add(depWord);
        }

        return filtered;
    }

    public Set<DepWord> getExtraWordsOnPath(Collection<DepWord> path,
                                            Collection<DepWord> exclude) {

        List<String> extraLinkLabelsDown = new ArrayList<String>();
        extraLinkLabelsDown.add("nn");

        List<String> extraLinkLabelsUp = new ArrayList<String>();
        extraLinkLabelsUp.add("nsubjpass");

        Set<DepWord> extraWordsOnPath = this.getExtraWordsOnPath(path, exclude,
                extraLinkLabelsDown, extraLinkLabelsUp);

        Set<DepWord> filtered = new HashSet<DepWord>();
        for (DepWord depWord : extraWordsOnPath) {

            if (depWord.getPosTag().startsWith("NNP"))
                continue;
            filtered.add(depWord);

        }

        return filtered;
    }

    public List<DepWord> getPath(DepWord subjectHead, DepWord objectHead) {

        pathBetweenWords = null;

        List<DepWord> path = Lists.newArrayList(subjectHead);

        for (DepLink link : subjectHead.getIncomingLinks()) {

            List<DepWord> newPath = Lists.newArrayList(path);
            newPath.add(link.getOriginWord());

            if (link.getOriginWord().equals(objectHead)) {
                pathBetweenWords = newPath;
            } else {
                getPath(newPath, link.getOriginWord(), objectHead);
            }
        }

        for (DepLink link : subjectHead.getOutgoingLinks()) {

            List<DepWord> newPath = Lists.newArrayList(path);
            newPath.add(link.getTargetWord());

            if (link.getTargetWord().equals(objectHead)) {
                pathBetweenWords = newPath;
            } else {
                getPath(newPath, link.getTargetWord(), objectHead);
            }
        }

        return pathBetweenWords;
    }

    public List<DepLink> getDependencies() {

        List<DepLink> dependencies = Lists.newArrayList();

        for (DepWord depWord : this.getWords()) {
            dependencies.addAll(depWord.getIncomingLinks());
        }

        return dependencies;
    }

    public Collection<? extends DepWord> getPrefix(DepWord firstToken) {

        int wordPosId = firstToken.getWordPosId();
        List<DepWord> prefix = Lists.newArrayList();
        for (DepWord word : words) {
            if (word.getWordPosId() == wordPosId - 1) prefix.add(word);
            if (word.getWordPosId() == wordPosId - 2) prefix.add(word);
        }
        return prefix;
    }

    public Collection<? extends DepWord> getPostfix(DepWord firstToken) {

        int wordPosId = firstToken.getWordPosId();
        List<DepWord> prefix = Lists.newArrayList();
        for (DepWord word : words) {
            if (word.getWordPosId() == wordPosId + 1) prefix.add(word);
            if (word.getWordPosId() == wordPosId + 2) prefix.add(word);
        }
        return prefix;
    }

    /**
     * Helper class for de-serialization.
     */
    private class WordList {
        List<Word> words;
    }

    /**
     * Helper class to parse uplink and downlink information.
     */
    private class Word {
        int wordPosId;
        List<DependencyHolder> downlinks;
        List<DependencyHolder> uplinks;
    }

    /**
     * Simple Value holder to access the up and downlink information for a word.
     */
    private class DependencyHolder {
        int targetId;
        String label;
    }

}
