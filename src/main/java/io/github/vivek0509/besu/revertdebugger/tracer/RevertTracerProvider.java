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

/** Factory for {@link RevertTracer} instances. Holds the live contract allow-list. */
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

  public void setContractAllowList(final List<String> contracts) {
    contractAllowList.set(normalize(contracts));
  }

  public Set<String> getContractAllowList() {
    return contractAllowList.get();
  }

  private static Set<String> normalize(final List<String> contracts) {
    return contracts.stream()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }
}
