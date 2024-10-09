package io.oigres.ecomm.cache;

import java.util.concurrent.locks.Lock;

/**
 * Factory class to create locks according to the cache provider.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
public interface CacheLockFactory {

    Lock create(String name);

}
