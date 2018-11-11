package com.xiezizhe.nlp.embedding;

import com.xiezizhe.nlp.model.WordVector;
import com.xiezizhe.nlp.utils.NlpUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xiezizhe
 * Date: 2018/11/2 11:06 AM
 */
@Service
public class PowerMeanEmb {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerMeanEmb.class);
    private static final String WORD_WEIGHTS_PATH = "word.weights.sif.txt";

    private boolean loadComplete;
    private Map<String, Double> wordWeights = new HashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("256")
    private WordVector wordVectors1;

    @Autowired
    @Qualifier("tencent")
    private WordVector wordVectors2;

    @Autowired
    @Qualifier("128")
    private WordVector wordVectors3;


    public PowerMeanEmb() {
        LOGGER.info("Enter SentenceEmbedding");
        loadComplete = loadExistingWordWeightsFile();
        LOGGER.info("Exit SentenceEmbedding");
    }


    /**
     * load existing WORD_WEIGHTs file and PC file from resources file
     *
     * @return
     */
    private boolean loadExistingWordWeightsFile() {
        InputStream wordWeightStream = this.getClass().getClassLoader().getResourceAsStream(WORD_WEIGHTS_PATH);
        if (wordWeightStream == null) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(wordWeightStream))) {
            String line = null;
            this.wordWeights = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                String[] array = line.split(File.separator);
                if (array.length == 2) {
                    this.wordWeights.putIfAbsent(array[0], Double.parseDouble(array[1]));
                } else {
                    if (array[0].length() > 0) {
                        this.wordWeights.putIfAbsent(File.separator, Double.parseDouble(array[0]));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("error occurs when reading file", e);
            return false;
        }

        return true;
    }

    /**
     * sentenceEmb.getRow(i) represents vector of the i-th sentence
     * (256+128+200)*3
     *
     * @param sentences
     * @return
     */
    public RealMatrix getEnhancedEmbedding(List<List<String>> sentences) {
        if (!loadComplete) {
            throw new IllegalStateException("Sentence Embedding isn't initialized well");
        }

        if (sentences == null) {
            return null;
        }

        int dimension = wordVectors1.getDimension() + wordVectors2.getDimension() + wordVectors3.getDimension();
        RealMatrix sentenceEmb = new Array2DRowRealMatrix(sentences.size(), dimension * 3);

        for (int i = 0; i < sentences.size(); i++) {
            List<String> sentence = sentences.get(i);
            double[] avgEmb = new double[dimension];
            double[] maxEmb = new double[dimension];
            double[] minEmb = new double[dimension];
            for (int j = 0; j < minEmb.length; j++) {
                maxEmb[j] = -Double.MAX_VALUE;
                minEmb[j] = Double.MAX_VALUE;
            }
            int count = 0;
            for (String term : sentence) {
                double[] vector1 = wordVectors1.get(term);
                double[] vector2 = wordVectors2.get(term);
                double[] vector3 = wordVectors3.get(term);
                if (vector1 == null && vector2 == null && vector3 == null) {
                    continue;
                }

                if (vector1 == null) vector1 = new double[wordVectors1.getDimension()];
                if (vector2 == null) vector2 = new double[wordVectors2.getDimension()];
                if (vector3 == null) vector3 = new double[wordVectors3.getDimension()];

                double[] termVector = NlpUtils.concat(vector1, vector2, vector3);
                if (termVector != null) {
                    double weight = wordWeights.getOrDefault(term, 1.0);
                    NlpUtils.selfMul(termVector, weight);
                    NlpUtils.selfAdd(avgEmb, termVector);
                    for (int j = 0; j < minEmb.length; j++) {
                        if (termVector[j] > maxEmb[j]) {
                            maxEmb[j] = termVector[j];
                        }
                        if (termVector[j] < minEmb[j]) {
                            minEmb[j] = termVector[j];
                        }
                    }
                    ++count;
                }
            }

            if (count == 0) {
                sentenceEmb.setRowMatrix(i, MatrixUtils.createRowRealMatrix(new double[dimension * 3]));
            } else {
                NlpUtils.selfMul(avgEmb, 1.0 / count);
                double[] result = NlpUtils.concat(maxEmb, avgEmb, minEmb);
                sentenceEmb.setRowMatrix(i, MatrixUtils.createRowRealMatrix(result));
            }
        }
        return sentenceEmb;
    }

    @Deprecated
    public void close() {
        this.wordVectors1.clear();
        this.wordVectors2.clear();
        this.wordVectors3.clear();
    }
}