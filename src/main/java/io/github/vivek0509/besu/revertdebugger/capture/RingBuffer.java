package io.github.vivek0509.besu.revertdebugger.capture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** Bounded thread-safe FIFO of {@link RevertRecord} entries. Evicts oldest on overflow. */
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

  /** Returns false if a record with the same {@code txHash} is already present. */
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

  /** Up to {@code limit} records, newest first. Negative or zero {@code limit} yields empty. */
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
