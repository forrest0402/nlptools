package com.xiezizhe.nlp.similarity.set;

import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public class Frechet<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(Frechet.class);

    private final DistanceFunction distFunc;

    public Frechet(DistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    public double dist(List<T> t1, List<T> t2) {
        double[][] ca = new double[t2.size()][t1.size()];
        for (int i = 0; i < t2.size(); ++i) {
            for (int j = 0; j < t1.size(); ++j) {
                ca[i][j] = -1.0D;
            }
        }
        double result = c(t2.size() - 1, t1.size() - 1, ca, t1, t2);
        return result;
    }

    private double c(int i, int j, double[][] ca, List<T> t1, List<T> t2) {
        if (ca[i][j] > -1.0D) {
            return ca[i][j];
        } else {
            if (i == 0 && j == 0) {
                ca[i][j] = distFunc.apply(t2.get(i), t1.get(j));
            } else if (i > 0 && j == 0) {
                ca[i][j] = Math.max(c(i - 1, 0, ca, t1, t2), distFunc.apply(t2.get(i), t1.get(j)));
            } else if (i == 0 && j > 0) {
                ca[i][j] = Math.max(c(0, j - 1, ca, t1, t2), distFunc.apply(t2.get(i), t1.get(j)));
            } else if (i > 0 && j > 0) {
                ca[i][j] = Math.max(Math.min(Math.min(c(i - 1, j, ca, t1, t2), c(i - 1, j - 1, ca, t1, t2)), c(i, j - 1, ca, t1, t2)), distFunc.apply(t2.get(i), t1.get(j)));
            } else {
                ca[i][j] = 1.0D / 0.0;
            }
            return ca[i][j];
        }
    }

}
