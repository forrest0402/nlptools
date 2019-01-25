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
public class EuclideanDistance<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(EuclideanDistance.class);

    private final DistanceFunction distFunc;

    /**
     * @param distFunc calculate the distance between two elements
     */
    public EuclideanDistance(DistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    /**
     * @param T1
     * @param T2
     * @return
     */
    public double dist(List<T> T1, List<T> T2) {
        if (T1.size() != T2.size()) {
            throw new IllegalArgumentException("T1 should be of the same getDimension as T2");
        }

        double dist = 0.0;
        for (int i = 0; i < T1.size(); ++i) {
            dist += distFunc.apply(T1.get(i), T2.get(i));
        }
        return Math.sqrt(dist);
    }

    public static double dist(double[] T1, double[] T2) {
        if (T1 == null || T2 == null || T1.length != T2.length) {
            throw new IllegalArgumentException("T1 and T2 cannot be null values and T1 should be of the same getDimension " +
                    "as T2");
        }

        double dist = 0.0;
        for (int i = 0; i < T1.length; i++) {
            dist += (T1[i] - T2[i]) * (T1[i] - T2[i]);
        }
        return Math.sqrt(dist);
    }

}
