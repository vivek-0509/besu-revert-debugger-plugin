package io.github.vivek0509.besu.revertdebugger.cli;

import java.util.List;

import picocli.CommandLine.Option;

/** Picocli mixin holding the {@code --plugin-revert-*} CLI flags. */
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
