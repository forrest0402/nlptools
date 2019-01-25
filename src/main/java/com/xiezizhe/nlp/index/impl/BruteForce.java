package com.xiezizhe.nlp.index.impl;

import com.xiezizhe.nlp.index.Index;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;
import com.xiezizhe.nlp.utils.NlpUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Thread safe
 * <p>
 * Created by xiezizhe
 * Date: 2018/11/5 9:22 PM
 */
public class BruteForce<T> implements Index<T> {

    List<Entry<T>> entries = null;

    @Override
    public void build(List<Entry<T>> entries, int k) {
        this.entries = entries;
    }

    @Override
    public List<Entry<T>> top(Entry<T> entry, int k) {
//        PriorityQueue<Entry<T>> results = new PriorityQueue<>(Comparator.comparing(Entry::getScore));
//        for (Entry<T> candidate : entries) {
//            Entry<T> candidateEntry = new Entry<>(candidate.getRepr(), candidate.getData());
//            candidateEntry.setScore(NlpUtils.cosine(entry.getRepr(), candidate.getRepr()));
//            results.add(candidateEntry);
//            if (results.size() == k)
//                results.poll();
//        }
//        return results.stream().sorted(Comparator.comparing(Entry::getScore)).collect(Collectors.toList());

        PriorityQueue<Entry<T>> results = new PriorityQueue<>((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()));
        for (Entry<T> candidate : entries) {
            Entry<T> candidateEntry = new Entry<>(candidate.getRepr(), candidate.getData());
            candidateEntry.setScore(EuclideanDistance.dist(entry.getRepr(), candidate.getRepr()));
            results.add(candidateEntry);
            if (results.size() == k + 1)
                results.poll();
        }
        return results.stream().sorted(Comparator.comparing(Entry::getScore)).collect(Collectors.toList());
    }

    private boolean isValid(double[] value) {
        if (value == null) return false;
        int count = 0;
        for (int i = 0; i < value.length; i++) {
            if (value[i] == 0) ++count;
        }
        return count != value.length;
    }

    @Override
    public List<Entry<T>> top(Entry<T> entry, double threshold) {
        if (!isValid(entry.getRepr())) {
            return new ArrayList<>();
        }

        List<Entry<T>> results = new ArrayList<>();
        for (Entry<T> candidate : entries) {
            double score = cosineSimilarity(entry.getRepr(), candidate.getRepr()) * 0.6;
            score += 0.4 * NlpUtils.stringDistance(entry.getData().toString(), candidate.getData().toString());
            if (score < threshold) continue;
            Entry<T> candidateEntry = new Entry<>(candidate.getRepr(), candidate.getData());
            candidateEntry.setScore(score);
            results.add(candidateEntry);
            if (score < 0.99) {
                System.out.print("");
            }
        }
        results.sort((e1, e2) -> Double.compare(e2.getScore(), e1.getScore()));
        return results;
    }

    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public List<Entry<T>> top(Entry<T> entry, int k, double deltaR) {
        return null;
    }
}
