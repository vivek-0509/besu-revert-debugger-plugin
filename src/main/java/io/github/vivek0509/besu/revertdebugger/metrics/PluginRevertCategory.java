package io.github.vivek0509.besu.revertdebugger.metrics;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

import java.util.Locale;
import java.util.Optional;

/**
 * Custom {@link MetricCategory} the plugin registers with Besu's metrics system.
 *
 * <p>Single-value enum because we contribute exactly one category. The category name (used by
 * {@code --metrics-category=PLUGIN_REVERT} and in Besu logs) is the lowercased enum name. The
 * application prefix is concatenated by Besu's metrics system before each metric name we create in
 * subsequent commits, so a metric named {@code revert_count_total} is rendered to Prometheus as
 * {@code plugin_revert_count_total}.
 */
public enum PluginRevertCategory implements MetricCategory {
  PLUGIN_REVERT;

  @Override
  public String getName() {
    return name().toLowerCase(Locale.ROOT);
  }

  @Override
  public Optional<String> getApplicationPrefix() {
    return Optional.of("plugin_");
  }
}
