package io.github.vivek0509.besu.revertdebugger.capture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Bounded FIFO store of {@link RevertRecord} entries with thread-safe access.
 *
 * <p>Capacity is fixed at construction. Once full, {@link #add(RevertRecord)} evicts the oldest
 * record before inserting the new one. All public methods are {@code synchronized}; this is the
 * simplest correct implementation given the actual write rate (the tracer adds at most a few
 * hundred records per minute) and read rate (RPC reads at a handful per minute). A {@link
 * java.util.concurrent.locks.ReadWriteLock} or hash-index for O(1) lookup is not justified at these
 * rates and would expand the concurrency surface.
 *
 * <p>{@link #recent(int)} returns a freshly allocated {@link List}; {@link #findByTxHash(String)}
 * returns an {@link Optional} wrapping a stored {@link RevertRecord}. Both are safe to read after
 * the lock is released because {@code RevertRecord} is an immutable Java record, so the reference
 * cannot observe concurrent modification.
 */
public class RingBuffer {

  private final int capacity;
  private final ArrayDeque<RevertRecord> records;

  public RingBuffer(final int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive, got " + capacity);
    }
    this.capacity = capacity;
    this.records = new ArrayDeque<>(capacity);
  }

  /**
   * Adds the record unless one with the same {@code txHash} is already present. Returns {@code
   * true} if the record was added, {@code false} if dropped as a duplicate. Some Besu consensus
   * paths invoke the per-block tracer's end-of-tx hook more than once per transaction, sometimes
   * across separate tracer instances; deduping at the buffer is the only way to catch the
   * cross-instance case.
   */
  public synchronized boolean add(final RevertRecord record) {
    for (final RevertRecord existing : records) {
      if (existing.txHash().equals(record.txHash())) {
        return false;
      }
    }
    if (records.size() >= capacity) {
      records.removeFirst();
    }
    records.addLast(record);
    return true;
  }

  public synchronized int size() {
    return records.size();
  }

  /**
   * Newest-first match for a transaction hash. Walks from the tail so recent reverts (the common
   * operator query) return after a few iterations rather than the full {@link #size()} worst case.
   */
  public synchronized Optional<RevertRecord> findByTxHash(final String txHash) {
    final Iterator<RevertRecord> it = records.descendingIterator();
    while (it.hasNext()) {
      final RevertRecord r = it.next();
      if (r.txHash().equals(txHash)) {
        return Optional.of(r);
      }
    }
    return Optional.empty();
  }

  /**
   * Up to {@code limit} records, newest first. {@code limit} is clamped into {@code [0, size()]};
   * negative values produce an empty list.
   */
  public synchronized List<RevertRecord> recent(final int limit) {
    final int n = Math.min(Math.max(0, limit), records.size());
    final List<RevertRecord> out = new ArrayList<>(n);
    final Iterator<RevertRecord> it = records.descendingIterator();
    while (it.hasNext() && out.size() < n) {
      out.add(it.next());
    }
    return out;
  }
}
