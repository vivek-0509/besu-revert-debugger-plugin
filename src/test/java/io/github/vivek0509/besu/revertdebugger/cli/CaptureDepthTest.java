package io.github.vivek0509.besu.revertdebugger.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CaptureDepthTest {

  private final CaptureDepth.PicocliConverter converter = new CaptureDepth.PicocliConverter();

  @Test
  void convertsLowercase() {
    assertThat(converter.convert("standard")).isEqualTo(CaptureDepth.STANDARD);
  }

  @Test
  void convertsUppercase() {
    assertThat(converter.convert("STANDARD")).isEqualTo(CaptureDepth.STANDARD);
  }

  @Test
  void convertsMixedCase() {
    assertThat(converter.convert("Minimal")).isEqualTo(CaptureDepth.MINIMAL);
  }

  @Test
  void rejectsInvalidValueWithIllegalArgument() {
    assertThatThrownBy(() -> converter.convert("banana"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
