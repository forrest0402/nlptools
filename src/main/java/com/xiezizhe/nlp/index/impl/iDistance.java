package com.xiezizhe.nlp.index.impl;

import com.xiezizhe.nlp.index.Index;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:30
 */
public class iDistance implements Index {

    private static final double DEFAULT_DELTA = 1.0;

    private static ThreadLocal<Boolean> stopFlags = new ThreadLocal<>();

    private Group group;

    private volatile boolean ready = false;

    DistanceFunction<Double, Double> func = (e1, e2) -> (e1 - e2) * (e1 - e2);
    private Similarity<Double> similarity = new EuclideanDistance<>(func);

    @Override
    public synchronized void build(List<Entry> entries) {
        ready = false;
        //step 1. partition data

        //step 2. build B+ tree

        ready = true;
    }

    @Override
    public List<Entry> top(Entry query, int k) {
        return top(query, k, DEFAULT_DELTA);
    }

    @Override
    public List<Entry> top(Entry query, int k, double deltaR) {
        if (!this.ready || query == null) return null;
        if (k < 0 || deltaR < 0) throw new IllegalArgumentException("arguments must be positive");
        if (k > this.group.length()) throw new IllegalArgumentException("k is too large");

        double r = 0;
        stopFlags.set(false);
        PriorityQueue<Entry> answers = new PriorityQueue<>(Comparator.comparing(Entry::getScore).reversed());
        boolean[] visit = new boolean[this.group.length()];
        int[] lp = new int[this.group.length()];
        int[] rp = new int[this.group.length()];

        while (!stopFlags.get()) {
            r += deltaR;
            searchO(query, r, answers, k, visit, lp, rp);
        }
        return answers.stream().sorted(Comparator.comparing(Entry::getScore)).collect(Collectors.toList());
    }

    private void searchO(Entry query, double radius, PriorityQueue<Entry> answers,
                         int k, boolean[] visit, int[] lp, int[] rp) {
        if (answers.size() == k && answers.peek().getDist() <= radius) {
            stopFlags.set(true);
        }

        for (int i = 0; i < this.group.length(); i++) {
            Entry representative = this.group.getRepresentative(i);
            double dist = dist(representative.get(), query.get());

            //group i has not been searched before
            if (!visit[i]) {
                // group i contains query
                if (dist + radius < this.group.getDisMax(i)) {
                    visit[i] = true;
                    lp[i] = searchInward(-1, dist, this.group.getGroup(i), representative, radius, k, answers);
                    rp[i] = searchOutward(-1, dist, this.group.getGroup(i), representative, radius, k, answers);
                }// group i intersets query
                else if (dist - radius < this.group.getDisMax(i)) {
                    visit[i] = true;
                    lp[i] = searchInward(-1, dist, this.group.getGroup(i), representative, radius, k, answers);
                }
            } else {
                //continue the search process
                lp[i] = searchInward(lp[i], dist, this.group.getGroup(i), representative, radius, k, answers);
                rp[i] = searchInward(rp[i], dist, this.group.getGroup(i), representative, radius, k, answers);
            }
        }
    }

    /**
     * currently use EuclideanDistance, the used distance must be a metric, otherwise the top-k search cannot
     * gaurantee the correctness
     *
     * @param array1
     * @param array2
     * @return
     */
    private double dist(double[] array1, double[] array2) {
        return EuclideanDistance.dist(array1, array2);
    }

    private int searchInward(int left, double dist, List<Entry> entries,
                             Entry query, double radius, int k, PriorityQueue<Entry>
                                     answers) {
        if (left == -1) {
            left = binarySearch(entries, dist);
        }
        // 0 position is representative
        if (left == 0) return left;

        double candidateDist = dist(entries.get(left).get(), query.get());
        entries.get(left).setScore(candidateDist);
        answers.add(entries.get(left));
        if (answers.size() > k) {
            answers.poll();
        }
        if (candidateDist < radius) {
            return searchInward(left - 1, dist, entries, query, radius, k, answers);
        }
        return left;
    }

    private int searchOutward(int right, double dist, List<Entry> entries, Entry query,
                              double radius, int k, PriorityQueue<Entry> answers) {
        if (right == -1) {
            right = binarySearch(entries, dist);
        }
        //out of range
        if (right == entries.size()) return entries.size();

        double candidateDist = dist(entries.get(right).get(), query.get());
        entries.get(right).setScore(candidateDist);
        answers.add(entries.get(right));
        if (answers.size() > k) {
            answers.poll();
        }
        if (candidateDist < radius) {
            return searchOutward(right + 1, dist, entries, query, radius, k, answers);
        }
        return right;
    }


    private int binarySearch(List<Entry> entries, double dist) {
        int left = 0, right = entries.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (entries.get(mid).getDist() == dist) break;
            else if (entries.get(mid).getDist() < dist) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return left;
    }

    private class Group {

        private List<List<Entry>> groups;

        private Entry[] representatives;

        private double[] dMax;

        int length() {
            return this.groups.size();
        }

        List<Entry> getGroup(int i) {
            if (i < 0 || i > length()) return null;
            return Collections.unmodifiableList(this.groups.get(i));
        }

        Entry getRepresentative(int i) {
            if (i < 0 || i > length()) return null;
            return this.representatives[i];
        }

        double getDisMax(int i) {
            if (i < 0 || i > length()) {
                throw new IllegalArgumentException("Array out of range. (build index before search)");
            }
            return this.dMax[i];
        }
    }

}
