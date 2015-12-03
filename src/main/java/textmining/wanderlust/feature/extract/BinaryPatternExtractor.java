package textmining.wanderlust.feature.extract;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.UndirectedSubgraph;
import textmining.wanderlust.nlp.domain.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alan
 * Date: 7/10/13
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class BinaryPatternExtractor {


    private int maxTokensInSentence = 20;

    private int maxTokensInSubtree = 4;

    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    private boolean flatten = false;

    // add part-of-speech tag information to pattern
    boolean addPos = true;


    private Set<String> subtreeWhitelist = Sets.newHashSet();
    private boolean useWhitelist = false;

    public BinaryPatternExtractor(ExtractorType extractorType) {

        this.useWhitelist = false;

        if (extractorType.equals(ExtractorType.EXPLORATORY_IE)) {

            final URL mappingFile = Resources.getResource("wanderlust-files/exploratory-paths");

            subtreeWhitelist = Sets.newHashSet();
            try {
                final List<String> lines = Resources.readLines(mappingFile, Charsets.UTF_8);
                for (String line : lines) {
                    subtreeWhitelist.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.useWhitelist = true;
        }


    }

    public List<PatternTuple> extract(DependencyParse dependencyParse, List<Entity> entities) {

        // this list stores the results of the feature extraction
        List<PatternTuple> allTuples = Lists.newArrayList();

        try {

            // make a graph object out of the dependency parse
            UndirectedGraph<DepWord, DependencyEdge> graph = makeDependencyGraph(dependencyParse);
            if (graph == null) {
                return null;
            }

            for (Entity entityOne : entities) {
                for (Entity entityTwo : entities) {

                    // produce patterns for each entity pair and add to result list
                    List<PatternTuple> tuples = producePatternsForEntityPair(graph, entityOne, entityTwo);
                    if (tuples != null) allTuples.addAll(tuples);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return allTuples;
    }

    /**
     * Inserts all dependencies of an annotated sentence into a graph
     * with governor and dependent as vertices and dependency type as
     * edge.
     *
     * @return UndirectedGraph of dependencies
     */
    public UndirectedGraph<DepWord, DependencyEdge> makeDependencyGraph(DependencyParse dependencyParse) {

        UndirectedGraph<DepWord, DependencyEdge> graph = new SimpleGraph<DepWord, DependencyEdge>(DependencyEdge.class);

        // we can limit the graph a bit
        // as we are only interested in (selectionType) like NERs, we can skip sentences which have < 2 NERs.

        DepWord governor = null;
        DepWord dependent = null;
        String dependencyType = null;

        List<String> backlistedDependencies = Lists.newArrayList("npadvmod", "advcl");
        //  backlistedDependencies = Lists.newArrayList();

        try {
            List<DepLink> dependencies = dependencyParse.getDependencies();

            for (DepLink dependency : dependencies) {

                if (dependency.getLinkLabel().equals("root")) continue;

                governor = dependency.getTargetWord();
                dependent = dependency.getOriginWord();
                dependencyType = dependency.getLinkLabel();

                if (backlistedDependencies.contains(dependencyType)) continue;

                graph.addVertex(governor);
                graph.addVertex(dependent);
                graph.addEdge(governor, dependent, new DependencyEdge(governor, dependent, dependencyType));

            }

        } catch (Exception e) {
            e.printStackTrace();

        }


        return graph;
    }

    /*
    Determines the head word of an entity
     */
    public DepWord determineHead(Entity entity) {

        List<DepWord> tokens = entity.getTokens();
        for (DepWord token : tokens) {

            boolean notUnderOtherToken = true;
            for (DepLink incomingLink : token.getIncomingLinks()) {

                if (tokens.contains(incomingLink.getOriginWord())) {
                    notUnderOtherToken = false;
                }
            }
            if (notUnderOtherToken)
                return token;
        }
        return null;
    }


    /**
     * Extracts patterns for a given entity pair and dependency dependencyGraph.
     *
     * @param dependencyGraph
     * @return DataBag dataBag     a data bag of ( entity_pair, pattern ) tuples
     * with the shortest path as pattern
     */
    public List<PatternTuple> producePatternsForEntityPair(final UndirectedGraph<DepWord, DependencyEdge> dependencyGraph, Entity entityOne, Entity entityTwo) {

        List<PatternTuple> dataBag = Lists.newArrayList();

        // sanity check
        if (entityOne == null || entityTwo == null) return dataBag;

        // get the "head" word of each entity (the "head" is the word from which all other entity words can be reached
        // through downward links - for example in "Crescent Moon" the head is "Moon")
        DepWord e1head = determineHead(entityOne);
        DepWord e2head = determineHead(entityTwo);

        // sanity check
        if (e1head == null || e2head == null) return dataBag;

        try {

            // check if sentence is too long - skip if so (all subtrees on long sentences can blow up the index)
            if (dependencyGraph.vertexSet().size() > this.maxTokensInSentence) return null;

            // generate tokens of pattern(s) - if we use only the shortest path, then the tokens of the pattern are
            // exactly the shortest path. If we use all subtrees, then generate the power set of all non-entity tokens
            // in dependencyGraph.
            Set<Set<DepWord>> subgraphs = Sets.newHashSet();

            // first we filter out all (the) determiners to make index smaller (this could be commented out)
            Set<DepWord> tokens = Sets.newHashSet();
            Set<DepWord> temp = dependencyGraph.vertexSet();
            for (DepWord depWord : temp) {
                boolean add = true;
                if (depWord.getPosTag().startsWith("D")) add = false;
                if (depWord.getUpLinkLabels().contains("aux")) add = false;

                if (add) tokens.add(depWord);
            }

            // then we filter out all entity tokens
            tokens.removeAll(entityOne.getTokens());
            tokens.removeAll(entityTwo.getTokens());

            // finally, we generate the power set
            subgraphs = Sets.powerSet(tokens);

            // this is so that we generate no pattern more than once for each entity pair and sentence
            Set<String> patternMemory = Sets.newHashSet();

            for (Set<DepWord> possibleSubgraph : subgraphs) {

                // only handle subtrees to a certain size
                if (possibleSubgraph.size() > this.maxTokensInSubtree) continue;

                // using the tokens, we create a subgraph of the dependency dependencyGraph and check if it is connected
                Set<DepWord> subgraphTokens = Sets.newHashSet(possibleSubgraph);
                subgraphTokens.add(e1head);
                subgraphTokens.add(e2head);
                if (!isConnectedSubgraph(dependencyGraph, subgraphTokens)) continue;


                SortedSet<Map.Entry<DepWord, Double>> entries = sortGraphTokensByPosition(subgraphTokens);

                // make signature
                String signature = makePosSignature(e1head, e2head, entries);

                if (useWhitelist && !subtreeWhitelist.contains(signature)) continue;


                // now look at optional tokens
                Set<DepWord> optionalConnectedTokens = determineOptionalConnectedTokens(subgraphTokens, Lists.newArrayList("amod", "nn"));

                for (DepWord optionalConnectedToken : optionalConnectedTokens) {

                    Set<DepWord> subgraphWithOptionalToken = Sets.newHashSet(subgraphTokens);
                    subgraphWithOptionalToken.add(optionalConnectedToken);

                    String pattern = makePatternString(e1head, e2head, subgraphWithOptionalToken);

                    if (patternMemory.contains(pattern)) continue;
                    patternMemory.add(pattern);

                    if (flatten) {
                        PatternTuple tuple = new PatternTuple(entityOne, entityTwo, pattern);
                        tuple.setSignature(signature);
                        dataBag.add(tuple);
                    }

                }


                // add mandatory tokens (some subgraphs never make sense as some token combinations should never
                // be split - for example negations)
                addMandatoryConnectedTokens(subgraphTokens, entries, "neg");
                addMandatoryConnectedTokens(subgraphTokens, entries, "prt");
                //   addMandatoryPathTokens(copy, subgraph, tokenPosition, entries, "conj", "cc");
                //   addMandatoryPathTokens(subgraphTokens, subgraph, entries, "poss", "possessive");


                // here the "magic" happens - a textual representation of the pattern is generated
                String pattern = makePatternString(e1head, e2head, subgraphTokens);

                if (patternMemory.contains(pattern)) continue;
                patternMemory.add(pattern);

                if (flatten) {
                    PatternTuple tuple = new PatternTuple(entityOne, entityTwo, pattern);
                    tuple.setSignature(signature);
                    dataBag.add(tuple);
                }
            }

            if (!flatten && patternMemory.size() > 0) {
                PatternTuple tuple = new PatternTuple(entityOne, entityTwo, patternMemory.toString());
                dataBag.add(tuple);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return dataBag;
    }

    private boolean isConnectedSubgraph(UndirectedGraph<DepWord, DependencyEdge> dependencyGraph, Set<DepWord> subgraphTokens) {
        UndirectedSubgraph<DepWord, DependencyEdge> subgraph = new UndirectedSubgraph<DepWord, DependencyEdge>(dependencyGraph, subgraphTokens, dependencyGraph.edgeSet());
        ConnectivityInspector<DepWord, DependencyEdge> inspector = new ConnectivityInspector<DepWord, DependencyEdge>(subgraph);

        // if not connected, skip subgraph
        if (inspector.isGraphConnected()) return true;
        return false;
    }

    private SortedSet<Map.Entry<DepWord, Double>> sortGraphTokensByPosition(Set<DepWord> subgraphTokens) {
        Map<DepWord, Double> tokenPosition = Maps.newHashMap();
        for (DepWord subgraphToken : subgraphTokens) {
            tokenPosition.put(subgraphToken, (double) subgraphToken.getWordPosId());
        }

        return entriesSortedByValues(tokenPosition);
    }

    private String makePosSignature(DepWord e1head, DepWord e2head, SortedSet<Map.Entry<DepWord, Double>> entries) {
        StringBuilder posSignature = new StringBuilder();
        for (Map.Entry<DepWord, Double> entry : entries) {

            DepWord token = entry.getKey();
            String pos = "";
            if (token.equals(e1head)) {
                pos = "x ";
            } else if (token.equals(e2head)) {
                pos = "y ";
            } else {
                pos = token.getPosTag().substring(0, 1) + " ";
            }
            posSignature.append(pos);
        }

        return posSignature.toString().trim();
    }

    private void addMandatoryPathTokens(Set<DepWord> copy, UndirectedSubgraph<DepWord, DependencyEdge> subgraph, SortedSet<Map.Entry<DepWord, Double>> entries, String triggerLabel, String addLabel) {
        boolean containsConj = false;
        for (Map.Entry<DepWord, Double> entry : entries) {
            Set<DependencyEdge> dependencyEdges = subgraph.edgesOf(entry.getKey());

            for (DependencyEdge edge : dependencyEdges) {
                if (edge.dependency.equals(triggerLabel)) {
                    containsConj = true;
                    break;
                }
            }
        }
        if (containsConj) {
            addMandatoryConnectedTokens(copy, entries, addLabel);
        }
    }


    private Set<DepWord> determineOptionalConnectedTokens(Set<DepWord> subgraphTokens, List<String> labels) {
        Set<DepWord> optionalTokens = Sets.newHashSet();
        for (String label : labels) {
            for (DepWord token : subgraphTokens) {
                if (token.getDownLinkLabels().contains(label)) {
                    //    negated = true;
                    DepWord optional = token.getOutgoingWordWithLabel(label);
                    if (!subgraphTokens.contains(optional)) optionalTokens.add(optional);
                }
                if (token.getUpLinkLabels().contains(label)) {
                    //    negated = true;
                    DepWord optional = token.getIncomingWordWithLabel(label);
                    if (!subgraphTokens.contains(optional)) optionalTokens.add(optional);
                }
            }
        }
        return optionalTokens;
    }

    private void addMandatoryConnectedTokens(Set<DepWord> copy, SortedSet<Map.Entry<DepWord, Double>> entries, String label) {
        for (Map.Entry<DepWord, Double> entry : entries) {
            if (entry.getKey().getDownLinkLabels().contains(label)) {
                //    negated = true;
                DepWord neg = entry.getKey().getOutgoingWordWithLabel(label);
                copy.add(neg);
            }
            if (entry.getKey().getUpLinkLabels().contains(label)) {
                //    negated = true;
                DepWord neg = entry.getKey().getIncomingWordWithLabel(label);
                copy.add(neg);
            }
        }
    }


    private String makePatternString(DepWord e1head,
                                     DepWord e2head,
                                     Set<DepWord> subgraphTokens) {

        SortedSet<Map.Entry<DepWord, Double>> entries = sortGraphTokensByPosition(subgraphTokens);
        StringBuilder pattern = new StringBuilder();

        for (Map.Entry<DepWord, Double> entry : entries) {

            DepWord token = entry.getKey();

            String appendText = "";
            if (token.equals(e1head)) {
                appendText = "[X] ";
            } else if (token.equals(e2head)) {
                appendText = "[Y] ";
            } else if (!token.getNerType().equals(DepWord.NULL_NER_TYPE)) {
                appendText = token.getNerType() + " ";
            } else if (token.getPosTag().startsWith("VBN")) {
                if (addPos) appendText = token.getWordName() + ".v ";
                else appendText = token.getWordName() + " ";
            } else if (token.getPosTag().startsWith("VBG")) {
                if (addPos) appendText = token.getWordName() + ".v ";
                else appendText = token.getWordName() + " ";
            } else if (token.getPosTag().startsWith("V")) {
                if (addPos) appendText = token.getWordLemma() + ".v ";
                else appendText = token.getWordLemma() + " ";
            } else if (token.getPosTag().startsWith("J")) {
                if (addPos) appendText = token.getWordLemma() + ".j ";
                else appendText = token.getWordLemma() + " ";
            } else if (token.getPosTag().startsWith("N")) {
                appendText = token.getWordLemma() + ".n ";
            } else appendText = token.getWordLemma() + " ";

            pattern.append(appendText);

        }
        return pattern.toString().trim();
    }

    private static class DependencyEdge {
        private DepWord from;
        private DepWord to;
        private String dependency;

        private DependencyEdge(DepWord from, DepWord to, String dependency) {
            this.from = from;
            this.to = to;
            this.dependency = dependency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyEdge that = (DependencyEdge) o;

            if (dependency != null ? !dependency.equals(that.dependency) : that.dependency != null) return false;
            if (from != null ? !from.equals(that.from) : that.from != null) return false;
            if (to != null ? !to.equals(that.to) : that.to != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = from != null ? from.hashCode() : 0;
            result = 31 * result + (to != null ? to.hashCode() : 0);
            result = 31 * result + (dependency != null ? dependency.hashCode() : 0);
            return result;
        }
    }


    static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(
            Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        if (e1.getValue().compareTo(e2.getValue()) == 0)
                            return -1;
                        return e1.getValue().compareTo(e2.getValue());
                    }
                });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }


}
