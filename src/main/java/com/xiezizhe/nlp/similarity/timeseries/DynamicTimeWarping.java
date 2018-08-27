package com.xiezizhe.nlp.similarity.timeseries;

import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public class DynamicTimeWarping<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(DynamicTimeWarping.class);

    private final DistanceFunction distFunc;

    private int warpingWindowSize;

    /**
     * @param distFunc          calculate the distance between two elements
     * @param warpingWindowSize only the elements whose position difference is less than warpingWindowSize can be
     *                          matched
     */
    public DynamicTimeWarping(DistanceFunction distFunc, int warpingWindowSize) {
        this.distFunc = distFunc;
        this.warpingWindowSize = warpingWindowSize;
    }

    public double dist(List<T> T1, List<T> T2) {

        if (T1.size() == 0 && T2.size() == 0) return 0;
        if (T1.size() == 0 || T2.size() == 0) return Integer.MAX_VALUE;

        double[][] dpInts = new double[T1.size() + 1][T2.size() + 1];

        warpingWindowSize = Math.max(warpingWindowSize, Math.abs(T1.size() - T2.size()));

        for (int i = 0; i <= T1.size(); ++i)
            for (int j = 0; j <= T2.size(); ++j)
                dpInts[i][j] = Integer.MAX_VALUE;

        dpInts[0][0] = 0;

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = Math.max(1, i - warpingWindowSize); j <= Math.min(T2.size(), i + warpingWindowSize); ++j) {
                dpInts[i][j] = distFunc.apply(T1.get(i - 1), T2.get(j - 1)) + min3(dpInts[i - 1][j - 1], dpInts[i -
                        1][j], dpInts[i][j - 1]);
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
