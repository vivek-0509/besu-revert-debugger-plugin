package io.github.vivek0509.besu.revertdebugger;

import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.PluginRevertCategory;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;
import io.github.vivek0509.besu.revertdebugger.rpc.RevertInspectMethod;
import io.github.vivek0509.besu.revertdebugger.rpc.RevertRecentMethod;
import io.github.vivek0509.besu.revertdebugger.rpc.RevertStatsMethod;
import io.github.vivek0509.besu.revertdebugger.tracer.RevertTracerProvider;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.BlockImportTracerProvider;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.plugin.services.RpcEndpointService;
import org.hyperledger.besu.plugin.services.metrics.MetricCategoryRegistry;

import java.util.concurrent.CompletableFuture;

import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the RevertDebugger Besu plugin.
 *
 * <p>The {@code @AutoService} annotation triggers an annotation processor at compile time that
 * generates {@code META-INF/services/org.hyperledger.besu.plugin.BesuPlugin} containing this
 * class's fully qualified name. Java's {@link java.util.ServiceLoader}, which Besu uses to discover
 * plugins on its classpath, reads that file at startup. We rely on the generated form rather than a
 * hand-written SPI file because a class rename here regenerates the SPI entry automatically; a
 * hand-written file would silently go stale.
 *
 * <p>This class currently implements the lifecycle as no-ops with INFO/DEBUG log lines so the
 * plugin loads cleanly and emits visible signals for each lifecycle phase. Subsequent commits fill
 * {@link #register(ServiceManager)} and {@link #start()} with real behaviour.
 */
@AutoService(BesuPlugin.class)
public class RevertDebuggerPlugin implements BesuPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(RevertDebuggerPlugin.class);
  private static final String PLUGIN_NAME = "RevertDebugger";

  private static final String CLI_NAMESPACE = "revert";
  private static final String RPC_NAMESPACE = "revert";

  private ServiceManager serviceManager;
  private RevertDebuggerOptions options;
  private RingBuffer ringBuffer;
  private RevertMetrics metrics;
  private RevertTracerProvider tracerProvider;

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }

  /**
   * Stashes the {@link ServiceManager}, instantiates the CLI options holder, registers it with
   * {@link PicoCLIOptions}, and registers our custom {@link PluginRevertCategory} with the {@link
   * MetricCategoryRegistry}. Both services are required: missing either means we cannot operate, so
   * {@code orElseThrow} is the right shape rather than a silent skip.
   */
  @Override
  public void register(final ServiceManager serviceManager) {
    LOG.info("{} registering", PLUGIN_NAME);
    this.serviceManager = serviceManager;
    this.options = new RevertDebuggerOptions();

    final PicoCLIOptions picoCliOptions =
        serviceManager
            .getService(PicoCLIOptions.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        PLUGIN_NAME
                            + " requires the PicoCLIOptions service but it was not available"));
    picoCliOptions.addPicoCLIOptions(CLI_NAMESPACE, options);

    final MetricCategoryRegistry categoryRegistry =
        serviceManager
            .getService(MetricCategoryRegistry.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        PLUGIN_NAME
                            + " requires the MetricCategoryRegistry service but it was not"
                            + " available"));
    categoryRegistry.addMetricCategory(PluginRevertCategory.PLUGIN_REVERT);

    final RpcEndpointService rpcEndpointService =
        serviceManager
            .getService(RpcEndpointService.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        PLUGIN_NAME + " requires the RpcEndpointService but it was not available"));
    rpcEndpointService.registerRPCEndpoint(
        RPC_NAMESPACE, "inspect", new RevertInspectMethod(() -> ringBuffer)::execute);
    rpcEndpointService.registerRPCEndpoint(
        RPC_NAMESPACE, "recent", new RevertRecentMethod(() -> ringBuffer)::execute);
    rpcEndpointService.registerRPCEndpoint(
        RPC_NAMESPACE, "stats", new RevertStatsMethod(() -> ringBuffer)::execute);
  }

  @Override
  public void beforeExternalServices() {
    LOG.debug("{} beforeExternalServices", PLUGIN_NAME);
  }

  @Override
  public void start() {
    LOG.info("{} starting", PLUGIN_NAME);

    final MetricsSystem metricsSystem =
        serviceManager
            .getService(MetricsSystem.class)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        PLUGIN_NAME
                            + " requires the MetricsSystem service but it was not available"));

    this.ringBuffer = new RingBuffer(options.getBufferSize());
    this.metrics = new RevertMetrics(metricsSystem, ringBuffer::size);
    this.tracerProvider = new RevertTracerProvider(ringBuffer, metrics, options);
    serviceManager.addService(BlockImportTracerProvider.class, tracerProvider);
  }

  @Override
  public void afterExternalServicePostMainLoop() {
    LOG.debug("{} afterExternalServicePostMainLoop", PLUGIN_NAME);
  }

  /**
   * Re-reads the contract allow-list from the options holder and atomically swaps it on the tracer
   * provider. Buffer size, capture depth, and the master enable flag are not reloaded: the enable
   * flag is already polled per-block by the provider, and the others would require rebuilding the
   * ring buffer (losing captured records).
   */
  @Override
  public CompletableFuture<Void> reloadConfiguration() {
    if (tracerProvider == null) {
      LOG.warn("{} reloadConfiguration called before start; ignoring", PLUGIN_NAME);
      return CompletableFuture.completedFuture(null);
    }
    LOG.info("{} reloading contract allow-list", PLUGIN_NAME);
    tracerProvider.setContractAllowList(options.getContracts());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void stop() {
    LOG.info("{} stopping", PLUGIN_NAME);
  }
}
