package com.khalwsh.chat_service.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// logs every service and controller call so we can trace what's going on
// without sprinkling log statements everywhere
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // all public methods in service impls
    @Pointcut("execution(* com.khalwsh.chat_service.service.impl..*(..))")
    private void serviceMethods() {}

    // all public methods in controllers
    @Pointcut("execution(* com.khalwsh.chat_service.controller..*(..))")
    private void controllerMethods() {}

    @Around("serviceMethods()")
    public Object logServiceCall(ProceedingJoinPoint jp) throws Throwable {
        String method = jp.getSignature().toShortString();
        log.debug(">>> {} args={}", method, summarizeArgs(jp.getArgs()));
        long start = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("<<< {} returned in {}ms", method, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("!!! {} threw {} after {}ms: {}", method, e.getClass().getSimpleName(), elapsed, e.getMessage());
            throw e;
        }
    }

    @Around("controllerMethods()")
    public Object logControllerCall(ProceedingJoinPoint jp) throws Throwable {
        String method = jp.getSignature().toShortString();
        log.info("=> {}", method);
        long start = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("<= {} completed in {}ms", method, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("<= {} failed after {}ms: {}", method, elapsed, e.getMessage());
            throw e;
        }
    }

    // keep it short — don't dump entire objects
    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : a.getClass().getSimpleName())
                .toList()
                .toString();
    }
}
