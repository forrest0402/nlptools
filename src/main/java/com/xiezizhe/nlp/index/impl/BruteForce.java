package com.xiezizhe.nlp.index.impl;

import com.xiezizhe.nlp.index.Index;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.utils.NlpUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
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
        PriorityQueue<Entry<T>> results = new PriorityQueue<>(Comparator.comparing(Entry::getScore));
        for (Entry<T> candidate : entries) {
            candidate.setScore(NlpUtils.cosine(entry.getRepr(), candidate.getRepr()));
            results.add(candidate);
            if (results.size() == k)
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

        PriorityQueue<Entry<T>> results = new PriorityQueue<>(Comparator.comparing(Entry::getScore));
        for (Entry<T> candidate : entries) {
            double score = NlpUtils.cosine(entry.getRepr(), candidate.getRepr());
            if (score < threshold) continue;
            candidate.setScore(score);
            results.add(candidate);
        }
        return results.stream().sorted(Comparator.comparing(Entry::getScore)).collect(Collectors.toList());
    }

    @Override
    public List<Entry<T>> top(Entry<T> entry, int k, double deltaR) {
        return null;
    }
}
