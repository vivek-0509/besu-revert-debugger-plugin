package io.github.vivek0509.besu.revertdebugger.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.hyperledger.besu.plugin.services.MetricsSystem;

import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.Test;

class RevertMetricsTest {

  @Test
  void constructorRegistersRevertCountWithContractAndReasonFormatLabels() {
    final MetricsSystem ms = mock(MetricsSystem.class);
    new RevertMetrics(ms, () -> 0.0);
    verify(ms)
        .createLabelledCounter(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_count_total"),
            anyString(),
            eq("contract"),
            eq("reason_format"));
  }

  @Test
  void constructorRegistersRevertGasUsedWithContractLabel() {
    final MetricsSystem ms = mock(MetricsSystem.class);
    new RevertMetrics(ms, () -> 0.0);
    verify(ms)
        .createLabelledCounter(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_gas_used_total"),
            anyString(),
            eq("contract"));
  }

  @Test
  void constructorRegistersBufferDepthGaugeWithSuppliedSource() {
    final MetricsSystem ms = mock(MetricsSystem.class);
    final DoubleSupplier supplier = () -> 42.0;
    new RevertMetrics(ms, supplier);
    verify(ms)
        .createGauge(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_buffer_depth"),
            anyString(),
            same(supplier));
  }

  @Test
  void constructorRegistersCaptureOverheadHistogramWithLatencyBuckets() {
    final MetricsSystem ms = mock(MetricsSystem.class);
    new RevertMetrics(ms, () -> 0.0);
    verify(ms)
        .createHistogram(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_capture_overhead_seconds"),
            anyString(),
            any(double[].class));
  }
}
