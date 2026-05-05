package io.github.vivek0509.besu.revertdebugger.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RingBufferTest {

  private static RevertRecord recordWithHash(final String txHash) {
    return new RevertRecord(
        txHash,
        1L,
        "0xblock",
        "0xcontract",
        "0xfrom",
        "0x12345678",
        "0x",
        RevertReasonFormat.UNKNOWN,
        null,
        "0x",
        0L,
        0,
        0L);
  }

  @Test
  void constructorRejectsNonPositiveCapacity() {
    assertThatThrownBy(() -> new RingBuffer(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RingBuffer(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addStoresRecord() {
    final RingBuffer buffer = new RingBuffer(4);
    final boolean added = buffer.add(recordWithHash("0xa"));

    assertThat(added).isTrue();
    assertThat(buffer.size()).isEqualTo(1);
    assertThat(buffer.findByTxHash("0xa")).isPresent();
  }

  @Test
  void addReturnsFalseAndKeepsBufferUnchangedForDuplicateTxHash() {
    final RingBuffer buffer = new RingBuffer(4);
    final boolean firstAdd = buffer.add(recordWithHash("0xa"));
    final boolean secondAdd = buffer.add(recordWithHash("0xa"));

    assertThat(firstAdd).isTrue();
    assertThat(secondAdd).isFalse();
    assertThat(buffer.size()).isEqualTo(1);
  }

  @Test
  void addBeyondCapacityEvictsOldest() {
    final RingBuffer buffer = new RingBuffer(3);
    buffer.add(recordWithHash("0xa"));
    buffer.add(recordWithHash("0xb"));
    buffer.add(recordWithHash("0xc"));
    buffer.add(recordWithHash("0xd"));

    assertThat(buffer.size()).isEqualTo(3);
    assertThat(buffer.findByTxHash("0xa")).isEmpty();
    assertThat(buffer.findByTxHash("0xb")).isPresent();
    assertThat(buffer.findByTxHash("0xc")).isPresent();
    assertThat(buffer.findByTxHash("0xd")).isPresent();
  }

  @Test
  void findByTxHashReturnsEmptyForUnknownHash() {
    final RingBuffer buffer = new RingBuffer(4);
    assertThat(buffer.findByTxHash("0xa")).isEmpty();

    buffer.add(recordWithHash("0xb"));
    assertThat(buffer.findByTxHash("0xa")).isEmpty();
  }

  @Test
  void recentReturnsNewestFirst() {
    final RingBuffer buffer = new RingBuffer(5);
    buffer.add(recordWithHash("0x1"));
    buffer.add(recordWithHash("0x2"));
    buffer.add(recordWithHash("0x3"));

    final List<RevertRecord> recent = buffer.recent(3);
    assertThat(recent).hasSize(3);
    assertThat(recent.get(0).txHash()).isEqualTo("0x3");
    assertThat(recent.get(1).txHash()).isEqualTo("0x2");
    assertThat(recent.get(2).txHash()).isEqualTo("0x1");
  }

  @Test
  void recentClampsLimit() {
    final RingBuffer buffer = new RingBuffer(5);
    buffer.add(recordWithHash("0x1"));
    buffer.add(recordWithHash("0x2"));

    assertThat(buffer.recent(0)).isEmpty();
    assertThat(buffer.recent(-5)).isEmpty();
    assertThat(buffer.recent(99)).hasSize(2);
  }
}
