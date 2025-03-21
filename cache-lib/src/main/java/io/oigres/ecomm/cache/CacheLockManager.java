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

import java.util.*;
import java.util.concurrent.locks.Lock;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;

/**
 * This class stores lock state which is attached to each thread.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
public class CacheLockManager {

  private static final ThreadLocal<Set<Lock>> locks = new NamedThreadLocal<>("Cache locks");
  private static final ThreadLocal<Map<String, Lock>> lockNames =
      new NamedThreadLocal<>("Cache locks names");

  /**
   * Return if cache synchronization is active for the current thread.
   * Can be called before register to avoid unnecessary instance creation.
   * @see #registerLock
   */
  public static boolean isSynchronizationActive() {
    return (locks.get() != null);
  }

  /**
   * Activate cache synchronization for the current thread.
   * Called by a cache aspect on synchronization begin.
   * @throws IllegalStateException if synchronization is already active
   */
  public static void initSynchronization() throws IllegalStateException {
    if (isSynchronizationActive()) {
      throw new IllegalStateException("Cannot activate cache synchronization - already active");
    }
    locks.set(new LinkedHashSet<>());
    lockNames.set(new LinkedHashMap<>());
  }

  /**
   * Register a new lock for the current thread.
   * Typically called by LockAwareCacheDecorator.
   * @param lock the lock object to register
   * @throws IllegalStateException if cache synchronization is not active
   */
  public static void registerLock(Lock lock, String name) throws IllegalStateException {
    Assert.notNull(lock, "Lock must not be null");
    Set<Lock> synchs = locks.get();
    Map<String, Lock> names = lockNames.get();
    if (synchs == null) {
      throw new IllegalStateException("Cache synchronization is not active");
    }
    synchs.add(lock);
    names.put(name, lock);
  }

  public static Lock getLocks(String name) throws IllegalStateException {
    if (!isSynchronizationActive()) {
      throw new IllegalStateException("Cache synchronization is not active");
    }
    return lockNames.get().get(name);
  }

  /**
   * Return an unmodifiable snapshot list of all locks for the current thread.
   * @return unmodifiable List of Lock instances
   * @throws IllegalStateException if synchronization is not active
   * @see Lock
   */
  public static List<Lock> getLocks() throws IllegalStateException {
    if (!isSynchronizationActive()) {
      throw new IllegalStateException("Cache synchronization is not active");
    }
    Set<Lock> synchs = locks.get();
    // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
    // while iterating and invoking synchronization callbacks that in turn
    // might register further synchronizations.
    if (synchs.isEmpty()) {
      return Collections.emptyList();
    } else if (synchs.size() == 1) {
      return Collections.singletonList(synchs.iterator().next());
    } else {
      return List.copyOf(synchs);
    }
  }

  /**
   * Deactivate cache synchronization for the current thread.
   * Called by the Cache aspect on locks cleanup.
   * @throws IllegalStateException if synchronization is not active
   */
  public static void clearLocks() throws IllegalStateException {
    if (!isSynchronizationActive()) {
      throw new IllegalStateException("Cannot deactivate cache synchronization - not active");
    }
    locks.remove();
    lockNames.remove();
  }
}
