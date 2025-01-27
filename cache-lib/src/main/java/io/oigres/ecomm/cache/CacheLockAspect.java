/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.cache;

import java.util.List;
import java.util.concurrent.locks.Lock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Aspect class to process @CacheLock annotation
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Slf4j
@Aspect
public class CacheLockAspect {

  @Around("@annotation(io.oigres.ecomm.cache.annotations.CacheLock)")
  public Object cacheLock(ProceedingJoinPoint joinPoint) throws Throwable {
    log.debug("Processing cache lock");
    boolean startLocking = !CacheLockManager.isSynchronizationActive();
    if (startLocking) { // if there is more than one aspect in the calls chain, only the first one
      // initializes and unlock all locks which were created.
      CacheLockManager.initSynchronization();
    }
    try {
      return joinPoint.proceed();
    } finally {
      if (startLocking) {
        List<Lock> locks = CacheLockManager.getLocks();
        locks.forEach(
            lock -> {
              try {
                lock.unlock();
              } catch (Throwable t) {
                log.warn(
                    String.format(
                        "Cache lock '%s' release failed: %s", lock.toString(), t.getMessage()));
              }
            });
        CacheLockManager.clearLocks();
      }
    }
  }
}
