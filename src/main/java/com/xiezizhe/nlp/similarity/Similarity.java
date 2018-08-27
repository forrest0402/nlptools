package com.xiezizhe.nlp.similarity;

import java.util.Comparator;
import java.util.List;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public interface Similarity<T> {

    double dist(List<T> T1, List<T> T2);

}
