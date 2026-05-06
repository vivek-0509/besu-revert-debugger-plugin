package io.github.vivek0509.besu.revertdebugger.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PluginRevertCategoryTest {

  @Test
  void getNameLowercasesEnumName() {
    assertThat(PluginRevertCategory.REVERT.getName()).isEqualTo("revert");
  }
}
