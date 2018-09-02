package com.xiezizhe.nlp.index;

import com.xiezizhe.nlp.model.Entry;

import java.util.List;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:26
 */
public interface Index {

    void build(List<Entry> entries, int k);

    List<Entry> top(Entry entry, int k);

    List<Entry> top(Entry entry, int k, double deltaR);

}
