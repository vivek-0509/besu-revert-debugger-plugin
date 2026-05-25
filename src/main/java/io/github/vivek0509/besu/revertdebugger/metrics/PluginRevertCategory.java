package io.github.vivek0509.besu.revertdebugger.metrics;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

import java.util.Locale;
import java.util.Optional;

/** Custom {@link MetricCategory} the plugin registers with Besu's metrics system. */
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
