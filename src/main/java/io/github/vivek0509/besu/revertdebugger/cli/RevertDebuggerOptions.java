package io.github.vivek0509.besu.revertdebugger.cli;

import java.util.List;

import picocli.CommandLine.Option;

/**
 * Picocli mixin holding the three {@code --plugin-revert-*} CLI flags. The plugin passes this
 * object to {@code PicoCLIOptions.addPicoCLIOptions("revert", this)}; Besu prepends {@code
 * --plugin-} to the namespace string, producing the {@code --plugin-revert-} prefix that every
 * field below uses. The class is values-only with no logic, so option parsing is unit-testable
 * without instantiating the plugin.
 */
public class RevertDebuggerOptions {

  @Option(
      names = "--plugin-revert-enabled",
      description = "Master toggle for the RevertDebugger plugin (default: ${DEFAULT-VALUE})",
      defaultValue = "true",
      arity = "1",
      paramLabel = "<true|false>")
  private boolean enabled = true;

  @Option(
      names = "--plugin-revert-buffer-size",
      description = "Ring buffer capacity for captured revert records (default: ${DEFAULT-VALUE})",
      defaultValue = "10000",
      paramLabel = "<COUNT>")
  private int bufferSize = 10000;

  @Option(
      names = "--plugin-revert-contracts",
      description =
          "Comma-separated contract addresses to capture; empty means capture all (default: empty)",
      paramLabel = "<0xaddr,...>",
      split = ",")
  private List<String> contracts = List.of();

  public boolean isEnabled() {
    return enabled;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public List<String> getContracts() {
    return contracts;
  }
}
