package com.xiezizhe.nlp.config;

import com.xiezizhe.nlp.model.WordVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by xiezizhe
 * Date: 2018/11/2 11:15 AM
 */
@Configuration
public class WordEmbConfig {

    private static Logger logger = LoggerFactory.getLogger(WordEmbConfig.class);

    @Autowired
    Environment environment;

    @Autowired
    ApplicationContext context;

    @Bean
    @Qualifier("tencent")
    public WordVector getTencentWordEmb() {
        String fileName = environment.getProperty("tencent.emb.path", "model/tencent.vec");
        WordVector vector = new WordVector(fileName);
        vector.init(fileName);
        return vector;
    }

    @Bean
    @Qualifier("256")
    public WordVector getWordEmb1() {
        String fileName = environment.getProperty("general1.emb.path", "model/model.vec");
        WordVector vector = new WordVector(fileName);
        vector.init(fileName);
        return vector;
    }

    @Bean
    @Qualifier("128")
    public WordVector getWordEmb2() {
        String fileName = environment.getProperty("general2.emb.path", "model/wordvec.vec");
        WordVector vector = new WordVector(fileName);
        vector.init(fileName);
        return vector;
    }
}
