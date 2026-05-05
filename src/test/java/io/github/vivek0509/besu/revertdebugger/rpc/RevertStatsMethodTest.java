package io.github.vivek0509.besu.revertdebugger.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;
import io.github.vivek0509.besu.revertdebugger.rpc.RevertStatsMethod.Stats;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

class RevertStatsMethodTest {

  private static final long NOW = 1_000_000L;
  private static final LongSupplier FIXED_CLOCK = () -> NOW;

  private static final String CONTRACT_A = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String CONTRACT_B = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  private static RevertRecord record(
      final String txHash,
      final String contract,
      final RevertReasonFormat format,
      final long timestamp) {
    return new RevertRecord(
        txHash,
        1L,
        "0xblock",
        contract,
        "0xfrom",
        "0x12345678",
        "0x12345678",
        format,
        format == RevertReasonFormat.UNKNOWN ? null : "decoded",
        "0x",
        21_000L,
        0,
        timestamp);
  }

  private static PluginRpcRequest requestWithParams(final Object... params) {
    final PluginRpcRequest request = mock(PluginRpcRequest.class);
    when(request.getParams()).thenReturn(params);
    return request;
  }

  private static RingBuffer bufferOf(final RevertRecord... records) {
    final RingBuffer buffer = new RingBuffer(Math.max(records.length, 1));
    for (final RevertRecord r : records) {
      buffer.add(r);
    }
    return buffer;
  }

  @Test
  void emptyBufferReturnsAllZerosAndEmptyMaps() {
    final Stats stats =
        new RevertStatsMethod(() -> new RingBuffer(10), FIXED_CLOCK)
            .execute(requestWithParams("60"));

    assertThat(stats.total()).isZero();
    assertThat(stats.perContract()).isEmpty();
    assertThat(stats.perReasonFormat()).isEmpty();
  }

  @Test
  void allRecordsInWindowAreCounted() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 30),
            record("0x2", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 20),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("60"));

    assertThat(stats.total()).isEqualTo(3);
  }

  @Test
  void recordsOutsideWindowAreExcluded() {
    final RingBuffer buffer =
        bufferOf(
            record("0x_old", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 1000),
            record("0x_in", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 30));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("60"));

    assertThat(stats.total()).isEqualTo(1);
    assertThat(stats.perContract()).containsExactly(java.util.Map.entry(CONTRACT_A, 1L));
  }

  @Test
  void perContractAggregatesAcrossContracts() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 5),
            record("0x2", CONTRACT_B, RevertReasonFormat.ERROR_STRING, NOW - 5),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 5));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("60"));

    assertThat(stats.perContract()).containsEntry(CONTRACT_A, 2L).containsEntry(CONTRACT_B, 1L);
  }

  @Test
  void perReasonFormatUsesDisplayNames() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW),
            record("0x2", CONTRACT_A, RevertReasonFormat.PANIC_UINT256, NOW),
            record("0x3", CONTRACT_A, RevertReasonFormat.UNKNOWN, NOW));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("60"));

    assertThat(stats.perReasonFormat())
        .containsEntry("Error(string)", 1L)
        .containsEntry("Panic(uint256)", 1L)
        .containsEntry("Unknown", 1L);
  }

  @Test
  void totalEqualsSumOfPerContract() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW),
            record("0x2", CONTRACT_B, RevertReasonFormat.ERROR_STRING, NOW),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("60"));

    final long sum = stats.perContract().values().stream().mapToLong(Long::longValue).sum();
    assertThat(stats.total()).isEqualTo(sum);
  }

  @Test
  void zeroWindowOnlyIncludesNowExactRecords() {
    final RingBuffer buffer =
        bufferOf(
            record("0x_now", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW),
            record("0x_pre", CONTRACT_A, RevertReasonFormat.ERROR_STRING, NOW - 1));

    final Stats stats =
        new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("0"));

    assertThat(stats.total()).isEqualTo(1);
  }

  @Test
  void throwsOnZeroParams() {
    final RingBuffer buffer = new RingBuffer(10);
    assertThatThrownBy(
            () -> new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsOnTooManyParams() {
    final RingBuffer buffer = new RingBuffer(10);
    assertThatThrownBy(
            () ->
                new RevertStatsMethod(() -> buffer, FIXED_CLOCK)
                    .execute(requestWithParams("60", "extra")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsOnNegativeWindow() {
    final RingBuffer buffer = new RingBuffer(10);
    assertThatThrownBy(
            () ->
                new RevertStatsMethod(() -> buffer, FIXED_CLOCK).execute(requestWithParams("-10")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
