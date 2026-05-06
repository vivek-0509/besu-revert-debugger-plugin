package io.github.vivek0509.besu.revertdebugger.metrics;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.Histogram;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;

import java.util.function.DoubleSupplier;

/**
 * Holds the four metric handles the plugin contributes under the {@link
 * PluginRevertCategory#REVERT} category. Construction registers all four metrics with the given
 * {@link MetricsSystem} and stashes the three writeable handles (the gauge is polled by Besu's
 * metrics system, so no handle is needed for it).
 *
 * <p>The metric short names ({@code count_total}, {@code gas_used_total}, {@code buffer_depth},
 * {@code capture_overhead_seconds}) deliberately do not embed the {@code plugin_revert_} prefix;
 * Besu's {@code CategorizedPrometheusCollector} prepends {@code applicationPrefix + categoryName +
 * "_"} at registration time, so the rendered Prometheus names are {@code plugin_revert_*}.
 *
 * <p>The {@code bufferDepthSupplier} is held by Besu's metrics system and called on every
 * Prometheus scrape. Callers pass {@code ringBuffer::size} so the gauge reports the live record
 * count.
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
            PluginRevertCategory.REVERT,
            "count_total",
            "Total reverted transactions captured by the RevertDebugger plugin",
            "contract",
            "reason_format");

    this.revertGasUsed =
        metricsSystem.createLabelledCounter(
            PluginRevertCategory.REVERT,
            "gas_used_total",
            "Total gas used by reverted transactions captured by the RevertDebugger plugin",
            "contract");

    this.captureOverhead =
        metricsSystem.createHistogram(
            PluginRevertCategory.REVERT,
            "capture_overhead_seconds",
            "Tracer capture overhead in seconds",
            OVERHEAD_BUCKETS_SECONDS);

    metricsSystem.createGauge(
        PluginRevertCategory.REVERT,
        "buffer_depth",
        "Current depth of the in-memory ring buffer of captured revert records",
        bufferDepthSupplier);
  }

  /**
   * Records a single captured revert: increments the {@code count_total} counter labelled with the
   * contract and reason format, and adds {@code gasUsed} to the {@code gas_used_total} counter
   * labelled with the contract. Rendered Prometheus names are {@code plugin_revert_count_total} and
   * {@code plugin_revert_gas_used_total} respectively.
   */
  public void recordRevert(
      final String contract, final RevertReasonFormat format, final long gasUsed) {
    revertCount.labels(contract, format.displayName()).inc();
    revertGasUsed.labels(contract).inc(gasUsed);
  }

  /** Records the wall-clock time the tracer spent assembling one record, in seconds. */
  public void recordCaptureOverheadSeconds(final double seconds) {
    captureOverhead.observe(seconds);
  }
}
