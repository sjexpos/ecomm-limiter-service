package io.oigres.ecomm.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Slf4j
@Aspect
public class CacheLockAspect {

    @Around("@annotation(io.oigres.ecomm.cache.annotations.CacheLock)")
    public Object cacheLock(ProceedingJoinPoint joinPoint) throws Throwable {
        log.debug("Processing cache lock");
        boolean startLocking = !CacheLockManager.isSynchronizationActive();
        if (startLocking) { // if there is more than one aspect in the calls chain, only the first one initializes and unlock all locks which were created.
            CacheLockManager.initSynchronization();
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (startLocking) {
                List<Lock> locks = CacheLockManager.getLocks();
                locks.forEach(lock -> {
                    try {
                        lock.unlock();
                    } catch (Throwable t) {
                        log.warn(String.format("Cache lock '%s' release failed: %s", lock.toString(), t.getMessage()));
                    }
                });
                CacheLockManager.clearLocks();
            }
        }
    }

}
