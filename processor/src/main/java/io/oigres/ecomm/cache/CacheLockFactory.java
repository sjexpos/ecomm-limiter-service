package io.oigres.ecomm.cache;

import java.util.concurrent.locks.Lock;

public interface CacheLockFactory {

    Lock create(String name);

}
