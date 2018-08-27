package com.xiezizhe.nlp.similarity.timeseries;

import com.xiezizhe.nlp.similarity.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * EDR
 *
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public class EditDistanceonRealSequence<T> implements Similarity<T> {

    private static Logger logger = LoggerFactory.getLogger(EditDistanceonRealSequence.class);

    private final Comparator<T> comparator;

    /**
     * @param comparator determine whether two elements are equal
     */
    public EditDistanceonRealSequence(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public double dist(List<T> T1, List<T> T2) {
        if (T1 == null || T1.size() == 0) {
            if (T2 != null) return T2.size();
            else return 0;
        }

        if (T2 == null || T2.size() == 0) {
            if (T1 != null) return T1.size();
            else return 0;
        }

        int[][] dpInts = new int[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = i;
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = j;
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                int subCost = 1;
                if (comparator.compare(T1.get(i - 1), T2.get(j - 1)) == 0)
                    subCost = 0;
                dpInts[i][j] = min3(dpInts[i - 1][j - 1] + subCost, dpInts[i - 1][j] + 1, dpInts[i][j - 1] + 1);
            }
        }

        return dpInts[T1.size()][T2.size()];
    }

    private int min3(int a, int b, int c) {
        if (a > b) a = b;
        if (a > c) a = c;
        return a;
    }

}
