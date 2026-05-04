package io.github.vivek0509.besu.revertdebugger;

import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;

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

  private static final String CLI_NAMESPACE = "plugin-revert";

  private ServiceManager serviceManager;
  private RevertDebuggerOptions options;

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }

  /**
   * Stashes the {@link ServiceManager}, instantiates the CLI options holder, and registers it with
   * the {@link PicoCLIOptions} service so Besu parses our four flags. Throws loudly if {@code
   * PicoCLIOptions} is unavailable: the plugin cannot operate without parsed CLI flags.
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
  }

  @Override
  public void beforeExternalServices() {
    LOG.debug("{} beforeExternalServices", PLUGIN_NAME);
  }

  @Override
  public void start() {
    LOG.info("{} starting", PLUGIN_NAME);
  }

  @Override
  public void afterExternalServicePostMainLoop() {
    LOG.debug("{} afterExternalServicePostMainLoop", PLUGIN_NAME);
  }

  @Override
  public void stop() {
    LOG.info("{} stopping", PLUGIN_NAME);
  }
}
