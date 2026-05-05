package io.github.vivek0509.besu.revertdebugger.rpc;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Handler for {@code revert_stats}. Aggregates captured reverts whose timestamp falls within the
 * window {@code [now - windowSeconds, now]} into a {@link Stats} record. Map keys are contract
 * addresses for {@code perContract} and reason format display names for {@code perReasonFormat}.
 */
public class RevertStatsMethod {

  /** Aggregate result. Returned to the JSON-RPC client as a JSON object via Jackson. */
  public record Stats(
      Map<String, Long> perContract, Map<String, Long> perReasonFormat, long total) {}

  private final Supplier<RingBuffer> ringBufferSupplier;
  private final LongSupplier nowEpochSeconds;

  public RevertStatsMethod(final Supplier<RingBuffer> ringBufferSupplier) {
    this(ringBufferSupplier, () -> System.currentTimeMillis() / 1000L);
  }

  // Test-only: inject a deterministic clock.
  RevertStatsMethod(
      final Supplier<RingBuffer> ringBufferSupplier, final LongSupplier nowEpochSeconds) {
    this.ringBufferSupplier = ringBufferSupplier;
    this.nowEpochSeconds = nowEpochSeconds;
  }

  public Stats execute(final PluginRpcRequest request) {
    final Object[] params = request.getParams();
    if (params == null || params.length != 1 || params[0] == null) {
      throw new IllegalArgumentException(
          "revert_stats expects exactly one parameter: windowSeconds");
    }
    final long windowSeconds = Long.parseLong(params[0].toString());
    if (windowSeconds < 0) {
      throw new IllegalArgumentException("revert_stats: windowSeconds must be non-negative");
    }

    final long now = nowEpochSeconds.getAsLong();
    final long minTimestamp = now - windowSeconds;

    final RingBuffer buffer = ringBufferSupplier.get();
    final List<RevertRecord> snapshot = buffer.recent(buffer.size());

    final Map<String, Long> perContract = new HashMap<>();
    final Map<String, Long> perReasonFormat = new HashMap<>();
    long total = 0;
    for (final RevertRecord r : snapshot) {
      if (r.timestamp() < minTimestamp) {
        continue;
      }
      perContract.merge(r.contract(), 1L, Long::sum);
      perReasonFormat.merge(r.reasonFormat().displayName(), 1L, Long::sum);
      total++;
    }
    return new Stats(Map.copyOf(perContract), Map.copyOf(perReasonFormat), total);
  }
}
