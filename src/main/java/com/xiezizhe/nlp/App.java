package com.xiezizhe.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class App {
    private static void f() {
        int a = 0;
        System.out.println(56 / a);

    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("1");
        try {
            f();
        } catch (Exception e) {
            Object ee = e.getStackTrace()[0];
        }
        Object[] array = list.toArray();
        List<String> temp = list.stream().filter(c -> c.length() > 5).collect(Collectors.toList());
        System.out.println("Hello World!");
    }
}
