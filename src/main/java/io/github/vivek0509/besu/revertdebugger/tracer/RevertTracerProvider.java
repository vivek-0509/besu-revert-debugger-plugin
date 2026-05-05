package io.github.vivek0509.besu.revertdebugger.tracer;

import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;

import org.hyperledger.besu.plugin.data.BlockHeader;
import org.hyperledger.besu.plugin.services.BlockImportTracerProvider;
import org.hyperledger.besu.plugin.services.tracer.BlockAwareOperationTracer;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Factory for {@link RevertTracer} instances. Besu calls {@link #getBlockImportTracer(BlockHeader)}
 * once per imported block; we hand back either a fresh tracer (when {@code --plugin-revert-enabled}
 * is true) or {@link BlockAwareOperationTracer#NO_TRACING} (otherwise). Returning the no-op tracer
 * rather than registering nothing keeps Besu's tracing machinery happy in both states.
 *
 * <p>The contract allow-list lives here in an {@link AtomicReference}, not in the tracer, so commit
 * 14's hot-reload can swap it without rebuilding the provider. Each tracer holds a {@link
 * java.util.function.Supplier} that reads the AtomicReference at capture time, so a reload mid-
 * block is observed at the very next revert. Decision 14 in {@code decisions.md} covers the privacy
 * and performance reasoning behind the allow-list. Decision 15 covers why we register one provider
 * via {@code addService} despite the last-write-wins semantics that creates with other tracers.
 */
public class RevertTracerProvider implements BlockImportTracerProvider {

  private final RingBuffer ringBuffer;
  private final RevertMetrics metrics;
  private final RevertDebuggerOptions options;
  private final AtomicReference<Set<String>> contractAllowList;

  public RevertTracerProvider(
      final RingBuffer ringBuffer,
      final RevertMetrics metrics,
      final RevertDebuggerOptions options) {
    this.ringBuffer = ringBuffer;
    this.metrics = metrics;
    this.options = options;
    this.contractAllowList = new AtomicReference<>(normalize(options.getContracts()));
  }

  @Override
  public BlockAwareOperationTracer getBlockImportTracer(final BlockHeader blockHeader) {
    if (!options.isEnabled()) {
      return BlockAwareOperationTracer.NO_TRACING;
    }
    return new RevertTracer(blockHeader, ringBuffer, metrics, contractAllowList::get);
  }

  /** Hot-reload entry point used by commit 14. Replaces the allow-list atomically. */
  public void setContractAllowList(final List<String> contracts) {
    contractAllowList.set(normalize(contracts));
  }

  private static Set<String> normalize(final List<String> contracts) {
    return contracts.stream()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }
}
