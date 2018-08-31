package com.xiezizhe.nlp.similarity.set;

import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * return a real number in the range of [0,1], |A∩B|/|A∪B|
 *
 * @author forrest0402
 * @Description
 * @date 2018/6/28
 */
public class Jaccard<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(Jaccard.class);

    public double dist(List<T> T1, List<T> T2) {

        if (T1 == null || T2 == null) {
            throw new IllegalArgumentException("inputs are null values");
        }

        if (T1.size() > T2.size()) {
            List<T> temp = T1;
            T1 = T2;
            T2 = temp;
        }

        double interN = 0, diffN = 0;
        Set<T> intersection = new HashSet<>(T2);
        for (T t1 : T1) {
            if (intersection.contains(t1)) {
                ++interN;
            } else {
                ++diffN;
            }
        }

        return interN / (T2.size() + diffN);
    }

}
