package textmining.wanderlust.exploration.ui.pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by alan on 12/11/13.
 */
public class ExtractorLucene {

    Version LUCENE_VERSION = Version.LUCENE_4_10_2;

    Analyzer analyzer = new WhitespaceAnalyzer(LUCENE_VERSION);

    Directory index;

    //   RAMDirectory index = new RAMDirectory();

    IndexReader reader;

    IndexSearcher searcher;

    private List<ExtractedType> allTypes = null;

    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION,
            analyzer);

    public ExtractorLucene(String indexPath) {
        try {
               index = new MMapDirectory(new File(indexPath));
     //       index = new NIOFSDirectory(new File(indexPath));

            //  System.out.println(index.getMaxChunkSize());
            // System.out.println(index.getUseUnmap());

            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Returns a list of vectors which contain the number
     * of occurrences in the first position and the Type in
     * the second position.
     * @return List of Vectors containing Types with count
     */
    public List<ExtractedType> getAllTypes() {
        if (allTypes != null)
            return allTypes;
        try {
            System.out.println("Lazily generating type list...");
            TermsEnum t = SlowCompositeReaderWrapper.wrap(reader).terms("subjectTypes").iterator(null);
            BytesRef br = t.next();
            List<ExtractedType> types = Lists.newArrayList();
            while (br != null) {
                if (t.seekExact(br)) {
                    DocsEnum de = t.docs(MultiFields.getLiveDocs(reader), null);
                    if (de != null) {
                        int counter = 0;
                        while (de.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                            counter += de.freq();
                        }
                        types.add(new ExtractedType(counter,br.utf8ToString()));
                    }
                }
                br = t.next();
            }
            allTypes = types;
            return types;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<PatternData> queryEntityPairs(Collection<String> entityPairs, int maxHitsNumber) {

        List<PatternData> hitList = Lists.newArrayList();

        String queryText = "";

        int count = 0;
        for (String entityPair : entityPairs) {
            if (count++ > 1020) break;
            queryText += "idPair:\"" + entityPair + "\" OR ";

            //   queryText += "(subject:\"" + entityPair.split(" \\+ ")[0] + "\" AND object:\"" + entityPair.split(" \\+ ")[1] + "\") OR ";
        }
        queryText = queryText.substring(0, queryText.length() - 3);

        QueryParser queryParser = new QueryParser(LUCENE_VERSION, "idPair", analyzer);

        Query query = null;
        try {
            System.out.println("entityPairSearch");
            double t1 = System.currentTimeMillis();
            query = queryParser.parse(queryText);

            TopDocs results = searcher.search(query, maxHitsNumber);
            ScoreDoc[] hits = results.scoreDocs;
            System.out.println("entityPairSearch took " + (System.currentTimeMillis()-t1) + "ms");

            Set<Integer> ids = Sets.newHashSet();

            // 4. display results
            if (hits.length == 0) return hitList;

            float bestScore = hits[0].score;
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                float score = hits[i].score;
                //          if (score < bestScore) return hitList;
                Document d = searcher.doc(docId);

                //   System.out.println(score + " d = " + d);

                if (ids.contains(docId))
                    continue;
                ids.add(docId);

                hitList.add(
                        new PatternData(
                                d.get("pattern"),
                                d.get("pair").split(" \\+ ")[0],
                                d.get("pair").split(" \\+ ")[1],
                                d.get("pair"),
                                d.get("idPair"),
                                d.get("sentence"),
                                d.get("label"),
                                Lists.newArrayList(d.get("subjectTypes").split(" ")),
                                Lists.newArrayList(d.get("objectTypes").split(" "))));


                if (ids.size() > maxHitsNumber)
                    break;
            }

            // searcher can only be closed when there
            // is no need to access the documents any more.
            // searcher.clo();
            // reader.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hitList;
    }

    public List<PatternData> queryEntityPair(String entityPair, int maxHitsNumber) {

        return queryEntityPairs(Lists.newArrayList(entityPair), maxHitsNumber);
    }

    public List<PatternData> queryIndex(String queryText, int maxHitsNumber) {
        return queryIndex(queryText, "", "", Lists.newArrayList(""), Lists.newArrayList(""), maxHitsNumber);
    }

    public List<PatternData> queryIndex(String queryText, Collection<String> subjectTypes, Collection<String> objectTypes, int maxHitsNumber) {
        return queryIndex(queryText, "", "", subjectTypes, objectTypes, maxHitsNumber);
    }


    public List<PatternData> queryLabel(String label, int maxHitsNumber) {

        List<PatternData> hitList = Lists.newArrayList();

        label = "label:\"" + label + "\" ";


        QueryParser queryParser = new QueryParser(LUCENE_VERSION, "object", analyzer);

        Query query = null;
        try {
            query = queryParser.parse(label);


            TopDocs results = searcher.search(query, maxHitsNumber);
            ScoreDoc[] hits = results.scoreDocs;

            Set<Integer> ids = Sets.newHashSet();

            // 4. display results
            if (hits.length == 0) return hitList;

            float bestScore = hits[0].score;
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                float score = hits[i].score;
                //          if (score < bestScore) return hitList;
                Document d = searcher.doc(docId);

                //   System.out.println(score + " d = " + d);

                if (ids.contains(docId))
                    continue;
                ids.add(docId);

                hitList.add(
                        new PatternData(
                                d.get("pattern"),
                                d.get("pair").split(" \\+ ")[0],
                                d.get("pair").split(" \\+ ")[1],
                                d.get("pair"),
                                d.get("idPair"),
                                d.get("sentence"),
                                d.get("label"),
                                Lists.newArrayList(d.get("subjectTypes").split(" ")),
                                Lists.newArrayList(d.get("objectTypes").split(" "))));


                if (ids.size() > maxHitsNumber)
                    break;
            }

            // searcher can only be closed when there
            // is no need to access the documents any more.
            //  searcher.close();
            //  reader.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hitList;
    }

    public List<String> searchPatterns(String pattern, int limit) {
        QueryParser qp = new QueryParser(LUCENE_VERSION, "pat", new StandardAnalyzer(LUCENE_VERSION, CharArraySet.EMPTY_SET));
        Query q = null;
        try {
            //q = qp.parse(pattern);
            System.out.println(pattern);
            q = qp.parse(pattern.replaceAll("\\[X\\]", "").replaceAll("\\[Y\\]", ""));
            List<String> results = new ArrayList<String>();
            TopScoreDocCollector collector = TopScoreDocCollector.create(limit, true);
            Similarity currentSimilarity = searcher.getSimilarity();
            searcher.setSimilarity(new DefaultSimilarity() {
                /**
                 * Do not weight the frequency of the search query inside
                 * the pattern
                 */
                public float tf(float frequency) {
                    return 1;
                }
            });
            searcher.search(q, collector);
            searcher.setSimilarity(currentSimilarity);
            for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                for (String pat : doc.get("pattern").split(" ")) {
                    if (!results.contains(pat.replaceAll("_", " "))) {
                        boolean contains = true;
                        for (String search_partial : pattern.split(" ")) {
                            if (!pat.contains(search_partial.replaceAll("_", " "))) {
                                contains = false;
                            }
                        }
                        if (contains)
                            results.add(pat.replaceAll("_", " "));
                    }
                }
                if (results.size() >= limit)
                    break;
            }
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public List<String> searchSubjects(String subject, int limit) {
        QueryParser qp = new QueryParser(LUCENE_VERSION, "subjectType", new StandardAnalyzer(LUCENE_VERSION, CharArraySet.EMPTY_SET));
        qp.setAllowLeadingWildcard(true);
        Query q = null;
        try {
            //q = qp.parse(pattern);
            q = qp.parse(subject);
            System.out.println(q);
            List<String> results = new ArrayList<String>();
            TopScoreDocCollector collector = TopScoreDocCollector.create(limit, true);
            searcher.search(q, collector);
            for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                for (IndexableField iF : doc.getFields("subjectType")) {
                    if (iF.stringValue().contains(subject) && !results.contains(iF.stringValue().replaceAll(" ", ".")))
                        results.add(iF.stringValue().replaceAll(" ", "."));
                }
                if (results.size() >= limit)
                    break;
            }
            System.out.println(results);
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public List<String> searchObjects(String object, int limit) {
        QueryParser qp = new QueryParser(LUCENE_VERSION, "objectType", new StandardAnalyzer(LUCENE_VERSION, CharArraySet.EMPTY_SET));
        qp.setAllowLeadingWildcard(true);
        Query q = null;
        try {
            //q = qp.parse(pattern);
            q = qp.parse(object);
            System.out.println(q);
            List<String> results = new ArrayList<String>();
            TopScoreDocCollector collector = TopScoreDocCollector.create(limit, true);
            searcher.search(q, collector);
            for (ScoreDoc scoreDoc : collector.topDocs().scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                for (IndexableField iF : doc.getFields("objectType")) {
                    if (iF.stringValue().contains(object) && !results.contains(iF.stringValue().replaceAll(" ", ".")))
                        results.add(iF.stringValue().replaceAll(" ", "."));
                }
                if (results.size() >= limit)
                    break;
            }
            System.out.println(results);
            return results;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }



    public List<PatternData> queryIndex(String queryText, String subjectText, String objectText, Collection<String> subjectTypes, Collection<String> objectTypes, int maxHitsNumber) {

        List<PatternData> hitList = Lists.newArrayList();

        queryText = "pattern:\"" + queryText.replaceAll(" ", "_") + "\" ";

        //================
        //Freebase ids set
        //================
        System.out.println(subjectText + " " + objectText);
        if (!StringUtils.isEmpty(subjectText) && !subjectText.equals("__extractor__")) {
            queryText += "AND subjID:\"" + subjectText + "\" ";
        }
        if (!StringUtils.isEmpty(objectText) && !objectText.equals("__extractor__")) {
            queryText += "AND objID:\"" + objectText + "\" ";
        }

        //=============
        //Extractor set
        //=============
        if (subjectText.equals("__extractor__") && subjectTypes.size() > 0) {
            queryText += "AND (";
            for (String subjectType : subjectTypes) {
                queryText += "subjID:\"" + subjectType + "\" OR ";
            }
            queryText = queryText.substring(0, queryText.length() - 4) + ") ";
        }

        if (objectText.equals("__extractor__") && objectTypes.size() > 0) {
            queryText += "AND (";
            for (String objectType : objectTypes) {
                queryText += "objID:\"" + objectType + "\" OR ";
            }
            queryText = queryText.substring(0, queryText.length() - 4) + ")";
        }

        //==================
        //Freebase types set
        //==================
        if (StringUtils.isEmpty(subjectText) && subjectTypes.size() > 0) {
            queryText += "AND (";
            for (String subjectType : subjectTypes) {
                queryText += "subjectTypes:\"" + subjectType + "\" OR ";
            }
            queryText = queryText.substring(0, queryText.length() - 4) + ") ";
        }

        if (StringUtils.isEmpty(objectText) && objectTypes.size() > 0) {
            queryText += "AND (";
            for (String objectType : objectTypes) {
                queryText += "objectTypes:\"" + objectType + "\" OR ";
            }
            queryText = queryText.substring(0, queryText.length() - 4) + ")";
        }




        //      queryText = QueryParser.escape(queryText);
        System.out.println("queryParser = " + queryText);

        QueryParser queryParser = new QueryParser(LUCENE_VERSION, "object", analyzer);

        Query query = null;
        try {
            query = queryParser.parse(queryText);
            //     System.out.println("Searching for: " + query.toString("pattern"));

            IndexReader reader = DirectoryReader.open(index);
            IndexSearcher searcher = new IndexSearcher(reader);

            TopDocs results = searcher.search(query, maxHitsNumber);
            ScoreDoc[] hits = results.scoreDocs;

            Set<Integer> ids = Sets.newHashSet();

            // 4. display results
            if (hits.length == 0) return hitList;

            float bestScore = hits[0].score;
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                float score = hits[i].score;
                //          if (score < bestScore) return hitList;
                Document d = searcher.doc(docId);

                //   System.out.println(score + " d = " + d);

                if (ids.contains(docId))
                    continue;
                ids.add(docId);

                //System.out.println(d.get("subjID") + " " + d.get("objID"));

                System.out.println("d = " + d);

                hitList.add(
                        new PatternData(
                                d.get("pattern"),
                                d.get("pair").split(" \\+ ")[0],
                                d.get("pair").split(" \\+ ")[1],
                                d.get("pair"),
                                d.get("idPair"),
                                d.get("sentence"),
                                d.get("label"),
                                Lists.newArrayList(d.get("subjectTypes").split(" ")),
                                Lists.newArrayList(d.get("objectTypes").split(" "))));


                if (ids.size() > maxHitsNumber)
                    break;
            }

            // searcher can only be closed when there
            // is no need to access the documents any more.
            //  searcher.close();
            //  reader.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hitList;
    }

    private void addDoc(IndexWriter w, String pair, String idPair, String pattern, String subject, String subjectTypes,
                        String object, String objectTypes, String sentence, String label) throws IOException {
        Document doc = new Document();

        Field pairField = new TextField("pair", pair, Field.Store.YES);

        Field idPairField = new TextField("idPair", idPair, Field.Store.YES);

        Field patternField = new TextField("pattern", pattern, Field.Store.YES);

        Field subjectField = new TextField("subject", subject, Field.Store.NO);
        Field subjectTypesField = new TextField("subjectTypes", subjectTypes, Field.Store.YES);

        Field objectField = new TextField("object", object, Field.Store.NO);
        Field objectTypesField = new TextField("objectTypes", objectTypes, Field.Store.YES);

        Field sentenceField = new StringField("sentence", sentence, Field.Store.YES);

        Field labelField = new StringField("label", label, Field.Store.YES);

        //	pathField.setBoost(2.0f);

        doc.add(patternField);
        for (String pat : pattern.split(" ")) {
            doc.add(new TextField("pat", pat.replaceAll("_", " ").replaceAll("\\.n", "").replaceAll("\\.v", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("#", ""), Field.Store.YES));
        }
        doc.add(subjectField);
        doc.add(objectField);
        doc.add(sentenceField);

        doc.add(subjectTypesField);
        for (String subType : subjectTypes.split(" ")) {
            doc.add(new TextField("subjectType", subType.replaceAll("[\\._]", " "), Field.Store.YES));
        }
        doc.add(objectTypesField);
        for (String objType : objectTypes.split(" ")) {
            doc.add(new TextField("objectType", objType.replaceAll("[\\._]", " "), Field.Store.YES));
        }
        doc.add(labelField);
        doc.add(pairField);
        doc.add(idPairField);

        // Add freebase id's seperately to make them searchable
        doc.add(new TextField("subjID", idPair.split(" \\+ ")[0].trim().replace("/m/", ""), Field.Store.YES));
        doc.add(new TextField("objID", idPair.split(" \\+ ")[1].trim().replace("/m/", ""), Field.Store.YES));

        w.addDocument(doc);
    }

    public static void optimize(String indexPath) {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47,
                new WhitespaceAnalyzer(Version.LUCENE_47));

        // Add new documents to an existing index:
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        try {
            IndexWriter w = new IndexWriter(new NIOFSDirectory(new File(indexPath)), config);
            w.forceMerge(1);
            w.close(true);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void indexFolder(String folder, boolean createNew) throws CorruptIndexException,
            LockObtainFailedException, IOException {

        IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION,
                analyzer);

        if (createNew) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        IndexWriter w = new IndexWriter(index, config);


        File[] files = new File(folder).listFiles();

        int count = 1;
        double sysTime = System.currentTimeMillis();
        for (File file : files) {
            System.out.println("indexing file " + count + " :\t" + file.getName());
            index(file.getAbsolutePath(), w);
            System.out.println("  Took " + ((System.currentTimeMillis() - sysTime) / 1000 / 60) + " minutes.");
            sysTime = System.currentTimeMillis();
            count++;
        }
        w.close();
    }

    public void indexFiles(String file, boolean createNew) throws CorruptIndexException,
            LockObtainFailedException, IOException {

        IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION,
                analyzer);

        if (createNew) {
            // Create a new index in the directory, removing any
            // previously indexed documents:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            // Add new documents to an existing index:
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        IndexWriter w = new IndexWriter(index, config);


        index(file, w);


        w.close();
    }

    private void index(String file, IndexWriter w) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String fileLine = null;
        while ((fileLine = in.readLine()) != null) {
            String[] line = fileLine.split("\t");

            System.out.println("fileLine = " + fileLine);
        //    System.out.println("fileLine = " + line.length);

            if (line.length == 8) {

                String pair = line[0] + " + " + line[1];
                String pairInverted = line[1] + " + " + line[0];

                String subject = line[0];
                String object = line[1];

                String pattern = line[2];
          //      System.out.println("pattern = " + pattern);
            //   pattern = pattern.substring(2, pattern.length() - 2).replaceAll(" ", "_");
             //   String[] split = pattern.split("\\),\\(");
              //  pattern = Joiner.on(" ").join(split);

                pattern = pattern.substring(1, pattern.length() - 1).replaceAll(" ", "_");
                String[] split = pattern.split(",");
                pattern = Joiner.on(" ").join(split);
           //     System.out.println("pattern = " + pattern);

                String invertedPattern = pattern.replaceAll("\\[X\\]", "[Z]");
                invertedPattern = invertedPattern.replaceAll("\\[Y\\]", "[X]");
                invertedPattern = invertedPattern.replaceAll("\\[Z\\]", "[Y]");
             //   System.out.println("invertedPattern = " + invertedPattern);

                //    String label = line[2];

                String sentence = line[3];

                String id1 = line[4];
                String id2 = line[5];
                String idPair = id1 + " + " + id2;
                String idPairInverted = id2 + " + " + id1;

                String subjectTypes[] = line[6].replaceAll("[\\[\\]{}\\(\\)]", "").split(",");
                Set<String> subjectT = Sets.newHashSet();
                Collections.addAll(subjectT, subjectTypes);
                String objectTypes[] = line[7].replaceAll("[\\[\\]{}\\(\\)]", "").split(",");
                Set<String> objectT = Sets.newHashSet();
                Collections.addAll(objectT, objectTypes);

                addDoc(w, pair, idPair, pattern, subject, Joiner.on(" ").join(subjectT), object, Joiner.on(" ").join(objectT), sentence, "");
                addDoc(w, pairInverted, idPairInverted, invertedPattern, object, Joiner.on(" ").join(objectT), subject, Joiner.on(" ").join(subjectT), sentence, "");
            }
        }
    }

    public class ExtractedType {

        private int count;
        private String name;

        public ExtractedType(int count, String name) {
            this.count = count;
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public String getName() {
            return name;
        }

    }

}
