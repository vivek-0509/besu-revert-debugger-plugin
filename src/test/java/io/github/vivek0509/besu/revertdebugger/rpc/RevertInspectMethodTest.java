package io.github.vivek0509.besu.revertdebugger.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import org.junit.jupiter.api.Test;

class RevertInspectMethodTest {

  private static final String TX_HASH =
      "0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef";

  private static RevertRecord recordWithHash(final String txHash) {
    return new RevertRecord(
        txHash,
        1L,
        "0xblock",
        "0xcontract",
        "0xfrom",
        "0x12345678",
        "0x12345678",
        RevertReasonFormat.ERROR_STRING,
        "Insufficient balance",
        "0x08c379a0",
        21_000L,
        0,
        1714670400L);
  }

  private static PluginRpcRequest requestWithParams(final Object... params) {
    final PluginRpcRequest request = mock(PluginRpcRequest.class);
    when(request.getParams()).thenReturn(params);
    return request;
  }

  @Test
  void returnsRecordWhenFound() {
    final RingBuffer buffer = new RingBuffer(10);
    buffer.add(recordWithHash(TX_HASH));
    final RevertInspectMethod method = new RevertInspectMethod(() -> buffer);

    final RevertRecord result = method.execute(requestWithParams(TX_HASH));

    assertThat(result).isNotNull();
    assertThat(result.txHash()).isEqualTo(TX_HASH);
  }

  @Test
  void returnsNullWhenNotFound() {
    final RingBuffer buffer = new RingBuffer(10);
    final RevertInspectMethod method = new RevertInspectMethod(() -> buffer);

    assertThat(method.execute(requestWithParams("0xnotinbuffer"))).isNull();
  }

  @Test
  void throwsOnZeroParams() {
    final RevertInspectMethod method = new RevertInspectMethod(() -> new RingBuffer(10));
    assertThatThrownBy(() -> method.execute(requestWithParams()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("txHash");
  }

  @Test
  void throwsOnTooManyParams() {
    final RevertInspectMethod method = new RevertInspectMethod(() -> new RingBuffer(10));
    assertThatThrownBy(() -> method.execute(requestWithParams(TX_HASH, "extra")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void txHashLookupIsCaseInsensitive() {
    final RingBuffer buffer = new RingBuffer(10);
    buffer.add(recordWithHash(TX_HASH));
    final RevertInspectMethod method = new RevertInspectMethod(() -> buffer);

    final RevertRecord result = method.execute(requestWithParams(TX_HASH.toUpperCase()));

    assertThat(result).isNotNull();
    assertThat(result.txHash()).isEqualTo(TX_HASH);
  }

  @Test
  void throwsOnNullFirstParam() {
    final RevertInspectMethod method = new RevertInspectMethod(() -> new RingBuffer(10));
    assertThatThrownBy(() -> method.execute(requestWithParams((Object) null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
  }
}
