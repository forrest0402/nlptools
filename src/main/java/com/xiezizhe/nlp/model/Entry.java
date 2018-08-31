package com.xiezizhe.nlp.model;

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
}
