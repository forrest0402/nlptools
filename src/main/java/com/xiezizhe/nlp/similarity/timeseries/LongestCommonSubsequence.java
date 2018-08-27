package com.xiezizhe.nlp.similarity.timeseries;

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
public class LongestCommonSubsequence<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(LongestCommonSubsequence.class);

    private final Comparator<T> comparator;

    private final int theta;

    /**
     * @param comparator determine whether two elements are equal
     * @param theta      only the elements whose position difference is less than theta can be matched
     */
    public LongestCommonSubsequence(Comparator<T> comparator, int theta) {
        this.comparator = comparator;
        this.theta = theta;
    }

    /**
     * @param T1
     * @param T2
     * @return
     */
    public double dist(List<T> T1, List<T> T2) {

        if (T1 == null || T2 == null || T1.size() == 0 || T2.size() == 0)
            return 0;

        int[][] dpInts = new int[T1.size()][T2.size()];

        if (comparator.compare(T1.get(0), T2.get(0)) == 0) dpInts[0][0] = 1;

        for (int i = 1; i < T1.size(); ++i) {
            if (comparator.compare(T1.get(i), T2.get(0)) == 0)
                dpInts[i][0] = 1;
            else dpInts[i][0] = dpInts[i - 1][0];
        }

        for (int i = 1; i < T2.size(); ++i) {
            if (comparator.compare(T2.get(i), T1.get(0)) == 0)
                dpInts[0][i] = 1;
            else dpInts[0][i] = dpInts[0][i - 1];
        }


        for (int i = 1; i < T1.size(); ++i) {
            for (int j = 1; j < T2.size(); ++j) {
                if (Math.abs(i - j) <= theta) {
                    if (comparator.compare(T1.get(i), T2.get(j)) == 0) {
                        dpInts[i][j] = 1 + dpInts[i - 1][j - 1];
                    } else {
                        dpInts[i][j] = Math.max(dpInts[i - 1][j], dpInts[i][j - 1]);
                    }
                }
            }
        }

        return dpInts[T1.size() - 1][T2.size() - 1];
    }

}
