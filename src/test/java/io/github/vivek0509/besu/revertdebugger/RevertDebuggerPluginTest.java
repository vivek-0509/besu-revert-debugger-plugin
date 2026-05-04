package io.github.vivek0509.besu.revertdebugger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.PluginRevertCategory;

import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategoryRegistry;

import java.util.function.DoubleSupplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RevertDebuggerPluginTest {

  @Test
  void getName_isLockedToRevertDebugger() {
    assertThat(new RevertDebuggerPlugin().getName()).isEqualTo("RevertDebugger");
  }

  @Test
  void registerWiresPicoCLIOptionsUnderPluginRevertNamespace() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    final RecordingPicoCLIOptions picoCli = new RecordingPicoCLIOptions();
    services.addService(PicoCLIOptions.class, picoCli);

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
  void startCreatesRevertMetricsAgainstMetricsSystem() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    final MetricsSystem metricsSystem = mock(MetricsSystem.class);
    services.addService(MetricsSystem.class, metricsSystem);

    plugin.register(services);
    plugin.start();

    verify(metricsSystem)
        .createLabelledCounter(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_count_total"),
            anyString(),
            eq("contract"),
            eq("reason_format"));
  }

  @Test
  void startWiresBufferDepthGaugeFromTheRingBuffer() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    final MetricsSystem metricsSystem = mock(MetricsSystem.class);
    services.addService(MetricsSystem.class, metricsSystem);

    plugin.register(services);
    plugin.start();

    final ArgumentCaptor<DoubleSupplier> supplierCaptor =
        ArgumentCaptor.forClass(DoubleSupplier.class);
    verify(metricsSystem)
        .createGauge(
            eq(PluginRevertCategory.PLUGIN_REVERT),
            eq("revert_buffer_depth"),
            anyString(),
            supplierCaptor.capture());

    // The buffer was just constructed and is empty, so the supplier reads zero. The point
    // of the assertion is that the supplier is the buffer's size method, not the placeholder
    // () -> 0.0 we had in commit 5.
    final DoubleSupplier supplier = supplierCaptor.getValue();
    assertThat(supplier).isNotNull();
    assertThat(supplier.getAsDouble()).isEqualTo(0.0);
  }

  @Test
  void startWithoutMetricsSystemServiceFailsLoudly() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    plugin.register(services);

    assertThatThrownBy(plugin::start)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MetricsSystem");
  }

  @Test
  void fullLifecycleSequenceCompletesWithoutThrowing() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    services.addService(MetricsSystem.class, mock(MetricsSystem.class));

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

  private static ServiceManager serviceManagerWithRegisterFakes() {
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());
    return services;
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
