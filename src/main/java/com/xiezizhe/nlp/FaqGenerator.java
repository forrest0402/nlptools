package com.xiezizhe.nlp;import com.xiezizhe.nlp.embedding.PowerMeanEmb;import com.xiezizhe.nlp.index.Index;import com.xiezizhe.nlp.index.impl.BruteForce;import com.xiezizhe.nlp.model.Entry;import com.xiezizhe.nlp.utils.ExcelUtils;import org.ansj.domain.Term;import org.ansj.splitWord.analysis.ToAnalysis;import org.apache.commons.math3.linear.RealMatrix;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.core.env.Environment;import org.springframework.stereotype.Service;import java.io.FileInputStream;import java.io.FileOutputStream;import java.io.IOException;import java.io.ObjectInputStream;import java.io.ObjectOutputStream;import java.nio.file.Files;import java.nio.file.Paths;import java.util.ArrayList;import java.util.List;import java.util.Map;import java.util.Set;import java.util.stream.Collectors;/** * 从语料库中搜索出top-k个句子来补充faq数据集 * <p> * Created by xiezizhe * Date: 2018/11/2 4:56 PM */@Servicepublic class FaqGenerator {    private static final Logger logger = LoggerFactory.getLogger(FaqGenerator.class);    @Autowired    PowerMeanEmb powerMeanEmb;    @Autowired    ExcelUtils excelUtils;    @Autowired    Environment environment;    public void run() throws IOException, ClassNotFoundException {        logger.info("Enter");        String corpusPath = environment.getProperty("corpus.path", "dict/corpus.txt");        String inputPath = environment.getProperty("input.path", "data/nb_Test_qas.xlsx");        String corpusSerFilePath = environment.getProperty("token.path", "dict/corpus.ser");        String indexSerPath = environment.getProperty("index.path", "dict/index.ser");        List<List<String>> corpus = null;        if (Files.exists(Paths.get(corpusSerFilePath))) {            logger.info("loading {}", corpusSerFilePath);            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(corpusSerFilePath))) {                corpus = (List<List<String>>) ois.readObject();            } catch (ClassNotFoundException e) {                logger.error("", e);            }        }        if (corpus == null) {            logger.info("building corpus Serialization file from {}", corpusPath);            corpus = Files.lines(Paths.get(corpusPath))                    .filter(s -> s.length() > 3).distinct()                    .map(s -> ToAnalysis.parse(s).getTerms().stream().map(Term::getName).collect(Collectors.toList()))                    .filter(s -> s.size() > 3).collect(Collectors.toList());            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(corpusSerFilePath))) {                oos.writeObject(corpus);            }            logger.info("save corpus Serialization file to {}", corpusSerFilePath);        }        logger.info("Loading corpus complete");        logger.info("corpus size: {}", corpus.size());        Map<String, Set<String>> faqs = excelUtils.getFaq(inputPath);        logger.info("start to get sentence embedding");        RealMatrix emb = powerMeanEmb.getEnhancedEmbedding(corpus);//        powerMeanEmb.close();        logger.info("start to load index");//        Index<String> index = Index.load(indexSerPath);//        if (index == null) {//            logger.info("no index exists");//            index = new iDistance<>();//            List<Entry<String>> entries = new ArrayList<>();//            for (int i = 0; i < emb.getRowDimension(); ++i) {//                entries.add(new Entry<>(emb.getRow(i),//                        corpus.get(i).stream().collect(Collectors.joining(""))));//            }//            index.build(entries, 150);//            logger.info("save index to {}", indexSerPath);//            Index.save(index, indexSerPath);//        }//        logger.info("load {} complete", indexSerPath);//        corpus.clear();        Index<String> index = new BruteForce<>();        List<Entry<String>> entries = new ArrayList<>();        for (int i = 0; i < emb.getRowDimension(); ++i) {            entries.add(new Entry<>(emb.getRow(i),                    corpus.get(i).stream().collect(Collectors.joining(""))));        }        index.build(entries, 150);//        List<String> query = Arrays.asList("白金卡年费多少", "白金卡有年费吗", "白金卡有没有年费啊");//        batchQuery(query, index);        for (Map.Entry<String, Set<String>> entry : faqs.entrySet()) {            logger.info("************ {} **************", entry.getKey());            batchQuery(new ArrayList<>(entry.getValue()), index, 5);        }    }    private void batchQuery(List<String> query, Index<String> index, int k) {        List<List<String>> queries = query.stream()                .map(s -> ToAnalysis.parse(s).getTerms().stream().map(Term::getName).collect(Collectors.toList()))                .collect(Collectors.toList());        RealMatrix simQuesEmb = powerMeanEmb.getEnhancedEmbedding(queries);        for (int i = 0; i < simQuesEmb.getRowDimension(); i++) {            logger.info("****************************");            logger.info("[{}]", query.get(i));            List<Entry<String>> results = index.top(new Entry<>(simQuesEmb.getRow(i), null), k);            for (Entry<String> result : results) {                logger.info(result.getData());            }            logger.info("***");        }    }}