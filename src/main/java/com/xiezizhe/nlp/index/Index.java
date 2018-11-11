package com.xiezizhe.nlp.index;

import com.xiezizhe.nlp.model.Entry;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by xiezizhe
 * Date: 2018/8/28 上午10:26
 */
public interface Index<T> {

    void build(List<Entry<T>> entries, int k);

    List<Entry<T>> top(Entry<T> entry, int k);

    List<Entry<T>> top(Entry<T> entry, double threshold);

    List<Entry<T>> top(Entry<T> entry, int k, double deltaR);

    static void save(Index index, String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(index);
        }
    }

    static Index load(String path) throws ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            Object obj = ois.readObject();
            return (Index) obj;
        } catch (IOException e) {
            return null;
        }
    }

}
