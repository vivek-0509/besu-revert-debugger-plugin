package io.github.vivek0509.besu.revertdebugger.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class RevertDebuggerOptionsTest {

  private static RevertDebuggerOptions parse(final String... args) {
    final RevertDebuggerOptions options = new RevertDebuggerOptions();
    new CommandLine(options).parseArgs(args);
    return options;
  }

  @Test
  void defaultsMatchSpec() {
    final RevertDebuggerOptions options = parse();
    assertThat(options.isEnabled()).isTrue();
    assertThat(options.getBufferSize()).isEqualTo(10000);
    assertThat(options.getContracts()).isEmpty();
    assertThat(options.getCaptureDepth()).isEqualTo(CaptureDepth.STANDARD);
  }

  @Test
  void enabledFalseDisablesPlugin() {
    assertThat(parse("--plugin-revert-enabled", "false").isEnabled()).isFalse();
  }

  @Test
  void bufferSizeAcceptsExplicitValue() {
    assertThat(parse("--plugin-revert-buffer-size", "500").getBufferSize()).isEqualTo(500);
  }

  @Test
  void contractsCsvSplitsIntoList() {
    assertThat(parse("--plugin-revert-contracts", "0xaaa,0xbbb").getContracts())
        .containsExactly("0xaaa", "0xbbb");
  }

  @Test
  void captureDepthLowercaseParsesToEnum() {
    assertThat(parse("--plugin-revert-capture-depth", "full").getCaptureDepth())
        .isEqualTo(CaptureDepth.FULL);
  }

  @Test
  void captureDepthInvalidValueThrowsParameterException() {
    final RevertDebuggerOptions options = new RevertDebuggerOptions();
    assertThatThrownBy(
            () -> new CommandLine(options).parseArgs("--plugin-revert-capture-depth", "banana"))
        .isInstanceOf(CommandLine.ParameterException.class);
  }
}
