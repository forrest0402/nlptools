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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 把几个faq excel合并起来，合并的时候把相似的问题合并在一起
 * <p>
 * Created by xiezizhe
 * Date: 2018/11/7 3:02 PM
 */
@Service
public class FaqCombinator2 {
    private static final Logger logger = LoggerFactory.getLogger(FaqCombinator2.class);

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
            Map<String, Set<String>> curFaq = excelUtils.trimFAQFile(excelUtils.getData(PREFIX + faqExcelFile, 1));
            faqs.putAll(curFaq);
            logger.info("{} has {} questions", faqExcelFile, curFaq.size());
        }
        faqs.remove("");
        logger.info("original faq size: {}", faqs.size());
        // simple combine
        Map<String, String> queryToStd = new HashMap<>();
        while (!isValidFaq(faqs)) {
            for (String curStandardQuestion : new ArrayList<>(faqs.keySet())) {
                if (!faqs.containsKey(curStandardQuestion)) continue;
                faqs.put(curStandardQuestion, faqs.get(curStandardQuestion).stream().filter(c -> c.length() > 2)
                        .collect(Collectors.toSet()));
                for (String query : faqs.get(curStandardQuestion)) {
                    String previousStandardQuestion = queryToStd.get(query);
                    if (previousStandardQuestion != null && !previousStandardQuestion.equals(curStandardQuestion)) {
                        faqs.get(previousStandardQuestion).addAll(faqs.get(curStandardQuestion));
                        for (String temp : faqs.get(curStandardQuestion)) {
                            queryToStd.put(temp, previousStandardQuestion);
                        }
                        faqs.remove(curStandardQuestion);
                        logger.info("combine {} and {}", previousStandardQuestion, curStandardQuestion);
                        break;
                    } else {
                        queryToStd.put(query, curStandardQuestion);
                    }
                }
            }
        }
        if (!isValidFaq(faqs)) {
            throw new IllegalStateException("faq is invalid");
        }
        logger.info("After simple combination, faq size: {}", faqs.size());
        logger.info("start to remove similar sentences");
        logger.info("1. get sentence embedding");
        Set<String> allSentences = new HashSet<>();
        faqs.forEach((k, v) -> {
            allSentences.add(k);
            allSentences.addAll(v);
        });

        List<List<String>> corpus = allSentences.stream().distinct()
                .map(s -> ToAnalysis.parse(s).getTerms().stream().map(Term::getName).collect(Collectors.toList()))
                .collect(Collectors.toList());

        logger.info("corpus size: {}", corpus.size());
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
        Set<String> removedFaqs = new HashSet<>();
        List<String> keys = new ArrayList<>(faqs.keySet());
        ExecutorService threadPool = new ThreadPoolExecutor(10, 10, 60000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ReentrantLock lock = new ReentrantLock();
        AtomicInteger process = new AtomicInteger();
        try (ProgressBar pb = new ProgressBar("Filtering faqs", faqs.size())) {
            for (String key : keys) {
                threadPool.execute(() -> {
                    Set<String> simQuestions = new HashSet<>(faqs.get(key));
                    int toIndex = Math.min(simQuestions.size(), 50);
//                        int toIndex = simQuestions.size();
                    Map<String, Set<String>> candidates = batchQuery(key,
                            new ArrayList<>(simQuestions).subList(0, toIndex),
                            index, 0.93, queryToStd);
                    try {
                        lock.lock();
                        if (!removedFaqs.contains(key)) {
                            for (Map.Entry<String, Set<String>> e : candidates.entrySet()) {
                                if (finalFaq.containsKey(e.getKey())|| removedFaqs.contains(e.getKey())) continue;
                                int threshold = faqs.containsKey(e.getKey()) ?
                                        (int) (faqs.get(e.getKey()).size() * 0.5) : Integer.MAX_VALUE;
                                // combine faqs.get(e.getKey()) and simQuestions
                                if (e.getValue().size() > threshold) {
                                    removedFaqs.add(e.getKey());
                                    simQuestions.addAll(faqs.get(e.getKey()));
                                    System.out.println("");
                                    logger.info("combine {} and {}, {} has {} similar questions", key, e.getKey(),
                                            key, simQuestions.size());
                                }
                            }
                            finalFaq.put(key, simQuestions);
                            if (simQuestions.size() > 1000) {
                                System.out.println("");
                                logger.error("{} has {} similar questions", key, simQuestions.size());
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                    pb.step();
                    progressPercentage(process.getAndIncrement(), keys.size());
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
        if (!isValidFaq(finalFaq)) {
            throw new IllegalStateException("faq is invalid");
        }
    }

    private static void progressPercentage(int remain, int total) {
        if (remain > total) {
            throw new IllegalArgumentException();
        }
        int maxBareSize = 10; // 10unit for 100%
        int remainProcent = ((100 * remain) / total) / maxBareSize;
        char defaultChar = '-';
        String icon = "*";
        String bare = new String(new char[maxBareSize]).replace('\0', defaultChar) + "]";
        StringBuilder bareDone = new StringBuilder();
        bareDone.append("[");
        for (int i = 0; i < remainProcent; i++) {
            bareDone.append(icon);
        }
        String bareRemain = bare.substring(remainProcent, bare.length());
        String percenStr = String.format("%.2f", (100.0 * remain) / total);
        System.out.print("\r" + bareDone + bareRemain + " " + percenStr + "%");
        if (remain == total) {
            System.out.print("\n");
        }
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
    private Map<String, Set<String>> batchQuery(String standardQuestion, List<String> query, Index<String> index,
                                                double threshold, Map<String, String> queryToFaq) {
        Set<String> results = new HashSet<>();
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
        Map<String, Set<String>> resMap = new HashMap<>();
        for (String result : results) {
            if (standardQuestion.equals(queryToFaq.get(result))) {
                continue;
            }
            if (!resMap.containsKey(queryToFaq.get(result))) {
                resMap.put(queryToFaq.get(result), new HashSet<>());
            }
            resMap.get(queryToFaq.get(result)).add(result);
        }
        resMap.remove(standardQuestion);
        resMap.remove(null);
        return resMap;
    }


    private boolean isValidFaq(Map<String, Set<String>> faqs) {
        Map<String, String> queryToStd = new HashMap<>();
        for (String curStandardQuestion : new ArrayList<>(faqs.keySet())) {
            if (!faqs.containsKey(curStandardQuestion)) continue;
            faqs.put(curStandardQuestion, faqs.get(curStandardQuestion).stream().filter(c -> c.length() > 2)
                    .collect(Collectors.toSet()));
            for (String query : faqs.get(curStandardQuestion)) {
                String previousStandardQuestion = queryToStd.get(query);
                if (previousStandardQuestion != null && !previousStandardQuestion.equals(curStandardQuestion)) {
                    return false;
                } else {
                    queryToStd.put(query, curStandardQuestion);
                }
            }
        }
        return true;
    }
}
