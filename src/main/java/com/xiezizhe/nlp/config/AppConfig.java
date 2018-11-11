package com.xiezizhe.nlp.config;

import com.xiezizhe.nlp.aspect.TimeAspect;
import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by xiezizhe
 * Date: 2018/11/2 11:13 AM
 */
@Configuration
@ComponentScan(basePackages = {"com.xiezizhe.nlp"})
@PropertySource(value = {"app.properties"}, encoding = "utf-8")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AppConfig {

//    @Bean
//    public TimeAspect timeAspect() {
//        return Aspects.aspectOf(TimeAspect.class);
//    }
}
