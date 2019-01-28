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
    private String testFileName2 = "test.iDistance.big.txt";
    private int dimensionSize = 100;
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
    public void compareEfficiency() {
        Random random = new Random();
        try (BufferedReader reader = new BufferedReader(new FileReader(testFileName2))) {
            String line;
            List<Entry<String>> entries = new ArrayList<>();
            List<double[]> seeds = new ArrayList<>();
            String preObj = null;
            while ((line = reader.readLine()) != null) {
                String data = line.split(" ")[0];
                double[] repr = Arrays.stream(line.split(" ")[1].split(","))
                        .mapToDouble(c -> Double.parseDouble(c))
                        .toArray();

                entries.add(new Entry<>(repr, data));

                if (preObj == null || !preObj.equals(data)) {
                    preObj = data;
                    seeds.add(repr.clone());
                }
            }

            System.out.println(entries.size());
            index1.setSeeds(seeds);
            index1.build(entries, seeds.size());
            index2.build(entries, seeds.size());

            int k = 50, size = dimensionSize, totalCount = 10;
            boolean flag = true;
            long totalTime1 = 0, totalTime2 = 0;
            for (int i = 0; i < totalCount; i++) {
                if (!flag) {
                    break;
                }

                double[] queryArray = new double[size];
                for (int j = 0; j < queryArray.length; j++) {
                    if (random.nextBoolean()) {
                        queryArray[j] = random.nextDouble() * 80;
                    } else {
                        queryArray[j] = -random.nextDouble() * 100;
                    }
                }
                long t1 = System.nanoTime();
                for (Entry<String> entry : entries) {
                    EuclideanDistance.dist(entry.getRepr(), queryArray);
                }
                long t2 = System.nanoTime();
                System.out.println(t2 - t1);

                Entry<String> query = new Entry<>(queryArray, null);
                long startTime = System.nanoTime();
                List<Entry<String>> result1 = index1.top(query, k);
                long endTime = System.nanoTime();
                totalTime1 += (endTime - startTime);
                startTime = System.nanoTime();
                List<Entry<String>> result2 = index2.top(query, k);
                endTime = System.nanoTime();
                totalTime2 += (endTime - startTime);

                for (int j = 0; j < k; j++) {
                    if (!result1.get(j).equals(result2.get(j))) {
                        flag = false;
                        break;
                    }
                }
            }

            System.out.println(totalTime1 / totalCount);
            System.out.println(totalTime2 / totalCount);

            Assert.assertTrue(flag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void validateCorrectness() {
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
            boolean flag = true;
            for (int i = 0; i < 1000; i++) {
                if (!flag) {
                    break;
                }
                double[] queryArray = new double[size];// {55, 55, 55, 55, 55, 55, 55, 55, 55, 55};
                for (int j = 0; j < queryArray.length; j++) {
                    if (random.nextBoolean()) {
                        queryArray[j] = random.nextDouble() * 100;
                    } else {
                        queryArray[j] = -random.nextDouble() * 100;
                    }
                }
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
        int classNumber = 1000;
        int[] seeds = new int[classNumber];
        int[] radiusArray = new int[classNumber];
        for (int i = 0; i < radiusArray.length; i++) {
            seeds[i] = 50 * i;
            radiusArray[i] = 30;
        }

        List<Entry<String>> centries = new ArrayList<>();
        for (int i = 0; i < seeds.length; i++) {
            double[] repr = new double[dimensionSize];
            Arrays.fill(repr, seeds[i]);
            centries.add(new Entry<>(repr, String.valueOf(seeds[i])));
        }

        Random random = new Random();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFileName2))) {
            for (int i = 0; i < radiusArray.length; ++i) {
                int size = 1000;
                Entry<String> repr = centries.get(i);
                writer.write(repr.toString());
                writer.newLine();
                for (int j = 0; j < size; j++) {
                    double[] array = new double[dimensionSize];
                    for (int k = 0; k < array.length; k++) {
                        if (random.nextBoolean()) {
                            array[k] += repr.getRepr()[k] + random.nextDouble() * radiusArray[i];
                        } else {
                            array[k] += repr.getRepr()[k] - random.nextDouble() * radiusArray[i];
                        }
                    }
                    writer.write(repr.getData() + " " + Arrays.stream(array).mapToObj(c -> String.valueOf(c))
                            .collect(Collectors.joining(",")));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
