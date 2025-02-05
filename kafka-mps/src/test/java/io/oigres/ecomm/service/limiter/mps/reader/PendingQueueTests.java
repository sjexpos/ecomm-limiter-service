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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class PendingQueueTests {

  @BeforeAll
  static void setup() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    log.info("----------------------- JVM memory ----------------------");
    log.info(
        "Max Heap size: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getMax()));
    log.info(
        "Initial Heap size: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getInit()));
    log.info(
        "Heap usage: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getUsed()));
    log.info("---------------------------------------------------------");
  }

  @AfterAll
  static void endup() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    log.info("----------------------- JVM memory ----------------------");
    log.info(
        "Max Heap size: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getMax()));
    log.info(
        "Initial Heap size: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getInit()));
    log.info(
        "Heap usage: {}",
        FileUtils.byteCountToDisplaySize(memoryBean.getHeapMemoryUsage().getUsed()));
    log.info("---------------------------------------------------------");
  }

  @RequiredArgsConstructor
  public static class Task implements Runnable {
    private final PendingQueue<Integer>.OrderIterator iterator;
    private final CountDownLatch doneSignal;
    private final AtomicBoolean readerEndsUp;
    private final AtomicInteger errors;
    @Getter private final List<Integer> data = new LinkedList<>();

    @Override
    public void run() {
      while (true) {
        try {
          Integer d = this.iterator.next(Duration.ofMillis(250));
          if (d != null) {
            data.add(d);
            // log.info(s);
            try {
              Thread.sleep(Duration.ofNanos(50));
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          } else if (readerEndsUp.get()) {
            break;
          }
        } catch (InterruptedException e) {
          errors.incrementAndGet();
          break;
        } catch (Exception e) {
          errors.incrementAndGet();
          log.error("Error: ", e);
          break;
        }
      }
      doneSignal.countDown();
    }
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(5, 2000, 2000, 1),
        Arguments.of(0, 2000, 2000, 1),
        Arguments.of(5, 4000, 4000, 1),
        Arguments.of(0, 4000, 4000, 1),
        Arguments.of(5, 5000, 5000, 20),
        Arguments.of(0, 5000, 5000, 20),
        Arguments.of(5, 10000, 10000, 20),
        Arguments.of(0, 10000, 10000, 20),
        Arguments.of(5, 5000, 5000, 50),
        Arguments.of(0, 5000, 5000, 50),
        Arguments.of(5, 10000, 10000, 50),
        Arguments.of(0, 10000, 10000, 50),
        Arguments.of(5, 5000, 5000, 500),
        Arguments.of(0, 5000, 5000, 500),
        Arguments.of(5, 10000, 10000, 500),
        Arguments.of(0, 10000, 10000, 500),
        Arguments.of(0, 50000, 50000, 5000),
        Arguments.of(0, 100000, 50000, 5000),
        Arguments.of(0, 200000, 30000, 5000),
        Arguments.of(0, 300000, 200000, 5000),
        Arguments.of(0, 500000, 500000, 1000),
        Arguments.of(0, 500000, 500000, 2500),
        Arguments.of(0, 500000, 500000, 5000),
        Arguments.of(0, 500000, 500000, 10000),
        Arguments.of(0, 500000, 500000, 20000));
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void test_offer_multi_poll(
      int initialLoad, int maxFirstChunkSize, int maxSecondChunkSize, int maxWriters)
      throws InterruptedException {
    // given
    long start = System.currentTimeMillis();
    log.info(
        String.format(
            "Running %d writers with chunks of %d and %d elements, starting from %d elements",
            maxWriters, maxFirstChunkSize, maxSecondChunkSize, initialLoad));
    List<Task> writers = new LinkedList<>();
    final PendingQueue<Integer> queue = new PendingQueue<>();
    PendingQueue<Integer>.OrderIterator orderIterator =
        (PendingQueue<Integer>.OrderIterator) queue.orderIterator();

    AtomicBoolean readerEndsUp = new AtomicBoolean(false);
    AtomicInteger errors = new AtomicInteger(0);
    CountDownLatch doneSignal = new CountDownLatch(maxWriters);

    IntStream.rangeClosed(1, maxWriters)
        .forEach(i -> writers.add(new Task(orderIterator, doneSignal, readerEndsUp, errors)));

    // when
    IntStream.rangeClosed(1, initialLoad).forEach(queue::add);
    VirtualThreadExecutor executor = new VirtualThreadExecutor("writer");
    writers.forEach(executor::execute);
    Thread reader =
        new Thread(
            () -> {
              IntStream.rangeClosed(initialLoad + 1, maxFirstChunkSize).forEach(queue::offer);
              try {
                Thread.sleep(Duration.ofSeconds(1));
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
              IntStream.rangeClosed(maxFirstChunkSize + 1, maxFirstChunkSize + maxSecondChunkSize)
                  .forEach(queue::offer);
            });
    reader.start();
    reader.join();
    readerEndsUp.set(true);

    // then
    Assertions.assertTrue(doneSignal.await(30, TimeUnit.SECONDS), "All writers didn't finish!!");
    log.info("took: " + Duration.ofMillis(System.currentTimeMillis() - start));
    Assertions.assertFalse(reader.isAlive());
    Set<Integer> processedData = new HashSet<>();
    AtomicInteger writerWithLessThanTen = new AtomicInteger();
    AtomicInteger writerWithUnsorted = new AtomicInteger();
    writers.forEach(
        t -> {
          if (t.getData().size() <= 10) {
            writerWithLessThanTen.getAndIncrement();
          }
          if (!t.getData().stream().sorted().toList().equals(t.getData())) {
            writerWithUnsorted.getAndIncrement();
          }
          processedData.addAll(t.getData());
        });
    if (writerWithLessThanTen.get() > 0) {
      log.warn(writerWithLessThanTen + " writers processed less than 10 elements");
    }
    Assertions.assertEquals(0, writerWithUnsorted.get());
    Assertions.assertEquals(maxFirstChunkSize + maxSecondChunkSize, processedData.size());
    Assertions.assertEquals(queue.size(), processedData.size());
    Assertions.assertEquals(0, errors.get());
  }

  @Test
  void test_order_iterator_with_filter() throws InterruptedException {
    // given
    final PendingQueue<Integer> queue = new PendingQueue<>();
    IntStream.rangeClosed(1, 200).forEach(queue::add);
    final List<Integer> validElements = List.of(1, 3, 5, 7, 9, 34, 89);
    Predicate<Integer> filter = validElements::contains;
    PendingQueue<Integer>.OrderIterator orderIterator =
        (PendingQueue<Integer>.OrderIterator) queue.orderIterator(filter);

    // when
    Integer i = -15;
    List<Integer> gottenElements = new LinkedList<>();
    while (i != null) {
      i = orderIterator.next(Duration.ofNanos(1));
      if (i != null) {
        gottenElements.add(i);
      }
    }

    // then
    Assertions.assertArrayEquals(
        validElements.toArray(new Integer[0]), gottenElements.toArray(new Integer[0]));
    Assertions.assertEquals(200, queue.size());
  }

  @Test
  void test_purge_without_order_iterator() {
    // given
    final PendingQueue<Integer> queue = new PendingQueue<>(5000);
    IntStream.rangeClosed(1, 200).forEach(queue::add);
    Assertions.assertEquals(200, queue.size());
    Assertions.assertEquals(4800, queue.remainingCapacity());

    // when
    queue.purge(i -> true);

    // then
    Assertions.assertEquals(0, queue.size());
    Assertions.assertEquals(5000, queue.remainingCapacity());
  }

  @Test
  void test_purge_with_order_iterator_01() {
    // given
    final PendingQueue<Integer> queue = new PendingQueue<>(5000);
    IntStream.rangeClosed(1, 200).forEach(queue::add);
    Assertions.assertEquals(200, queue.size());
    Assertions.assertEquals(4800, queue.remainingCapacity());
    PendingQueue<Integer>.OrderIterator orderIterator =
        (PendingQueue<Integer>.OrderIterator) queue.orderIterator();

    // when
    queue.purge(i -> true);

    // then
    Assertions.assertEquals(200, queue.size());
    Assertions.assertEquals(4800, queue.remainingCapacity());
  }

  @Test
  void test_purge_with_order_iterator_02() {
    // given
    final PendingQueue<Integer> queue = new PendingQueue<>(5000);
    IntStream.rangeClosed(1, 200).forEach(queue::add);
    Assertions.assertEquals(200, queue.size());
    Assertions.assertEquals(4800, queue.remainingCapacity());
    PendingQueue<Integer>.OrderIterator orderIterator =
        (PendingQueue<Integer>.OrderIterator) queue.orderIterator();

    // when
    orderIterator.next();
    orderIterator.next();
    orderIterator.next();
    queue.purge(i -> true);

    // then
    Assertions.assertEquals(197, queue.size());
    Assertions.assertEquals(4803, queue.remainingCapacity());

    // when
    PendingQueue<Integer>.OrderIterator orderIterator2 =
        (PendingQueue<Integer>.OrderIterator) queue.orderIterator();
    Assertions.assertEquals(4, orderIterator2.next());
    Assertions.assertEquals(197, queue.size());
    Assertions.assertEquals(4803, queue.remainingCapacity());
  }

  @Test
  void test_purge_with_timeout() {
    // given
    final PendingQueue<Integer> queue = new PendingQueue<>(5000);
    IntStream.rangeClosed(1, 2000).forEach(queue::add);

    // when
    queue.purge(
        i -> {
          try {
            Thread.sleep(Duration.ofMillis(5));
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          return true;
        },
        Duration.ofMillis(50));
    // when

    Assertions.assertEquals(1990, queue.size());
  }
}
