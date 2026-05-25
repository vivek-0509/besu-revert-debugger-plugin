package io.github.vivek0509.besu.revertdebugger.rpc;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Handler for {@code revert_recent}. Returns up to {@code limit} records newest-first, optionally
 * filtered by contract and reason format.
 */
public class RevertRecentMethod {

  private final Supplier<RingBuffer> ringBufferSupplier;

  public RevertRecentMethod(final Supplier<RingBuffer> ringBufferSupplier) {
    this.ringBufferSupplier = ringBufferSupplier;
  }

  public List<RevertRecord> execute(final PluginRpcRequest request) {
    final Object[] params = request.getParams();
    if (params == null || params.length < 1 || params.length > 3) {
      throw new IllegalArgumentException(
          "revert_recent expects 1 to 3 parameters: limit, optional contractFilter,"
              + " optional reasonFilter");
    }
    if (params[0] == null) {
      throw new IllegalArgumentException("revert_recent: limit cannot be null");
    }

    final int limit = Integer.parseInt(params[0].toString());
    final String contractFilter = optional(params, 1);
    final String reasonFilterStr = optional(params, 2);
    final RevertReasonFormat reasonFilter =
        reasonFilterStr == null ? null : RevertReasonFormat.fromDisplayName(reasonFilterStr);

    final List<RevertRecord> recent = ringBufferSupplier.get().recent(limit);

    if (contractFilter == null && reasonFilter == null) {
      return recent;
    }

    final String contractKey =
        contractFilter == null ? null : contractFilter.toLowerCase(Locale.ROOT);

    return recent.stream()
        .filter(r -> contractKey == null || r.contract().equals(contractKey))
        .filter(r -> reasonFilter == null || r.reasonFormat() == reasonFilter)
        .toList();
  }

  private static String optional(final Object[] params, final int index) {
    if (index >= params.length || params[index] == null) {
      return null;
    }
    final String s = params[index].toString();
    return s.isEmpty() ? null : s;
  }
}
