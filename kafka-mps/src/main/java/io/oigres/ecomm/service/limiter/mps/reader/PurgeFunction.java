package io.oigres.ecomm.service.limiter.mps.reader;

import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface PurgeFunction<E> {

    boolean isPurgeable(E x) throws TimeoutException;

}
