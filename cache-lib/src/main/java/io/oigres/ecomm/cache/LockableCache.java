package io.oigres.ecomm.cache;

import org.springframework.cache.Cache;

import java.util.concurrent.locks.Lock;

public interface LockableCache extends Cache {

    Lock getLock(Object key);

}
