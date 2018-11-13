package com.xiezizhe.nlp;

import com.xiezizhe.nlp.config.AppConfig;
import com.xiezizhe.nlp.config.WordEmbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Hello world!
 */
@Service
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class,
            WordEmbConfig.class);

    public static void main(String[] args) {
        try {
            context.getBean(FaqCombinator2.class).run();
        } catch (Exception e) {
            logger.error("", e);
        }
    }
}
