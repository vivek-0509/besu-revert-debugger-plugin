package io.github.vivek0509.besu.revertdebugger.metrics;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;

import java.util.function.DoubleSupplier;

/** Holds the metric handles the plugin contributes under {@link PluginRevertCategory#REVERT}. */
public class RevertMetrics {

  private final LabelledMetric<Counter> revertCount;
  private final LabelledMetric<Counter> revertGasUsed;

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

    metricsSystem.createGauge(
        PluginRevertCategory.REVERT,
        "buffer_depth",
        "Current depth of the in-memory ring buffer of captured revert records",
        bufferDepthSupplier);
  }

  public void recordRevert(
      final String contract, final RevertReasonFormat format, final long gasUsed) {
    revertCount.labels(contract, format.displayName()).inc();
    revertGasUsed.labels(contract).inc(gasUsed);
  }
}
