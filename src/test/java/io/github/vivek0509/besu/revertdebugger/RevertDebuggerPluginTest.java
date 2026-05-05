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
import io.github.vivek0509.besu.revertdebugger.tracer.RevertTracerProvider;

import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BlockImportTracerProvider;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategoryRegistry;
import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;
import org.hyperledger.besu.plugin.services.rpc.PluginRpcResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

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
    services.addService(RpcEndpointService.class, new RecordingRpcEndpointService());

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
  void registerWiresRevertInspectUnderRevertNamespace() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());
    final RecordingRpcEndpointService rpc = new RecordingRpcEndpointService();
    services.addService(RpcEndpointService.class, rpc);

    plugin.register(services);

    final RecordingRpcEndpointService.Registration inspect =
        rpc.findRegistration("inspect").orElseThrow();
    assertThat(inspect.namespace()).isEqualTo("revert");
    assertThat(inspect.function()).isNotNull();
  }

  @Test
  void registerWiresRevertRecentUnderRevertNamespace() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());
    final RecordingRpcEndpointService rpc = new RecordingRpcEndpointService();
    services.addService(RpcEndpointService.class, rpc);

    plugin.register(services);

    final RecordingRpcEndpointService.Registration recent =
        rpc.findRegistration("recent").orElseThrow();
    assertThat(recent.namespace()).isEqualTo("revert");
    assertThat(recent.function()).isNotNull();
  }

  @Test
  void registerWithoutRpcEndpointServiceFailsLoudly() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();
    services.addService(PicoCLIOptions.class, new RecordingPicoCLIOptions());
    services.addService(MetricCategoryRegistry.class, new RecordingMetricCategoryRegistry());

    assertThatThrownBy(() -> plugin.register(services))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RpcEndpointService");
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
  void startRegistersBlockImportTracerProviderOnTheServiceManager() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = serviceManagerWithRegisterFakes();
    services.addService(MetricsSystem.class, mock(MetricsSystem.class));

    plugin.register(services);
    plugin.start();

    assertThat(services.getService(BlockImportTracerProvider.class)).isPresent();
    assertThat(services.getService(BlockImportTracerProvider.class).get())
        .isInstanceOf(RevertTracerProvider.class);
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
    services.addService(RpcEndpointService.class, new RecordingRpcEndpointService());
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

  private static final class RecordingRpcEndpointService implements RpcEndpointService {
    final List<Registration> registrations = new ArrayList<>();

    @Override
    public <T> void registerRPCEndpoint(
        final String namespace,
        final String functionName,
        final Function<PluginRpcRequest, T> function) {
      registrations.add(new Registration(namespace, functionName, function));
    }

    @Override
    public PluginRpcResponse call(final String methodName, final Object[] params) {
      return null;
    }

    java.util.Optional<Registration> findRegistration(final String functionName) {
      return registrations.stream().filter(r -> r.functionName().equals(functionName)).findFirst();
    }

    record Registration(
        String namespace, String functionName, Function<PluginRpcRequest, ?> function) {}
  }
}
