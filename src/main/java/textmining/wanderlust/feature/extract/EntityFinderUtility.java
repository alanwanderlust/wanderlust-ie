package textmining.wanderlust.feature.extract;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import textmining.wanderlust.nlp.domain.*;

import java.util.*;


public class EntityFinderUtility {

    public static List<Entity> locateNumericEntities(DependencyParse parse) {

        List<Entity> numericEntities = Lists.newArrayList();

        List<DepWord> orderedWords = parse.getOrderedWords();

        for (int i = 0; i < orderedWords.size(); i++) {
            if (orderedWords.get(i).getPosTag().startsWith("CD")){
                Entity entity = locateEntityInParse(parse, orderedWords.get(i).getWordName());
                if (entity != null) {
                    entity.setClasses("numeric");
                    entity.setUri(orderedWords.get(i).getWordName());
                    numericEntities.add(entity);
                }
            }
        }

        return numericEntities;
    }

    public static Entity locateEntityInParse(DependencyParse parse, String text) {

        int entity1TokenSize = text.split(" ").length;


        List<DepWord> orderedWords = parse.getOrderedWords();
        for (int i = 0; i < orderedWords.size(); i++) {

            String sentenceText = "";
            for (int j = i; j < orderedWords.size(); j++) {
                sentenceText += orderedWords.get(j).getWordName() + " ";
            }

            if (sentenceText.startsWith(text)) {

                Entity ner1 = new Entity(text);
               // System.out.println("text = " + text + "   FOUND");

                for (int j = i; j < i + entity1TokenSize; j++) {
                //    System.out.println(orderedWords.get(j));
                    ner1.addToken(orderedWords.get(j));
                }
                return ner1;
            }
        }

        return null;
    }

    public static Entity locateEntityInParse(DependencyParse parse, Entity entity) {

        String[] entity1Tokens = entity.getText().split(" ");

        Entity ner1 = null;

        for (int i = 0; i < parse.getOrderedWords().size(); i++) {
            DepWord term = parse.getOrderedWords().get(i);

            if (ner1 == null)
                ner1 = addEntityIfMatches(parse, entity1Tokens, i,
                        term);

        }

        return ner1;
    }

    public static List<Entity> findEntitiesFrom(DependencyParse depParse,
                                                EntityType entityType) {
        if (entityType.equals(EntityType.ALL))
            return findAllEntitiesFrom(depParse);

        List<Entity> entities = new ArrayList<Entity>();

        Entity ner = new Entity();

        for (DepWord term : depParse.getOrderedWords()) {

            if (isEntity(term, entityType)) {
                ner.addToken(term);
            } else {
                ner = flushEntities(entities, ner);
            }
        }

        flushEntitiesLast(entities, ner);
        //System.out.println(entities.size());
        return entities;
    }

    private static Entity addEntityIfMatches(DependencyParse depParse,
                                             String[] entity1Tokens, int i, DepWord term) {

        Entity ner = new Entity();

        if (term.getWordName().toLowerCase()
                .equals(entity1Tokens[0].toLowerCase())
                && (i + entity1Tokens.length) <= depParse.getOrderedWords()
                .size()) {

            boolean entityFound = true;
            for (int j = 0; j < entity1Tokens.length; j++) {

                DepWord depWord = depParse.getOrderedWords().get(i + j);
                ner.addToken(depWord);

                if (!depWord.getWordName().toLowerCase()
                        .equals(entity1Tokens[j].toLowerCase())) {
                    entityFound = false;
                }
            }

            if (entityFound)
                return ner;


        }
        return null;
    }

    private static List<DepWord> getYield(DepWord root, List<DepWord> trace) {

        List<DepWord> yield = Lists.newArrayList();

        yield.add(root);
        trace.add(root);

        List<DepLink> incoming = root.getOutgoingLinks();

        for (DepLink depLink : incoming) {
            if (depLink.getLinkLabel().equals("dep"))
                continue;

            if (trace.contains(depLink.getTargetWord()))
                continue;

            yield.addAll(getYield(depLink.getTargetWord(), trace));
        }

        return yield;
    }

    private static List<Entity> findAllEntitiesFrom(DependencyParse depParse) {

        Set<Entity> entities = Sets.newHashSet();

        for (DepWord token : depParse.getWords()) {

            if (token.getPosTag().startsWith("N")) {

                List<DepWord> yield = getYield(token, Lists.newArrayList(token));
                Collections.sort(yield);
                Entity entity = new Entity();
                for (DepWord np : yield) {
                    entity.addToken(np);
                }
                entities.add(entity);

            }
        }

        entities.addAll(findEntitiesFrom(depParse, EntityType.NOUN_PHRASE));

        return Lists.newArrayList(entities);
    }

    private static boolean isEntity(DepWord term, EntityType type) {

        if (type.equals(EntityType.NOUN_PHRASE))
            return isNounPhrase(term);
        return isProperNoun(term);
    }

    private static boolean isProperNoun(DepWord term) {
        return (term.getPosTag().startsWith("NNP"));
    }

    private static boolean isNounPhrase(DepWord term) {
        return term.getPosTag().startsWith("N");

    }

    private static boolean isNamedEntity(DepWord term) {

        return !term.getNerType().equals("O")
                && !term.getNerType().equals("none")
                && !term.getNerType().equals("PERCENT")
                && !term.getNerType().equals("NUMBER")
                && !term.getNerType().equals("ORDINAL")
                && !term.getNerType().equals("DATE")
                && !term.getNerType().equals("TIME")
                && !term.getNerType().equals("MONEY");
    }

    private static Entity flushEntities(List<Entity> entities, Entity ner) {
        if (ner.size() > 0) {
            entities.add(ner);
            ner = new Entity();
        }
        return ner;
    }

    private static void flushEntitiesLast(List<Entity> entities, Entity ner) {
        if (ner.size() > 0) {
            entities.add(ner);
        }
    }

}