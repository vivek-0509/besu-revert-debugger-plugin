package io.github.vivek0509.besu.revertdebugger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.PluginRevertCategory;

import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategoryRegistry;

import org.junit.jupiter.api.Test;

class RevertDebuggerPluginTest {

  @Test
  void getName_isLockedToRevertDebugger() {
    assertThat(new RevertDebuggerPlugin().getName()).isEqualTo("RevertDebugger");
  }

  @Test
  void registerWiresPicoCLIOptionsUnderPluginRevertNamespace() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    final RecordingPicoCLIOptions picoCli = new RecordingPicoCLIOptions();
    services.addService(PicoCLIOptions.class, picoCli);
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());

    plugin.register(services);

    assertThat(picoCli.namespace).isEqualTo("plugin-revert");
    assertThat(picoCli.holder).isInstanceOf(RevertDebuggerOptions.class);
  }

  @Test
  void registerAddsPluginRevertCategoryToMetricCategoryRegistry() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    final RecordingMetricCategoryRegistry registry = new RecordingMetricCategoryRegistry();
    services.addService(MetricCategoryRegistry.class, registry);

    plugin.register(services);

    assertThat(registry.addedCategory).isEqualTo(PluginRevertCategory.PLUGIN_REVERT);
  }

  @Test
  void registerWithoutPicoCLIOptionsServiceFailsLoudly() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();

    assertThatThrownBy(() -> plugin.register(services))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PicoCLIOptions");
  }

  @Test
  void registerWithoutMetricCategoryRegistryFailsLoudly() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());

    assertThatThrownBy(() -> plugin.register(services))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MetricCategoryRegistry");
  }

  @Test
  void fullLifecycleSequenceCompletesWithoutThrowing() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());

    assertThatCode(
            () -> {
              plugin.register(services);
              plugin.beforeExternalServices();
              plugin.start();
              plugin.afterExternalServicePostMainLoop();
              plugin.stop();
            })
        .doesNotThrowAnyException();
  }

  private static final class RecordingPicoCLIOptions implements PicoCLIOptions {
    String namespace;
    Object holder;

    @Override
    public void addPicoCLIOptions(final String namespace, final Object holder) {
      this.namespace = namespace;
      this.holder = holder;
    }
  }

  private static final class RecordingMetricCategoryRegistry implements MetricCategoryRegistry {
    MetricCategory addedCategory;

    @Override
    public void addMetricCategory(final MetricCategory newMetricCategory) {
      this.addedCategory = newMetricCategory;
    }

    @Override
    public boolean isMetricCategoryEnabled(final MetricCategory metricCategory) {
      return true;
    }
  }
}
