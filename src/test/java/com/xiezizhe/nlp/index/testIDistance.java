package com.xiezizhe.nlp.index;

import com.xiezizhe.nlp.index.impl.BruteForce;
import com.xiezizhe.nlp.index.impl.iDistance;
import com.xiezizhe.nlp.model.Entry;
import com.xiezizhe.nlp.similarity.timeseries.EuclideanDistance;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author xiezizhe
 * @Description
 * @date 2018/9/2
 */
public class testIDistance {

    private static Logger logger = LoggerFactory.getLogger(testIDistance.class);

    private String testFileName = "test.iDistance.txt";
    Index<String> index1 = new iDistance<>();
    Index<String> index2 = new BruteForce<>();

    @Test
    public void test() {

        List<Entry<String>> entries = Arrays.asList(new Entry<>(new double[]{1, 1}, "1"),
                new Entry<>(new double[]{1, 2}, "2"),
                new Entry<>(new double[]{1, 3}, "3"),
                new Entry<>(new double[]{4, 5}, "4"),
                new Entry<>(new double[]{10, 20}, "5"),
                new Entry<>(new double[]{10, 21}, "6"),
                new Entry<>(new double[]{10, 30}, "7"),
                new Entry<>(new double[]{9, 20}, "8"),
                new Entry<>(new double[]{7, 20}, "9"),
                new Entry<>(new double[]{4, 8}, "10"));
        int k = 2;
        index1.build(entries, k);

        Entry<String> query = new Entry<>(new double[]{5, 5}, null);
        List<Entry<String>> ans = index1.top(query, 3);
        for (Entry an : ans) {
            System.out.print(an.toString());
            System.out.println(" -> " + EuclideanDistance.dist(an.getRepr(), query.getRepr()));
        }
    }

    @Test
    public void compare() {
        Random random = new Random();
        try (BufferedReader reader = new BufferedReader(new FileReader(testFileName))) {
            String line;
            List<Entry<String>> entries = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String data = line.split(" ")[0];
                double[] repr = Arrays.stream(line.split(" ")[1].split(","))
                        .mapToDouble(c -> Double.parseDouble(c))
                        .toArray();

                entries.add(new Entry<>(repr, data));
            }

            System.out.println(entries.size());
            index1.build(entries, 4);
            index2.build(entries, 4);

            int k = 4, size = 10;
//            double[] queryArray1 = new double[size];
//            for (int i = 0; i < queryArray1.length; i++) {
//                if (random.nextBoolean()) {
//                    queryArray1[i] = random.nextDouble() * 100;
//                } else {
//                    queryArray1[i] = -random.nextDouble() * 100;
//                }
//            }
            boolean flag = true;
            for (int i = 0; i < 1; i++) {
                if (!flag) {
                    break;
                }
                double[] queryArray = {-21.7494037490923, 57.42813583501694, 86.06123797308457, -4.413708342623801,
                        70.78576407905189, 40.042846471300834, -60.340407642937656, -47.15732793040862,
                        78.1607175994991, 22.728227634239907};// {55, 55, 55, 55, 55, 55, 55, 55, 55, 55};
//                for (int j = 0; j < queryArray.length; j++) {
//                    if (random.nextBoolean()) {
//                        queryArray[j] = random.nextDouble() * 100;
//                    } else {
//                        queryArray[j] = -random.nextDouble() * 100;
//                    }
//                }
                Entry<String> query = new Entry<>(queryArray, null);
                List<Entry<String>> result1 = index1.top(query, k);
                List<Entry<String>> result2 = index2.top(query, k);

                for (int j = 0; j < k; j++) {
                    if (!result1.get(j).equals(result2.get(j))) {
                        flag = false;
                        break;
                    }
                }
                System.out.println(i);
            }

            Assert.assertTrue(flag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void generate() {
        List<Entry<String>> entries = Arrays.asList(new Entry<>(new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, "1"),
                new Entry<>(new double[]{100, 100, 100, 100, 100, 100, 100, 100, 100, 100}, "100"),
                new Entry<>(new double[]{50, 50, 50, 50, 50, 50, 50, 50, 50, 50}, "50"),
                new Entry<>(new double[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 15}, "15"));

        Random random = new Random();
        int[] radiusArray = {15, 20, 30, 6};
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFileName))) {
            for (int i = 0; i < radiusArray.length; ++i) {
                int size = 100;
                Entry<String> repr = entries.get(i);
                writer.write(repr.toString());
                writer.newLine();
                for (int j = 0; j < size; j++) {
                    double[] array = new double[10];
                    for (int k = 0; k < array.length; k++) {
                        if (random.nextBoolean()) {
                            array[k] += repr.getRepr()[k] + random.nextDouble() * radiusArray[i];
                        } else {
                            array[k] += repr.getRepr()[k] - random.nextDouble() * radiusArray[i];
                        }
                    }
                    writer.write(repr.getData() + " " + Arrays.stream(array).mapToObj(c -> String.valueOf(c)).collect
                            (Collectors.joining(",")));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
