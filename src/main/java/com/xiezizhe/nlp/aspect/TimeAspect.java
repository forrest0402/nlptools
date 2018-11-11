package com.xiezizhe.nlp.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by xiezizhe
 * Date: 2018/11/5 1:05 PM
 */
@Aspect
@Component
public class TimeAspect {

    private static final Logger logger = LoggerFactory.getLogger(TimeAspect.class);


//    @Pointcut("execution(* com.xiezizhe.nlp.model.WordVector.init(..))")
//    public void sentenceEmb() {
//    }

    @Pointcut("execution(* com.xiezizhe.nlp.index..*.build(..))")
    public void buildIndex() {
    }

    @Around("buildIndex()")
    public void printRunningTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        joinPoint.proceed();
        long endTime = System.nanoTime();
        logger.info(joinPoint.getSignature().toString() + " running time is {} ms", (endTime - startTime) / 1000000L);
    }


}
