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

package io.oigres.ecomm.service.limiter.mps.reader;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An optionally-bounded {@linkplain java.util.concurrent.BlockingQueue blocking queue} based on
 * linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 */
class PendingQueue<E> implements java.io.Serializable {
  private static final long serialVersionUID = -6903933977591709194L;

  /**
   * Linked list node class.
   */
  static class Node<E> {
    AtomicInteger orderIteratorCount =
        new AtomicInteger(0); // store how many OrderIterator are pointing this node
    E item;

    /**
     * One of:
     * - the real successor Node
     * - this Node, meaning the successor is head.next
     * - null, meaning there is no successor (this is the last node)
     */
    Node<E> next;

    Node(E x) {
      item = x;
    }
  }

  /** The capacity bound, or Integer.MAX_VALUE if none */
  private final int capacity;

  /** Current number of elements */
  private final AtomicInteger count = new AtomicInteger();

  /**
   * Head of linked list.
   * Invariant: head.item == null
   */
  private transient Node<E> head;

  /**
   * Tail of linked list.
   * Invariant: last.next == null
   */
  private transient Node<E> last;

  /** Lock held by take, poll, etc */
  private final ReentrantLock takeLock = new ReentrantLock();

  /** Wait queue for waiting takes */
  @SuppressWarnings("serial") // Classes implementing Condition may be serializable.
  private final Condition notEmpty = takeLock.newCondition();

  /** Lock held by put, offer, etc */
  private final ReentrantLock putLock = new ReentrantLock();

  /** Wait queue for waiting puts */
  @SuppressWarnings("serial") // Classes implementing Condition may be serializable.
  private final Condition notFull = putLock.newCondition();

  /** Wait queue for waiting puts */
  @SuppressWarnings("serial") // Classes implementing Condition may be serializable.
  private final Condition newElement = putLock.newCondition();

