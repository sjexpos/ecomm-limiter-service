package io.oigres.ecomm.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class CacheLockAspect {

//    @Pointcut("@annotation(io.oigres.ecomm.cache.annotations.CacheLock)")
//    private void check(){}

    @Around("@annotation(io.oigres.ecomm.cache.annotations.CacheLock)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

}
