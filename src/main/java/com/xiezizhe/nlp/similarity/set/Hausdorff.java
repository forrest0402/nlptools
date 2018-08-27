package com.xiezizhe.nlp.similarity.set;

import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public class Hausdorff<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(Hausdorff.class);

    private final DistanceFunction distFunc;

    public Hausdorff(DistanceFunction distFunc) {
        this.distFunc = distFunc;
    }

    public double dist(List<T> T1, List<T> T2) {
        double[][] dist_matrix;
        dist_matrix = new double[T2.size()][T1.size()];
        double result = 0.0D;
        ArrayList<Double> minDistances1 = new ArrayList();
        ArrayList<Double> minDistances2 = new ArrayList();

        int i;
        for (i = 0; i < dist_matrix.length; ++i) {
            for (int j = 0; j < dist_matrix[0].length; ++j) {
                dist_matrix[i][j] = distFunc.apply(T1.get(j), T2.get(i));
            }
        }

        int j;
        double min;
        for (i = 0; i < dist_matrix.length; ++i) {
            min = 1.0D / 0.0;
            for (j = 0; j < dist_matrix[0].length; ++j) {
                if (dist_matrix[i][j] <= min) {
                    min = dist_matrix[i][j];
                }
            }

            minDistances1.add(min);
        }

        for (i = 0; i < dist_matrix[0].length; ++i) {
            min = 1.0D / 0.0;

            for (j = 0; j < dist_matrix.length; ++j) {
                if (dist_matrix[j][i] <= min) {
                    min = dist_matrix[j][i];
                }
            }

            minDistances2.add(min);
        }

        Collections.sort(minDistances1);
        Collections.sort(minDistances2);
        double value1 = (Double) minDistances1.get(minDistances1.size() - 1);
        double value2 = (Double) minDistances2.get(minDistances2.size() - 1);
        result = Math.max(value1, value2);
        return result;
    }
}