  /**
   * Signals a waiting take. Called only from put/offer (which do not
   * otherwise ordinarily lock takeLock.)
   */
  private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
  }

  /**
   * Signals a waiting put. Called only from take/poll.
   */
  private void signalNotFull() {
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
      notFull.signal();
    } finally {
      putLock.unlock();
    }
  }

  /**
   * Links node at end of queue.
   *
   * @param node the node
   */
  private void enqueue(Node<E> node) {
    // assert putLock.isHeldByCurrentThread();
    // assert last.next == null;
    last = last.next = node;
    this.newElement.signal();
  }

  /**
   * Removes a node from head of queue.
   *
   * @return the node
   */
  private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
  }

  /**
   * Locks to prevent both puts and takes.
   */
  void fullyLock() {
    putLock.lock();
    takeLock.lock();
  }

  /**
   * Unlocks to allow both puts and takes.
   */
  void fullyUnlock() {
    takeLock.unlock();
    putLock.unlock();
  }

  /**
   * Creates a {@code LinkedBlockingQueue} with a capacity of
   * {@link Integer#MAX_VALUE}.
   */
  public PendingQueue() {
    this(Integer.MAX_VALUE);
  }

  /**
   * Creates a {@code LinkedBlockingQueue} with the given (fixed) capacity.
   *
   * @param capacity the capacity of this queue
   * @throws IllegalArgumentException if {@code capacity} is not greater
   *         than zero
   */
  public PendingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    last = head = new Node<E>(null);
  }

  /**
   * Creates a {@code LinkedBlockingQueue} with a capacity of
   * {@link Integer#MAX_VALUE}, initially containing the elements of the
   * given collection,
   * added in traversal order of the collection's iterator.
   *
   * @param c the collection of elements to initially contain
   * @throws NullPointerException if the specified collection or any
   *         of its elements are null
   */
  public PendingQueue(Collection<? extends E> c) {
    this(Integer.MAX_VALUE);
    final ReentrantLock putLock = this.putLock;
    putLock.lock(); // Never contended, but necessary for visibility
    try {
      int n = 0;
      for (E e : c) {
        if (e == null) throw new NullPointerException();
        if (n == capacity) throw new IllegalStateException("Queue full");
        enqueue(new Node<E>(e));
        ++n;
      }
      count.set(n);
    } finally {
      putLock.unlock();
    }
  }

  // this doc comment is overridden to remove the reference to collections
  // greater in size than Integer.MAX_VALUE
  /**
   * Returns the number of elements in this queue.
   *
   * @return the number of elements in this queue
   */
  public int size() {
    return count.get();
  }

  // this doc comment is a modified copy of the inherited doc comment,
  // without the reference to unlimited queues.
  /**
   * Returns the number of additional elements that this queue can ideally
   * (in the absence of memory or resource constraints) accept without
   * blocking. This is always equal to the initial capacity of this queue
   * less the current {@code size} of this queue.
   *
   * <p>Note that you <em>cannot</em> always tell if an attempt to insert
   * an element will succeed by inspecting {@code remainingCapacity}
   * because it may be the case that another thread is about to
   * insert or remove an element.
   */
  public int remainingCapacity() {
    return capacity - count.get();
  }

  /**
   * Inserts the specified element at the tail of this queue, waiting if
   * necessary for space to become available.
   *
   * @throws InterruptedException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    final int c;
    final Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
      /*
       * Note that count is used in wait guard even though it is
       * not protected by lock. This works because count can
       * only decrease at this point (all other puts are shut
       * out by lock), and we (or some other waiting put) are
       * signalled if it ever changes from capacity. Similarly
       * for all other uses of count in other wait guards.
       */
      while (count.get() == capacity) {
        notFull.await();
      }
      enqueue(node);
      c = count.getAndIncrement();
      if (c + 1 < capacity) notFull.signal();
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
  }

  /**
   * Inserts the specified element at the tail of this queue, waiting if
   * necessary up to the specified wait time for space to become available.
   *
   * @return {@code true} if successful, or {@code false} if
   *         the specified waiting time elapses before space is available
   * @throws InterruptedException {@inheritDoc}
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean offer(E e, Duration timeout) throws InterruptedException {

    if (e == null) throw new NullPointerException();
    long nanos = timeout.toNanos();
    final int c;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
      while (count.get() == capacity) {
        if (nanos <= 0L) return false;
        nanos = notFull.awaitNanos(nanos);
      }
      enqueue(new Node<E>(e));
      c = count.getAndIncrement();
      if (c + 1 < capacity) notFull.signal();
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
    return true;
  }

  /**
   * Inserts the specified element at the tail of this queue if it is
   * possible to do so immediately without exceeding the queue's capacity,
   * returning {@code true} upon success and {@code false} if this queue
   * is full.
   * When using a capacity-restricted queue, this method is generally
   * preferable to method {@link BlockingQueue#add add}, which can fail to
   * insert an element only by throwing an exception.
   *
   * @throws NullPointerException if the specified element is null
   */
  public boolean offer(E e) {
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    if (count.get() == capacity) return false;
    final int c;
    final Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
      if (count.get() == capacity) return false;
      enqueue(node);
      c = count.getAndIncrement();
      if (c + 1 < capacity) notFull.signal();
    } finally {
      putLock.unlock();
    }
    if (c == 0) signalNotEmpty();
    return true;
  }

  public E take() throws InterruptedException {
    final E x;
    final int c;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
      while (count.get() == 0) {
        notEmpty.await();
      }
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    final E x;
    final int c;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
      while (count.get() == 0) {
        if (nanos <= 0L) return null;
        nanos = notEmpty.awaitNanos(nanos);
      }
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  public E poll() {
    final AtomicInteger count = this.count;
    if (count.get() == 0) return null;
    final E x;
    final int c;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      if (count.get() == 0) return null;
      x = dequeue();
      c = count.getAndDecrement();
      if (c > 1) notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
    if (c == capacity) signalNotFull();
    return x;
  }

  public E peek() {
    final AtomicInteger count = this.count;
    if (count.get() == 0) return null;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      return (count.get() > 0) ? head.next.item : null;
    } finally {
      takeLock.unlock();
    }
  }

  /**
   * Unlinks interior Node p with predecessor pred.
   */
  void unlink(Node<E> p, Node<E> pred) {
    // assert putLock.isHeldByCurrentThread();
    // assert takeLock.isHeldByCurrentThread();
    // p.next is not changed, to allow iterators that are
    // traversing p to maintain their weak-consistency guarantee.
    p.item = null;
    pred.next = p.next;
    if (last == p) last = pred;
    if (count.getAndDecrement() == capacity) notFull.signal();
  }

  /**
   * Removes a single instance of the specified element from this queue,
   * if it is present.  More formally, removes an element {@code e} such
   * that {@code o.equals(e)}, if this queue contains one or more such
   * elements.
   * Returns {@code true} if this queue contained the specified element
   * (or equivalently, if this queue changed as a result of the call).
   *
   * @param o element to be removed from this queue, if present
   * @return {@code true} if this queue changed as a result of the call
   */
  public boolean remove(Object o) {
    if (o == null) return false;
    fullyLock();
    try {
      for (Node<E> pred = head, p = pred.next; p != null; pred = p, p = p.next) {
        if (o.equals(p.item)) {
          unlink(p, pred);
          return true;
        }
      }
      return false;
    } finally {
      fullyUnlock();
    }
  }

  /**
   * Returns {@code true} if this queue contains the specified element.
   * More formally, returns {@code true} if and only if this queue contains
   * at least one element {@code e} such that {@code o.equals(e)}.
   *
   * @param o object to be checked for containment in this queue
   * @return {@code true} if this queue contains the specified element
   */
  public boolean contains(Object o) {
    if (o == null) return false;
    fullyLock();
    try {
      for (Node<E> p = head.next; p != null; p = p.next) if (o.equals(p.item)) return true;
      return false;
    } finally {
      fullyUnlock();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue, in
   * proper sequence.
   *
   * <p>The returned array will be "safe" in that no references to it are
   * maintained by this queue.  (In other words, this method must allocate
   * a new array).  The caller is thus free to modify the returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based
   * APIs.
   *
   * @return an array containing all of the elements in this queue
   */
  public Object[] toArray() {
    fullyLock();
    try {
      int size = count.get();
      Object[] a = new Object[size];
      int k = 0;
      for (Node<E> p = head.next; p != null; p = p.next) a[k++] = p.item;
      return a;
    } finally {
      fullyUnlock();
    }
  }

  /**
   * Returns an array containing all of the elements in this queue, in
   * proper sequence; the runtime type of the returned array is that of
   * the specified array.  If the queue fits in the specified array, it
   * is returned therein.  Otherwise, a new array is allocated with the
   * runtime type of the specified array and the size of this queue.
   *
   * <p>If this queue fits in the specified array with room to spare
   * (i.e., the array has more elements than this queue), the element in
   * the array immediately following the end of the queue is set to
   * {@code null}.
   *
   * <p>Like the {@link #toArray()} method, this method acts as bridge between
   * array-based and collection-based APIs.  Further, this method allows
   * precise control over the runtime type of the output array, and may,
   * under certain circumstances, be used to save allocation costs.
   *
   * <p>Suppose {@code x} is a queue known to contain only strings.
   * The following code can be used to dump the queue into a newly
   * allocated array of {@code String}:
   *
   * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
   *
   * Note that {@code toArray(new Object[0])} is identical in function to
   * {@code toArray()}.
   *
   * @param a the array into which the elements of the queue are to
   *          be stored, if it is big enough; otherwise, a new array of the
   *          same runtime type is allocated for this purpose
   * @return an array containing all of the elements in this queue
   * @throws ArrayStoreException if the runtime type of the specified array
   *         is not a supertype of the runtime type of every element in
   *         this queue
   * @throws NullPointerException if the specified array is null
   */
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    fullyLock();
    try {
      int size = count.get();
      if (a.length < size)
        a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

      int k = 0;
      for (Node<E> p = head.next; p != null; p = p.next) a[k++] = (T) p.item;
      if (a.length > k) a[k] = null;
      return a;
    } finally {
      fullyUnlock();
    }
  }

  /**
   * Atomically removes all of the elements from this queue.
   * The queue will be empty after this call returns.
   */
  public void clear() {
    fullyLock();
    try {
      for (Node<E> p, h = head; (p = h.next) != null; h = p) {
        h.next = h;
        p.item = null;
      }
      head = last;
      // assert head.item == null && head.next == null;
      if (count.getAndSet(0) == capacity) notFull.signal();
    } finally {
      fullyUnlock();
    }
  }

  /**
   * @throws UnsupportedOperationException {@inheritDoc}
   * @throws ClassCastException            {@inheritDoc}
   * @throws NullPointerException          {@inheritDoc}
   * @throws IllegalArgumentException      {@inheritDoc}
   */
  public int drainTo(Collection<? super E> c) {
    return drainTo(c, Integer.MAX_VALUE);
  }

  /**
   * @throws UnsupportedOperationException {@inheritDoc}
   * @throws ClassCastException            {@inheritDoc}
   * @throws NullPointerException          {@inheritDoc}
   * @throws IllegalArgumentException      {@inheritDoc}
   */
  public int drainTo(Collection<? super E> c, int maxElements) {
    Objects.requireNonNull(c);
    if (c == this) throw new IllegalArgumentException();
    if (maxElements <= 0) return 0;
    boolean signalNotFull = false;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      int n = Math.min(maxElements, count.get());
      // count.get provides visibility to first n Nodes
      Node<E> h = head;
      int i = 0;
      try {
        while (i < n) {
          Node<E> p = h.next;
          c.add(p.item);
          p.item = null;
          h.next = h;
          h = p;
          ++i;
        }
        return n;
      } finally {
        // Restore invariants even if c.add() threw
        if (i > 0) {
          // assert h.item == null;
          head = h;
          signalNotFull = (count.getAndAdd(-i) == capacity);
        }
      }
    } finally {
      takeLock.unlock();
      if (signalNotFull) signalNotFull();
    }
  }

  /**
   * Used for any element traversal that is not entirely under lock.
   * Such traversals must handle both:
   * - dequeued nodes (p.next == p)
   * - (possibly multiple) interior removed nodes (p.item == null)
   */
  Node<E> succ(Node<E> p) {
    if (p == (p = p.next)) p = head.next;
    return p;
  }

  /**
   * Returns an iterator over the elements in this queue in proper sequence.
   * The elements will be returned in order from first (head) to last (tail).
   *
   * <p>The returned iterator is
   * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
   *
   * @return an iterator over the elements in this queue in proper sequence
   */
  public Iterator<E> iterator() {
    return new Itr();
  }

  /**
   * Weakly-consistent iterator.
   *
   * Lazily updated ancestor field provides expected O(1) remove(),
   * but still O(n) in the worst case, whenever the saved ancestor
   * is concurrently deleted.
   */
  private class Itr implements Iterator<E> {
    private Node<E> next; // Node holding nextItem
    private E nextItem; // next item to hand out
    private Node<E> lastRet;
    private Node<E> ancestor; // Helps unlink lastRet on remove()

    Itr() {
      fullyLock();
      try {
        if ((next = head.next) != null) nextItem = next.item;
      } finally {
        fullyUnlock();
      }
    }

    public boolean hasNext() {
      return next != null;
    }

    public E next() {
      Node<E> p;
      if ((p = next) == null) throw new NoSuchElementException();
      lastRet = p;
      E x = nextItem;
      fullyLock();
      try {
        E e = null;
        for (p = p.next; p != null && (e = p.item) == null; ) p = succ(p);
        next = p;
        nextItem = e;
      } finally {
        fullyUnlock();
      }
      return x;
    }

    public void forEachRemaining(Consumer<? super E> action) {
      // A variant of forEachFrom
      Objects.requireNonNull(action);
      Node<E> p;
      if ((p = next) == null) return;
      lastRet = p;
      next = null;
      final int batchSize = 64;
      Object[] es = null;
      int n, len = 1;
      do {
        fullyLock();
        try {
          if (es == null) {
            p = p.next;
            for (Node<E> q = p; q != null; q = succ(q))
              if (q.item != null && ++len == batchSize) break;
            es = new Object[len];
            es[0] = nextItem;
            nextItem = null;
            n = 1;
          } else n = 0;
          for (; p != null && n < len; p = succ(p))
            if ((es[n] = p.item) != null) {
              lastRet = p;
              n++;
            }
        } finally {
          fullyUnlock();
        }
        for (int i = 0; i < n; i++) {
          @SuppressWarnings("unchecked")
          E e = (E) es[i];
          action.accept(e);
        }
      } while (n > 0 && p != null);
    }

    public void remove() {
      Node<E> p = lastRet;
      if (p == null) throw new IllegalStateException();
      lastRet = null;
      fullyLock();
      try {
        if (p.item != null) {
          if (ancestor == null) ancestor = head;
          ancestor = findPred(p, ancestor);
          unlink(p, ancestor);
        }
      } finally {
        fullyUnlock();
      }
    }
  }

  public OrderIterator orderIterator() {
    return orderIterator(x -> true);
  }

  public OrderIterator orderIterator(Predicate<E> belongFilter) {
    return new OrderIterator(belongFilter);
  }

  public final class OrderIterator implements Iterator<E> {
    private Predicate<E> belongFilter;
    private Node<E> pointer;

    OrderIterator(Predicate<E> belongFilter) {
      this.belongFilter = belongFilter;
      fullyLock();
      try {
        this.pointer = head;
        this.pointer.orderIteratorCount.incrementAndGet();
      } finally {
        fullyUnlock();
      }
    }

    @Override
    public E next() {
      Node<E> p = this.pointer;
      if (p.next == null) {
        throw new NoSuchElementException();
      }
      fullyLock();
      E e = null;
      try {
        for (p = p.next; p != null && ((e = p.item) == null || !this.belongFilter.test(p.item)); )
          p = succ(p);
        if (p != null) {
          this.pointer.orderIteratorCount.decrementAndGet();
          this.pointer = p;
          this.pointer.orderIteratorCount.incrementAndGet();
          e = this.pointer.item;
        } else {
          e = null;
        }
      } finally {
        fullyUnlock();
      }
      return e;
    }

    public E next(Duration timeout) throws InterruptedException {
      long nanos = timeout.toNanos();
      final ReentrantLock localPutLock = putLock;
      localPutLock.lockInterruptibly();
      try {
        Node<E> p = this.pointer;
        while (p.next == null) {
          if (nanos <= 0L) return null;
          nanos = newElement.awaitNanos(nanos);
          p = this.pointer;
        }
        return next();
      } finally {
        localPutLock.unlock();
      }
    }

    @Override
    public boolean hasNext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * A customized variant of Spliterators.IteratorSpliterator.
   * Keep this class in sync with (very similar) LBDSpliterator.
   */
  private final class LBQSpliterator implements Spliterator<E> {
    static final int MAX_BATCH = 1 << 25; // max batch array size;
    Node<E> current; // current node; null until initialized
    int batch; // batch size for splits
    boolean exhausted; // true when no more nodes
    long est = size(); // size estimate

    LBQSpliterator() {}

    public long estimateSize() {
      return est;
    }

    public Spliterator<E> trySplit() {
      Node<E> h;
      if (!exhausted && ((h = current) != null || (h = head.next) != null) && h.next != null) {
        int n = batch = Math.min(batch + 1, MAX_BATCH);
        Object[] a = new Object[n];
        int i = 0;
        Node<E> p = current;
        fullyLock();
        try {
          if (p != null || (p = head.next) != null)
            for (; p != null && i < n; p = succ(p)) if ((a[i] = p.item) != null) i++;
        } finally {
          fullyUnlock();
        }
        if ((current = p) == null) {
          est = 0L;
          exhausted = true;
        } else if ((est -= i) < 0L) est = 0L;
        if (i > 0)
          return Spliterators.spliterator(
              a, 0, i, (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT));
      }
      return null;
    }

    public boolean tryAdvance(Consumer<? super E> action) {
      Objects.requireNonNull(action);
      if (!exhausted) {
        E e = null;
        fullyLock();
        try {
          Node<E> p;
          if ((p = current) != null || (p = head.next) != null)
            do {
              e = p.item;
              p = succ(p);
            } while (e == null && p != null);
          if ((current = p) == null) exhausted = true;
        } finally {
          fullyUnlock();
        }
        if (e != null) {
          action.accept(e);
          return true;
        }
      }
      return false;
    }

    public void forEachRemaining(Consumer<? super E> action) {
      Objects.requireNonNull(action);
      if (!exhausted) {
        exhausted = true;
        Node<E> p = current;
        current = null;
        forEachFrom(action, p, x -> false);
      }
    }

    public int characteristics() {
      return (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }
  }

  /**
   * Returns a {@link Spliterator} over the elements in this queue.
   *
   * <p>The returned spliterator is
   * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
   *
   * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
   * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
   *
   * @implNote
   * The {@code Spliterator} implements {@code trySplit} to permit limited
   * parallelism.
   *
   * @return a {@code Spliterator} over the elements in this queue
   * @since 1.8
   */
  public Spliterator<E> spliterator() {
    return new LBQSpliterator();
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   */
  public void forEach(Consumer<? super E> action) {
    Objects.requireNonNull(action);
    forEachFrom(action, null, x -> false);
  }

  public void forEach(Consumer<? super E> action, Predicate<? super E> stop) {
    Objects.requireNonNull(action);
    forEachFrom(action, null, stop);
  }

  /**
   * Runs action on each element found during a traversal starting at p.
   * If p is null, traversal starts at head.
   */
  void forEachFrom(Consumer<? super E> action, Node<E> p, Predicate<? super E> stop) {
    // Extract batches of elements while holding the lock; then
    // run the action on the elements while not
    final int batchSize = 64; // max number of elements per batch
    Object[] es = null; // container for batch of elements
    int n, len = 0;
    do {
      fullyLock();
      try {
        if (es == null) {
          if (p == null) p = head.next;
          for (Node<E> q = p; q != null; q = succ(q))
            if (q.item != null && ++len == batchSize) break;
          es = new Object[len];
        }
        for (n = 0; p != null && n < len; p = succ(p)) if ((es[n] = p.item) != null) n++;
      } finally {
        fullyUnlock();
      }
      for (int i = 0; i < n; i++) {
        @SuppressWarnings("unchecked")
        E e = (E) es[i];
        action.accept(e);
        if (stop.test(e)) {
          return;
        }
      }
    } while (n > 0 && p != null);
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean removeIf(Predicate<? super E> filter) {
    Objects.requireNonNull(filter);
    return bulkRemove(filter);
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean removeAll(Collection<?> c) {
    Objects.requireNonNull(c);
    return bulkRemove(e -> c.contains(e));
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean retainAll(Collection<?> c) {
    Objects.requireNonNull(c);
    return bulkRemove(e -> !c.contains(e));
  }

  /**
   * Returns the predecessor of live node p, given a node that was
   * once a live ancestor of p (or head); allows unlinking of p.
   */
  Node<E> findPred(Node<E> p, Node<E> ancestor) {
    // assert p.item != null;
    if (ancestor.item == null) ancestor = head;
    // Fails with NPE if precondition not satisfied
    for (Node<E> q; (q = ancestor.next) != p; ) ancestor = q;
    return ancestor;
  }

  /** Implementation of bulk remove methods. */
  @SuppressWarnings("unchecked")
  private boolean bulkRemove(Predicate<? super E> filter) {
    boolean removed = false;
    Node<E> p = null, ancestor = head;
    Node<E>[] nodes = null;
    int n, len = 0;
    do {
      // 1. Extract batch of up to 64 elements while holding the lock.
      fullyLock();
      try {
        if (nodes == null) { // first batch; initialize
          p = head.next;
          for (Node<E> q = p; q != null; q = succ(q)) if (q.item != null && ++len == 64) break;
          nodes = (Node<E>[]) new Node<?>[len];
        }
        for (n = 0; p != null && n < len; p = succ(p)) nodes[n++] = p;
      } finally {
        fullyUnlock();
      }

      // 2. Run the filter on the elements while lock is free.
      long deathRow = 0L; // "bitset" of size 64
      for (int i = 0; i < n; i++) {
        final E e;
        if ((e = nodes[i].item) != null && filter.test(e)) deathRow |= 1L << i;
      }

      // 3. Remove any filtered elements while holding the lock.
      if (deathRow != 0) {
        fullyLock();
        try {
          for (int i = 0; i < n; i++) {
            final Node<E> q;
            if ((deathRow & (1L << i)) != 0L && (q = nodes[i]).item != null) {
              ancestor = findPred(q, ancestor);
              unlink(q, ancestor);
              removed = true;
            }
            nodes[i] = null; // help GC
          }
        } finally {
          fullyUnlock();
        }
      }
    } while (n > 0 && p != null);
    return removed;
  }

  /**
   * Saves this queue to a stream (that is, serializes it).
   *
   * @param s the stream
   * @throws java.io.IOException if an I/O error occurs
   * @serialData The capacity is emitted (int), followed by all of
   * its elements (each an {@code Object}) in the proper order,
   * followed by a null
   */
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {

    fullyLock();
    try {
      // Write out any hidden stuff, plus capacity
      s.defaultWriteObject();

      // Write out all elements in the proper order.
      for (Node<E> p = head.next; p != null; p = p.next) s.writeObject(p.item);

      // Use trailing null as sentinel
      s.writeObject(null);
    } finally {
      fullyUnlock();
    }
  }

  /**
   * Reconstitutes this queue from a stream (that is, deserializes it).
   * @param s the stream
   * @throws ClassNotFoundException if the class of a serialized object
   *         could not be found
   * @throws java.io.IOException if an I/O error occurs
   */
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    // Read in capacity, and any hidden stuff
    s.defaultReadObject();

    count.set(0);
    last = head = new Node<E>(null);

    // Read in all elements and place in queue
    for (; ; ) {
      @SuppressWarnings("unchecked")
      E item = (E) s.readObject();
      if (item == null) break;
      add(item);
    }
  }

  public boolean add(E e) {
    if (offer(e)) return true;
    else throw new IllegalStateException("Queue full");
  }

  public void purge(PurgeFunction<E> purger) {
    fullyLock();
    int c = count.get();
    try {
      Node<E> pred = null;
      Node<E> p = this.head;
      while (p.next != null) {
        try {
          if (p.orderIteratorCount.get() > 0) {
            return;
          }
          if (purger.isPurgeable(p.item)) {
            if (pred == null) {
              p = p.next;
              dequeue();
              c = count.getAndDecrement();
            } else {
              unlink(p, pred);
            }
          } else {
            pred = p;
            p = p.next;
          }
        } catch (TimeoutException e) {
          break;
        }
      }
      if (c > 1) notEmpty.signal();
    } finally {
      fullyUnlock();
    }
    if (c == capacity) signalNotFull();
  }

  public void purge(final PurgeFunction<E> purger, final Duration maxInvestTime) {
    final long start = System.currentTimeMillis();
    purge(
        e -> {
          if (System.currentTimeMillis() - start > maxInvestTime.toMillis()) {
            throw new TimeoutException();
          }
          return purger.isPurgeable(e);
        });
  }
}
