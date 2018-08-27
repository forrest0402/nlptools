package com.xiezizhe.nlp.similarity;

/**
 * @author forrest0402
 * @Description
 * @date 2018/6/27
 */
public interface DistanceFunction<T, U> {
    double apply(T t, U u);
}