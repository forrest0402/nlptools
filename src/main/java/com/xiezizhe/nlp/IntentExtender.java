package com.xiezizhe.nlp;

import com.alibaba.fastjson.JSONObject;
import com.xiezizhe.nlp.embedding.PowerMeanEmb;
import com.xiezizhe.nlp.utils.ExcelUtils;
import com.xiezizhe.nlp.utils.NlpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xiezizhe
 * Date: 2018/11/9 5:03 PM
 */
@Service
public class IntentExtender {

    private static final Logger logger = LoggerFactory.getLogger(IntentExtender.class);

    @Autowired
    PowerMeanEmb powerMeanEmb;

    @Autowired
    ExcelUtils excelUtils;

    @Autowired
    Environment environment;

    private final static String PREFIX = "data/";

    private String split(String text, String[] references) {
        StringBuilder result = new StringBuilder();
        int curIdx = 0;
        for (int i = 0; i < references.length; i++) {
            String reference = references[i];
            boolean inside = true;
            for (int j = 0; j < reference.length(); ++j) {
                String character = reference.charAt(j) + "";
                int idx = text.indexOf(character, curIdx);
                if (idx == curIdx) {
                    if (result.length() > 0 && inside) {
                        result.append(" ");
                    }
                    inside = false;
                    result.append(character);
                    curIdx += 1;
                }
            }
        }
        return result.toString();
    }

    public void run() throws IOException {
        String fileName = "train.txt.bak.split";
        Set<String> values = new HashSet<>();
        List<String> modIntents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(PREFIX + fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject lineObj = JSONObject.parseObject(line);
                if ("".equals(lineObj.getString("value"))) {
                    continue;
                }
                String text = lineObj.getString("value");
                String replacement = split(NlpUtils.removeMeaninglessWords(text.replaceAll(" ", "")), text.split(" "));
                if (replacement.length() == 0)
                    replacement = text;
                if (replacement.length() == 0 || replacement.contains("银行 那个 周")) {
                    System.out.println(text);
                    split(NlpUtils.removeMeaninglessWords(text.replaceAll(" ", "")), text.split(" "));
                }
                lineObj.put("value", replacement);
                String value = lineObj.toJSONString();
                if (values.contains(value)) {
                    continue;
                }
                values.add(value);
                modIntents.add(value);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String modIntent : modIntents) {
                writer.write(modIntent);
                writer.newLine();
            }
        }

    }
}
