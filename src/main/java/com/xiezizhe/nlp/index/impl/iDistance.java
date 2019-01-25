package com.xiezizhe.nlp.index.impl;

import com.xiezizhe.nlp.index.Index;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.DistanceFunction;
import com.xiezizhe.nlp.similarity.Similarity;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:30
 */
public class iDistance<T> implements Index<T>, Serializable {

    private static final long serialVersionUID = -1128021648697075434L;

    private static Logger logger = LoggerFactory.getLogger(iDistance.class);

    private static final double DEFAULT_DELTA = 20;

    private static final double KMEANS_THRESHOLD = 1.0;

    private static final int MAX_ROUNDS = 10000;

    private static ThreadLocal<Boolean> stopFlags = new ThreadLocal<>();

    private Group group;

    private volatile boolean ready = false;

    private transient DistanceFunction<Double, Double> func = (e1, e2) -> (e1 - e2) * (e1 - e2);
    private transient Similarity<Double> similarity = new EuclideanDistance<>(func);

    @Override
    public synchronized void build(List<Entry<T>> entries, int k) {
        if (ready) return;
        logger.info("building index - {}", entries.size());
        if (!validate(entries, k)) {
            throw new IllegalArgumentException("k is too large or entries are invalid");
        }
        this.group = kMeans(entries, k);
        this.group.show();
        ready = true;
        logger.info("building complete");
    }

    @Override
    public List<Entry<T>> top(Entry<T> query, int k) {
        return top(query, k, DEFAULT_DELTA);
    }

    @Override
    public List<Entry<T>> top(Entry<T> query, double threshold) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public List<Entry<T>> top(Entry<T> query, int k, double deltaR) {
        if (!this.ready || query == null) return null;
        if (k < 0 || deltaR < 0) throw new IllegalArgumentException("arguments must be positive");
        if (k > this.group.getCandidateNumber()) throw new IllegalArgumentException("k is too large");

        double r = 0;
        stopFlags.set(false);
        PriorityQueue<Entry<T>> answers = new PriorityQueue<>(Comparator.comparing(Entry<T>::getScore).reversed());
        boolean[] visit = new boolean[this.group.length()];
        int[] lp = new int[this.group.length()];
        int[] rp = new int[this.group.length()];

        while (!stopFlags.get()) {
            r += deltaR;
            searchO(query, r, answers, k, visit, lp, rp);
        }

        return answers.stream().sorted(Comparator.comparing(Entry::getScore)).collect(Collectors.toList());
    }

