package com.xiezizhe.nlp.similarity.string;

import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * In information theory, the Hamming distance between two strings of equal length is the number of positions at
 * which the corresponding symbols are different.
 * In other words, it measures the minimum number of substitutions required to change one string into the other
 * <p>
 * https://en.wikipedia.org/wiki/Hamming_distance
 *
 * @author forrest0402
 * @Description
 * @date 2018/6/28
 */
public class Hamming<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(Hamming.class);

    public double dist(List<T> T1, List<T> T2) {
        if (T1.size() != T2.size())
            throw new IllegalArgumentException("");
        double sum = 0;
        for (int i = 0; i < T1.size(); ++i) {
            if (T1.get(i) != T2.get(i)) ++sum;
        }
        return sum;
    }
}
