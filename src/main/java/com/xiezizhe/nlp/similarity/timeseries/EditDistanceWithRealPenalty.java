package com.xiezizhe.nlp.similarity.timeseries;

import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ERP
 *
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public class EditDistanceWithRealPenalty<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(EditDistanceWithRealPenalty.class);

    private final T g;

    private final DistanceFunction distFunc;

    /**
     * @param g        a constant element
     * @param distFunc calculate the distance between two elements
     */
    public EditDistanceWithRealPenalty(T g, DistanceFunction distFunc) {
        this.g = g;
        this.distFunc = distFunc;
    }

    /**
     * This distance must be used after normalization of two sequences
     *
     * @param T1
     * @param T2
     * @return
     */
    public double dist(List<T> T1, List<T> T2) {
        if (T1 == null || T1.size() == 0) {
            double res = 0.0;
            if (T2 != null) {
                for (T t : T2) {
                    res += distFunc.apply(t, g);
                }
            }
            return res;
        }

        if (T2 == null || T2.size() == 0) {
            double res = 0.0;
            if (T1 != null) {
                for (T t : T1) {
                    res += distFunc.apply(t, g);
                }
            }
            return res;
        }

        double[][] dpInts = new double[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = distFunc.apply(T1.get(i - 1), g) + dpInts[i - 1][0];
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = distFunc.apply(T2.get(j - 1), g) + dpInts[0][j - 1];
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                dpInts[i][j] = min3(dpInts[i - 1][j - 1] + distFunc.apply(T1.get(i - 1), T2.get(j - 1)),
                        dpInts[i - 1][j] + distFunc.apply(T1.get(i - 1), g),
                        dpInts[i][j - 1] + distFunc.apply(g, T2.get(j - 1)));
            }
        }

        return dpInts[T1.size()][T2.size()];
    }


    private double min3(double a, double b, double c) {
        if (a > b) a = b;
        if (a > c) a = c;
        return a;
    }
}
