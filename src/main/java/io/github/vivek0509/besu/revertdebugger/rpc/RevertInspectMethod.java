package io.github.vivek0509.besu.revertdebugger.rpc;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Handler for {@code revert_inspect}. Looks up a captured {@link RevertRecord} by transaction hash;
 * returns the record on hit, {@code null} on miss.
 *
 * <p>Holds a {@link Supplier} for the ring buffer rather than the buffer itself, because the buffer
 * is constructed in the plugin's {@code start()} but the {@code RpcEndpointService} contract
 * requires endpoint registration during {@code register()}. The supplier reads the plugin's field
 * at call time, by which point {@code start()} has run and the field is populated.
 */
public class RevertInspectMethod {

  private final Supplier<RingBuffer> ringBufferSupplier;

  public RevertInspectMethod(final Supplier<RingBuffer> ringBufferSupplier) {
    this.ringBufferSupplier = ringBufferSupplier;
  }

  public RevertRecord execute(final PluginRpcRequest request) {
    final Object[] params = request.getParams();
    if (params == null || params.length != 1) {
      throw new IllegalArgumentException("revert_inspect expects exactly one parameter: txHash");
    }
    if (params[0] == null) {
      throw new IllegalArgumentException("revert_inspect: txHash cannot be null");
    }
    final String txHash = params[0].toString().toLowerCase(Locale.ROOT);
    return ringBufferSupplier.get().findByTxHash(txHash).orElse(null);
  }
}
