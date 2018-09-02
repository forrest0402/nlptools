package com.xiezizhe.nlp.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:29
 */
public class Entry {

    final double[] data;

    /**
     * distant to the representative
     */
    private double dist;

    /**
     * distant to the query
     */
    private double score;

    public Entry(double[] data) {
        this.data = data;
    }

    public double[] get() {
        return this.data;
    }

    public int length() {
        return this.data.length;
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
        return "(" + Arrays.stream(data).mapToObj(String::valueOf).collect(Collectors.joining(" ")) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entry entry = (Entry) o;
        return Arrays.equals(data, entry.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
