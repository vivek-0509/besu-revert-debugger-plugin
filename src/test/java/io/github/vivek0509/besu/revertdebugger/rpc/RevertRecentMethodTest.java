package io.github.vivek0509.besu.revertdebugger.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.List;

import org.junit.jupiter.api.Test;

class RevertRecentMethodTest {

  private static final String CONTRACT_A = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String CONTRACT_B = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

  private static RevertRecord record(
      final String txHash, final String contract, final RevertReasonFormat format) {
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
        0L);
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
  void limitTruncatesResults() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x4", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x5", CONTRACT_A, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("2"));

    assertThat(result).hasSize(2);
  }

  @Test
  void ordersNewestFirst() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("3"));

    assertThat(result).extracting(RevertRecord::txHash).containsExactly("0x3", "0x2", "0x1");
  }

  @Test
  void emptyContractFilterMeansNoFilter() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_B, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("5", "", null));

    assertThat(result).hasSize(2);
  }

  @Test
  void nullContractFilterMeansNoFilter() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_B, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("5", null));

    assertThat(result).hasSize(2);
  }

  @Test
  void contractFilterAppliesEqualityCheck() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_B, RevertReasonFormat.ERROR_STRING),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("5", CONTRACT_A));

    assertThat(result).extracting(RevertRecord::txHash).containsExactly("0x3", "0x1");
  }

  @Test
  void contractFilterIsCaseInsensitive() {
    final RingBuffer buffer = bufferOf(record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING));
    final String upper = CONTRACT_A.toUpperCase().replace("0X", "0x");

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("5", upper));

    assertThat(result).hasSize(1);
  }

  @Test
  void reasonFilterAppliesByDisplayName() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_A, RevertReasonFormat.PANIC_UINT256),
            record("0x3", CONTRACT_A, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("5", null, "Error(string)"));

    assertThat(result).extracting(RevertRecord::txHash).containsExactly("0x3", "0x1");
  }

  @Test
  void reasonFilterUnknownValueThrows() {
    final RingBuffer buffer = bufferOf(record("0x1", CONTRACT_A, RevertReasonFormat.UNKNOWN));

    assertThatThrownBy(
            () ->
                new RevertRecentMethod(() -> buffer)
                    .execute(requestWithParams("5", null, "banana")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bothFiltersCombinedNarrowsResults() {
    final RingBuffer buffer =
        bufferOf(
            record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING),
            record("0x2", CONTRACT_A, RevertReasonFormat.PANIC_UINT256),
            record("0x3", CONTRACT_B, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer)
            .execute(requestWithParams("5", CONTRACT_A, "Error(string)"));

    assertThat(result).extracting(RevertRecord::txHash).containsExactly("0x1");
  }

  @Test
  void limitOfZeroReturnsEmpty() {
    final RingBuffer buffer = bufferOf(record("0x1", CONTRACT_A, RevertReasonFormat.ERROR_STRING));

    final List<RevertRecord> result =
        new RevertRecentMethod(() -> buffer).execute(requestWithParams("0"));

    assertThat(result).isEmpty();
  }

  @Test
  void throwsOnZeroParams() {
    final RingBuffer buffer = bufferOf();

    assertThatThrownBy(() -> new RevertRecentMethod(() -> buffer).execute(requestWithParams()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsOnTooManyParams() {
    final RingBuffer buffer = bufferOf();

    assertThatThrownBy(
            () ->
                new RevertRecentMethod(() -> buffer)
                    .execute(requestWithParams("5", null, null, "extra")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
