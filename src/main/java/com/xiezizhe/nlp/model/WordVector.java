package com.xiezizhe.nlp.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiezizhe
 * Date: 2018/11/2 11:17 AM
 */
@Component
@Scope("prototype")
public class WordVector {

    private static Logger logger = LoggerFactory.getLogger(WordVector.class);
    private Map<String, double[]> wordEmb;
    private int dimension = Integer.MIN_VALUE;
    private boolean isClear = false;
    private String fileName = null;

    public WordVector(String fileName) {
        this.fileName = fileName;
    }

    private WordVector() {
    }

    public void init(String fileName) {
        logger.info(" loading {}", fileName);
        long startTime = System.nanoTime();
        this.fileName = fileName;
        wordEmb = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line = reader.readLine();
            dimension = Integer.parseInt(line.split(" ")[1]);
            while ((line = reader.readLine()) != null) {
                String[] array = line.split(" ");
                double[] vec = new double[dimension];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = Double.parseDouble(array[i + 1]);
                }
                wordEmb.put(array[0], vec);
//                break;
            }
        } catch (IOException e) {
            logger.error("failed to load {}", fileName, e);
        }
        long endTime = System.nanoTime();
        logger.info(" loading {} costs {} ms", fileName, (endTime - startTime) / 1000000L);
    }

    public void init() {
        init(this.fileName);
    }

    public int getDimension() {
        return this.dimension;
    }

    public synchronized void clear() {
        this.wordEmb.clear();
        isClear = true;
    }

    public double[] get(String term) {
        if (isClear) {
            throw new IllegalStateException("this word vectors had been cleared");
        }
        return wordEmb.get(term);
    }

}
