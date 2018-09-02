package com.xiezizhe.nlp.index;

import com.xiezizhe.nlp.index.impl.iDistance;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xiezizhe
 * @Description
 * @date 2018/9/2
 */
public class testIDistance {

    private static Logger logger = LoggerFactory.getLogger(testIDistance.class);

    @Test
    public void test() {
        Index index = new iDistance();


        List<Entry> entries = Arrays.asList(new Entry(new double[]{1, 1}),
                new Entry(new double[]{1, 2}),
                new Entry(new double[]{1, 3}),
                new Entry(new double[]{4, 5}),
                new Entry(new double[]{10, 20}),
                new Entry(new double[]{10, 21}),
                new Entry(new double[]{10, 30}),
                new Entry(new double[]{9, 20}),
                new Entry(new double[]{7, 20}),
                new Entry(new double[]{4, 8}));
        int k = 2;
        index.build(entries, k);

        Entry query = new Entry(new double[]{5, 5});
        List<Entry> ans = index.top(query, 3);
        for (Entry an : ans) {
            System.out.print(Arrays.stream(an.get()).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            System.out.println(" -> " + EuclideanDistance.dist(an.get(), query.get()));
        }
    }
}
