package com.xiezizhe.nlp.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:29
 */
public class Entry<T> implements Serializable, IEntry<T> {

    private static final long serialVersionUID = 733521108316381015L;

    final double[] representation;

    final T data;

    /**
     * distant to the representative
     */
    private double dist;

    /**
     * distant to the query
     */
    private double score;

    public Entry(double[] representation, T data) {
        this.representation = representation;
        this.data = data;
    }

    public Entry(Entry<T> entry) {
        this.representation = entry.getRepr().clone();
        this.data = entry.getData();
    }

    public T getData() {
        return this.data;
    }

    public double[] getRepr() {
        return this.representation;
    }

    public int getDimension() {
        return this.representation.length;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return String.valueOf(data) + " " +
                Arrays.stream(representation).mapToObj(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Arrays.equals(representation, entry.representation);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(representation);
    }

}
