package io.github.vivek0509.besu.revertdebugger.metrics;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.Histogram;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;

import java.util.function.DoubleSupplier;

/**
 * Holds the four metric handles the plugin contributes under the {@link
 * PluginRevertCategory#PLUGIN_REVERT} category. Construction registers all four metrics with the
 * given {@link MetricsSystem} and stashes the three writeable handles (the gauge is polled by
 * Besu's metrics system, so no handle is needed for it).
 *
 * <p>The {@code bufferDepthSupplier} is held by Besu's metrics system and called on every
 * Prometheus scrape. Until commit 7 introduces the ring buffer, callers pass a placeholder supplier
 * that returns 0; once the buffer exists, callers swap in {@code ringBuffer::size}.
 */
public class RevertMetrics {

  private static final double[] OVERHEAD_BUCKETS_SECONDS = {
    0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05
  };

  private final LabelledMetric<Counter> revertCount;
  private final LabelledMetric<Counter> revertGasUsed;
  private final Histogram captureOverhead;

  public RevertMetrics(
      final MetricsSystem metricsSystem, final DoubleSupplier bufferDepthSupplier) {
    this.revertCount =
        metricsSystem.createLabelledCounter(
            PluginRevertCategory.PLUGIN_REVERT,
            "revert_count_total",
            "Total reverted transactions captured by the RevertDebugger plugin",
            "contract",
            "reason_format");

    this.revertGasUsed =
        metricsSystem.createLabelledCounter(
            PluginRevertCategory.PLUGIN_REVERT,
            "revert_gas_used_total",
            "Total gas used by reverted transactions captured by the RevertDebugger plugin",
            "contract");

    this.captureOverhead =
        metricsSystem.createHistogram(
            PluginRevertCategory.PLUGIN_REVERT,
            "revert_capture_overhead_seconds",
            "Tracer capture overhead in seconds",
            OVERHEAD_BUCKETS_SECONDS);

    metricsSystem.createGauge(
        PluginRevertCategory.PLUGIN_REVERT,
        "revert_buffer_depth",
        "Current depth of the in-memory ring buffer of captured revert records",
        bufferDepthSupplier);
  }

  public LabelledMetric<Counter> revertCount() {
    return revertCount;
  }

  public LabelledMetric<Counter> revertGasUsed() {
    return revertGasUsed;
  }

  public Histogram captureOverhead() {
    return captureOverhead;
  }
}