    private void searchO(Entry<T> query, double radius, PriorityQueue<Entry<T>> answers,
                         int k, boolean[] visit, int[] lp, int[] rp) {

        if (answers.size() == k && answers.peek().getScore() <= radius) {
            stopFlags.set(true);
        }

        for (int i = 0; i < this.group.length(); i++) {
            Entry representative = this.group.getRepresentative(i);
            double dist = dist(representative.getRepr(), query.getRepr());

            //group i has not been searched before
            if (!visit[i]) {
                // group i contains query
                if (dist + radius < this.group.getDisMax(i)) {
                    visit[i] = true;
                    lp[i] = searchInward(-1, dist, this.group.getGroup(i), query, radius, k, answers);
                    rp[i] = searchOutward(-1, dist, this.group.getGroup(i), query, radius, k, answers);
                }// group i intersets query
                else if (dist - radius < this.group.getDisMax(i)) {
                    visit[i] = true;
                    lp[i] = searchInward(-1, dist, this.group.getGroup(i), query, radius, k, answers);
                }
            } else {
                //continue the search process
                lp[i] = searchInward(lp[i], dist, this.group.getGroup(i), query, radius, k, answers);
                rp[i] = searchOutward(rp[i], dist, this.group.getGroup(i), query, radius, k, answers);
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

    private int searchInward(int left, double dist, List<Entry<T>> entries,
                             Entry<T> query, double radius, int k,
                             PriorityQueue<Entry<T>> answers) {
        if (left == -1) {
            left = binarySearch(entries, dist);
            if (left == entries.size()) --left;
        }
        // 0 position is representative
        if (left == 0 || left == entries.size()) return left;

        double candidateDist = dist(entries.get(left).getRepr(), query.getRepr());
        entries.get(left).setScore(candidateDist);
        answers.add(entries.get(left));
        if (answers.size() > k) {
            answers.poll();
        }
        if (candidateDist < radius) {
            return searchInward(left - 1, dist, entries, query, radius, k, answers);
        }
        return left - 1;
    }

    private int searchOutward(int right, double dist, List<Entry<T>> entries,
                              Entry<T> query, double radius, int k,
                              PriorityQueue<Entry<T>> answers) {
        if (right == -1) {
            right = binarySearch(entries, dist) + 1;
        }
        //out of range
        if (right == 0 || right >= entries.size()) return entries.size();

        double candidateDist = dist(entries.get(right).getRepr(), query.getRepr());
        entries.get(right).setScore(candidateDist);
        answers.add(entries.get(right));
        if (answers.size() > k) {
            answers.poll();
        }
        if (candidateDist < radius) {
            return searchOutward(right + 1, dist, entries, query, radius, k, answers);
        }
        return right + 1;
    }


    private int binarySearch(List<Entry<T>> entries, double dist) {
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

    private Group kMeans(List<Entry<T>> entries, int k) {
        Random random = new SecureRandom();
        final Entry<T>[] representatives = new Entry[k];
        final double[] dMax = new double[k];
        Map<Integer, List<Entry<T>>> groupMap = null;

        boolean[] chosenIdx = new boolean[entries.size()];
        for (int i = 0; i < representatives.length; i++) {
            int idx = random.nextInt(entries.size());
            while (chosenIdx[idx]) idx = random.nextInt(entries.size());
            chosenIdx[idx] = true;
            representatives[i] = new Entry<>(entries.get(idx));
        }

        representatives[0] = new Entry<>(entries.get(0));
        representatives[1] = new Entry<>(entries.get(101));
        representatives[2] = new Entry<>(entries.get(202));
        representatives[3] = new Entry<>(entries.get(303));

        double preVariance = Double.MAX_VALUE;
        int round = 0;
        while (++round < MAX_ROUNDS && preVariance > KMEANS_THRESHOLD) {

            groupMap = new HashMap<>();
            double variance = 0.0;

            for (Entry<T> entry : entries) {
                double minDist = Double.MAX_VALUE;
                int groupIdx = 0;
                for (int i = 0; i < representatives.length; i++) {
                    double dist = dist(representatives[i].getRepr(), entry.getRepr());
                    if (dist < minDist) {
                        minDist = dist;
                        groupIdx = i;
                    }
                }
                List<Entry<T>> groupMembers = groupMap.get(groupIdx);
                if (groupMembers == null) {
                    groupMembers = new ArrayList<>();
                    groupMap.put(groupIdx, groupMembers);
                }
                groupMembers.add(entry);
                variance += minDist;
            }

            for (int i = 0; i < representatives.length; i++) {
                List<Entry<T>> groupMembers = groupMap.get(i);
                double[] representative = representatives[i].getRepr();
                for (int j = 0; j < representative.length; j++) {
                    final int idx = j;
                    representative[j] = groupMembers.stream().mapToDouble(e -> e.getRepr()[idx]).average()
                            .getAsDouble();
                }
            }

            variance /= entries.size();
//            if (preVariance - variance < preVariance * 0.001) break;
            preVariance = variance;
        }

        groupMap.forEach((key, value) -> dMax[key] = value.stream()
                .mapToDouble(e -> dist(e.getRepr(), representatives[key].getRepr()))
                .max()
                .getAsDouble()
        );

        List<List<Entry<T>>> groups = new ArrayList<>(new TreeMap<>(groupMap).values());
        for (int i = 0; i < representatives.length; i++) {
            groups.get(i).add(0, representatives[i]);
            for (Entry<T> entry : groups.get(i)) {
                entry.setDist(dist(entry.getRepr(), representatives[i].getRepr()));
            }
            Collections.sort(groups.get(i), Comparator.comparingDouble(Entry::getDist));
        }

        return new Group(groups, representatives, dMax, entries.size());
    }


    private boolean validate(List<Entry<T>> entries, int k) {
        if (entries == null || entries.size() == 0 || k > entries.size()) {
            return false;
        }
        int dimension = entries.get(0).getDimension();
        for (Entry<T> entry : entries) {
            if (dimension != entry.getDimension()) {
                return false;
            }
        }
        return true;
    }

    private class Group<T> implements Serializable {

        private static final long serialVersionUID = 5634363310910217267L;

        private final List<List<Entry<T>>> groups;

        private final Entry<T>[] representatives;

        private final double[] dMax;

        private final int candidateNumber;

        private Group(List<List<Entry<T>>> groups, Entry<T>[] representatives, double[] dMax, int candidateNumber) {
            this.groups = groups;
            this.representatives = representatives;
            this.dMax = dMax;
            this.candidateNumber = candidateNumber;
        }

        int length() {
            return this.groups.size();
        }

        List<Entry<T>> getGroup(int i) {
            if (i < 0 || i > length()) return null;
            return Collections.unmodifiableList(this.groups.get(i));
        }

        Entry<T> getRepresentative(int i) {
            if (i < 0 || i > length()) return null;
            return this.representatives[i];
        }

        double getDisMax(int i) {
            if (i < 0 || i > length()) {
                throw new IllegalArgumentException("Array out of range. (build index before search)");
            }
            return this.dMax[i];
        }

        public int getCandidateNumber() {
            return candidateNumber;
        }

        void show() {
            for (int i = 0; i < representatives.length; i++) {
                Entry<T> representative = representatives[i];
                if (logger.isDebugEnabled()) {
                    logger.debug("{}: {}, [{}] ", i, representative.toString(), groups.get(i).toString());
                }
            }
        }
    }

}
