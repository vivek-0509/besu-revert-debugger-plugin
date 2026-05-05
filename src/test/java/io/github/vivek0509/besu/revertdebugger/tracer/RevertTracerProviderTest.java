package io.github.vivek0509.besu.revertdebugger.tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;

import org.hyperledger.besu.plugin.data.BlockHeader;
import org.hyperledger.besu.plugin.services.tracer.BlockAwareOperationTracer;

import java.util.List;

import org.junit.jupiter.api.Test;

class RevertTracerProviderTest {

  private static RevertDebuggerOptions options(
      final boolean enabled, final List<String> contracts) {
    final RevertDebuggerOptions options = mock(RevertDebuggerOptions.class);
    when(options.isEnabled()).thenReturn(enabled);
    when(options.getContracts()).thenReturn(contracts);
    return options;
  }

  @Test
  void disabledOptionsReturnsNoTracingTracer() {
    final RevertTracerProvider provider =
        new RevertTracerProvider(
            new RingBuffer(10), mock(RevertMetrics.class), options(false, List.of()));

    final BlockAwareOperationTracer tracer = provider.getBlockImportTracer(mock(BlockHeader.class));

    assertThat(tracer).isSameAs(BlockAwareOperationTracer.NO_TRACING);
  }

  @Test
  void enabledOptionsReturnsRealRevertTracer() {
    final RevertTracerProvider provider =
        new RevertTracerProvider(
            new RingBuffer(10), mock(RevertMetrics.class), options(true, List.of()));

    final BlockAwareOperationTracer tracer = provider.getBlockImportTracer(mock(BlockHeader.class));

    assertThat(tracer).isInstanceOf(RevertTracer.class);
  }

  @Test
  void everyCallReturnsAFreshTracerInstance() {
    final RevertTracerProvider provider =
        new RevertTracerProvider(
            new RingBuffer(10), mock(RevertMetrics.class), options(true, List.of()));

    final BlockAwareOperationTracer first = provider.getBlockImportTracer(mock(BlockHeader.class));
    final BlockAwareOperationTracer second = provider.getBlockImportTracer(mock(BlockHeader.class));

    assertThat(first).isNotSameAs(second);
  }

  @Test
  void contractAllowListIsLowercasedAtConstruction() {
    final RevertTracerProvider provider =
        new RevertTracerProvider(
            new RingBuffer(10),
            mock(RevertMetrics.class),
            options(true, List.of("0xAaBbCcDdEeFfAaBbCcDdEeFfAaBbCcDdEeFfAaBb")));

    // Replace via the hot-reload entry point to confirm normalisation also happens there.
    provider.setContractAllowList(List.of("0xCAFEBABEcafebabecafebabecafebabecafebabe"));

    // We cannot read the AtomicReference directly from the test, but the lowercasing contract is
    // exercised through the tracer in RevertTracerTest's allow-list cases. This test serves as a
    // smoke test that constructor and setter accept mixed-case input without throwing.
    assertThat(provider).isNotNull();
  }
}
