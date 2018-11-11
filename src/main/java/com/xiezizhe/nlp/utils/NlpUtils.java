package com.xiezizhe.nlp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiezizhe
 * Date: 2018/11/2 11:07 AM
 */
@Service
public class NlpUtils {

    private static final Logger logger = LoggerFactory.getLogger(NlpUtils.class);

    private static List<Pattern> meaninglessWordsPatterns;
    private static final String MEANLESS_PATTERN_PATH = "meaningless.pattern";

    public static double cosine(double[] array1, double[] array2) {
        if (array1 == null || array2 == null || array1.length != array2.length) {
            throw new IllegalArgumentException("");
        }
        double sum = 0;
        for (int i = 0; i < array1.length; i++) {
            sum += array1[i] * array2[i];
        }
        return sum / (lpNorm(array1, 2) * lpNorm(array2, 2));
    }

    public static double lpNorm(double[] array, int p) {
        if (array == null || p < 1) {
            throw new IllegalArgumentException("");
        }
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += Math.pow(array[i], p);
        }
        return Math.pow(sum, 1.0 / p);
    }

    public static double[] concat(double[]... array) {
        if (array == null || array.length == 0)
            return null;
        int length = 0;
        for (int i = 0; i < array.length; i++) {
            length += array[i].length;
        }
        double[] result = new double[length];
        int pos = 0;
        for (int i = 0; i < array.length; i++) {
            System.arraycopy(array[i], 0, result, pos, array[i].length);
            pos += array[i].length;
        }
        return result;
    }

    public static void selfAdd(double[] array1, double[] array2) {
        if (array1 == null || array2 == null || array1.length != array2.length) {
            throw new IllegalArgumentException("");
        }

        for (int i = 0; i < array1.length; i++) {
            array1[i] += array2[i];
        }
    }

    public static void selfMul(double[] array1, double number) {
        if (array1 == null) {
            throw new IllegalArgumentException("");
        }
        for (int i = 0; i < array1.length; i++) {
            array1[i] *= number;
        }
    }

    public static boolean isDigit(String token) {
        Pattern pattern = Pattern.compile("(\\d+|[一二三四五六七八九十]+)[几个十百千万]*(多|左右)?(元|对|块|美元|人民币|期|公里|千米|年)?");
        return pattern.matcher(token).find();
    }

    public static String removeMeaninglessWords(String text) {
        loadPatterns();
        boolean flag = true;
        String result = text;
        while (flag) {
            flag = false;
            for (Pattern pattern : meaninglessWordsPatterns) {
                if (pattern.matcher(result).find()) {
                    flag = true;
                    result = result.replaceAll(pattern.toString(), "");
                    System.out.print("");
                }
            }
        }
        return result;
    }

    private static void loadPatterns() {
        if (meaninglessWordsPatterns == null) {
            meaninglessWordsPatterns = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(NlpUtils.class.getClassLoader()
                    .getResourceAsStream(MEANLESS_PATTERN_PATH)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    meaninglessWordsPatterns.add(Pattern.compile(line));
                }
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }
}
