package io.github.vivek0509.besu.revertdebugger.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PluginRevertCategoryTest {

  @Test
  void getNameLowercasesEnumName() {
    assertThat(PluginRevertCategory.PLUGIN_REVERT.getName()).isEqualTo("plugin_revert");
  }

  @Test
  void applicationPrefixIsPluginUnderscore() {
    assertThat(PluginRevertCategory.PLUGIN_REVERT.getApplicationPrefix()).contains("plugin_");
  }

  @Test
  void enumHasSingleValue() {
    assertThat(PluginRevertCategory.values()).hasSize(1);
  }
}
