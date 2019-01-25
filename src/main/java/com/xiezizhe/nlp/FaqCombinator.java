package com.xiezizhe.nlp;

import com.xiezizhe.nlp.embedding.PowerMeanEmb;
import com.xiezizhe.nlp.index.Index;
import com.xiezizhe.nlp.index.impl.BruteForce;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.utils.ExcelUtils;
import me.tongfei.progressbar.ProgressBar;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 把几个faq excel合并起来，合并的时候把相似的问题合并在一起
 * <p>
 * Created by xiezizhe
 * Date: 2018/11/7 3:02 PM
 */
@Service
public class FaqCombinator {
    private static final Logger logger = LoggerFactory.getLogger(FaqCombinator.class);

    @Autowired
    PowerMeanEmb powerMeanEmb;

    @Autowired
    ExcelUtils excelUtils;

    @Autowired
    Environment environment;

    private final static String PREFIX = "data/";

    public void run() throws IOException {
        String[] faqExcelFiles = environment.getProperty("faq.sources").split(",");
        Map<String, Set<String>> faqs = new ConcurrentHashMap<>();
        for (String faqExcelFile : faqExcelFiles) {
            Map<String, Set<String>> curFaq = excelUtils.getFaq(PREFIX + faqExcelFile);
            faqs.putAll(curFaq);
        }

        // simple combine
        Map<String, String> queryToFaq = new HashMap<>();
        for (String key : faqs.keySet()) {
            faqs.put(key, faqs.get(key).stream().distinct().filter(c -> c.length() > 2).collect(Collectors.toSet()));
            List<String> duplicateQueries = new ArrayList<>();
            for (String query : faqs.get(key)) {
                if (queryToFaq.containsKey(key)) {
                    duplicateQueries.add(query);
                    logger.info("{} was in {}, now in {}", query, queryToFaq.get(key), key);
                } else queryToFaq.put(query, key);
            }
            faqs.get(key).removeAll(duplicateQueries);
        }

        logger.info("faq size: {}", faqs.size());
        logger.info("start to remove similar sentences");
        logger.info("1. get sentence embedding");
        List<String> allSentences = new ArrayList<>();
        faqs.forEach((k, v) -> {
            allSentences.add(k);
            allSentences.addAll(v);
        });
        List<List<String>> corpus = allSentences.stream().filter(s -> s.length() > 3).distinct()
                .map(s -> ToAnalysis.parse(s).getTerms().stream().map(Term::getName).collect(Collectors.toList()))
                .filter(s -> s.size() > 2).collect(Collectors.toList());

        RealMatrix emb = powerMeanEmb.getEnhancedEmbedding(corpus);
        logger.info("2. build index");
        Index<String> index = new BruteForce<>();
        List<Entry<String>> entries = new ArrayList<>();
        for (int i = 0; i < emb.getRowDimension(); ++i) {
            entries.add(new Entry<>(emb.getRow(i),
                    corpus.get(i).stream().collect(Collectors.joining(""))));
        }
        index.build(entries, -1);

        logger.info("3. filter faq");
        Map<String, Set<String>> finalFaq = new ConcurrentHashMap<>();
        faqs.remove("");
        List<String> keys = new ArrayList<>(faqs.keySet());
        ExecutorService threadPool = new ThreadPoolExecutor(5, 5, 60000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ReentrantLock lock = new ReentrantLock();
        try (ProgressBar pb = new ProgressBar("Filtering faqs", faqs.size())) {
            for (String key : keys) {
                threadPool.execute(() -> {
                    boolean flag = true;
                    try {
                        lock.lock();
                        flag = faqs.containsKey(key);
                    } finally {
                        lock.unlock();
                    }

                    if (!flag) {
                        Set<String> simQuestions = faqs.get(key);
//                      int toIndex = Math.min(simQuestions.size(), 20);
                        int toIndex = simQuestions.size();
                        Map<String, List<String>> candidates = batchQuery(key,
                                new ArrayList<>(simQuestions).subList(0, toIndex), index, 0.93, queryToFaq);
                        try {
                            lock.lock();
                            finalFaq.put(key, simQuestions);
                            for (Map.Entry<String, List<String>> e : candidates.entrySet()) {
                                if (finalFaq.containsKey(e.getKey()) || !faqs.containsKey(e.getKey())) continue;
                                int threshold = faqs.containsKey(e.getKey()) ? faqs.get(e.getKey()).size() / 2 : Integer
                                        .MAX_VALUE;
                                if (e.getValue().size() > threshold) {
                                    finalFaq.get(key).addAll(faqs.get(e.getKey()));
                                    faqs.remove(e.getKey());
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                        pb.step();
                    }
                });
            }
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("", e);
        }

        logger.info("final faq size: {}", finalFaq.size());
        String filePath = String.format("transfer_faq_%s_%d.xlsx", new SimpleDateFormat("yyyymmdd")
                .format(System.currentTimeMillis()), finalFaq.size());
        excelUtils.writeFaqToFile(finalFaq, filePath);
    }

    /**
     * key for standard question, value for occurrence
     *
     * @param standardQuestion
     * @param query
     * @param index
     * @param threshold
     * @param queryToFaq
     * @return
     */
    private Map<String, List<String>> batchQuery(String standardQuestion, List<String> query, Index<String> index,
                                                 double threshold, Map<String, String> queryToFaq) {
        List<String> results = new ArrayList<>();
        List<List<String>> queries = query.stream()
                .map(s -> ToAnalysis.parse(s).getTerms().stream().map(Term::getName).collect(Collectors.toList()))
                .collect(Collectors.toList());
        RealMatrix simQuesEmb = powerMeanEmb.getEnhancedEmbedding(queries);
        for (int i = 0; i < simQuesEmb.getRowDimension(); i++) {
            List<Entry<String>> result = index.top(new Entry<>(simQuesEmb.getRow(i), query.get(i)), threshold)
                    .stream().filter(e -> !standardQuestion.equals(queryToFaq.get(e.getData())))
                    .collect(Collectors.toList());

            results.addAll(result.stream().map(c -> c.getData()).collect(Collectors.toList()));
            if (result.size() > 1000) {
                System.out.println("impossible");
            }
        }
        Map<String, List<String>> resMap = new HashMap<>();
        for (String result : results) {
            if (standardQuestion.equals(queryToFaq.get(result))) {
                continue;
            }
            if (!resMap.containsKey(queryToFaq.get(result))) {
                resMap.put(queryToFaq.get(result), new ArrayList<>());
            }
            resMap.get(queryToFaq.get(result)).add(result);
        }
        resMap.remove(standardQuestion);
        resMap.remove(null);
        return resMap;
    }
}
