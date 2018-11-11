package com.xiezizhe.nlp.index;

import com.xiezizhe.nlp.index.impl.iDistance;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author xiezizhe
 * @Description
 * @date 2018/9/2
 */
public class testIDistance {

    private static Logger logger = LoggerFactory.getLogger(testIDistance.class);

    @Test
    public void test() {
        Index<String> index = new iDistance<>();

        List<Entry<String>> entries = Arrays.asList(new Entry<>(new double[]{1, 1}, "1"),
                new Entry<>(new double[]{1, 2}, "2"),
                new Entry<>(new double[]{1, 3}, "3"),
                new Entry<>(new double[]{4, 5}, "4"),
                new Entry<>(new double[]{10, 20}, "5"),
                new Entry<>(new double[]{10, 21}, "6"),
                new Entry<>(new double[]{10, 30}, "7"),
                new Entry<>(new double[]{9, 20}, "8"),
                new Entry<>(new double[]{7, 20}, "9"),
                new Entry<>(new double[]{4, 8}, "10"));
        int k = 2;
        index.build(entries, k);

        Entry<String> query = new Entry<>(new double[]{5, 5}, null);
        List<Entry<String>> ans = index.top(query, 3);
        for (Entry an : ans) {
            System.out.print(an.toString());
            System.out.println(" -> " + EuclideanDistance.dist(an.getRepr(), query.getRepr()));
        }
    }

    @Test
    public void efficiencyTest() {

    }

}
