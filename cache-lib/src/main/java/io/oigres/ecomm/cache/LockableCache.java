package io.oigres.ecomm.cache;

import org.springframework.cache.Cache;

import java.util.concurrent.locks.Lock;

/**
 * Defines a spring cache which can be locked.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
public interface LockableCache extends Cache {

    Lock getLock(Object key);

}
