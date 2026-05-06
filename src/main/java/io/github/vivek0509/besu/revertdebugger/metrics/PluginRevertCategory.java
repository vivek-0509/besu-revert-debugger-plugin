package io.github.vivek0509.besu.revertdebugger.metrics;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

import java.util.Locale;
import java.util.Optional;

/**
 * Custom {@link MetricCategory} the plugin registers with Besu's metrics system.
 *
 * <p>Single-value enum because we contribute exactly one category. The CLI flag {@code
 * --metrics-category=REVERT} matches against the uppercased {@link #getName()} value. Besu's
 * metrics system composes the rendered Prometheus name as {@code applicationPrefix + getName() +
 * "_" + metricName}, so a metric named {@code count_total} under this category is rendered as
 * {@code plugin_revert_count_total}. The category name and metric names must therefore not embed
 * the application prefix; doing so produces a doubled prefix in the rendered name.
 */
public enum PluginRevertCategory implements MetricCategory {
  REVERT;

  @Override
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  @Override
  public Optional<String> getApplicationPrefix() {
    return Optional.of("plugin_");
  }
}
