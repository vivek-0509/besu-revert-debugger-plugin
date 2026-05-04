package io.github.vivek0509.besu.revertdebugger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.hyperledger.besu.plugin.ServiceManager;

import org.junit.jupiter.api.Test;

class RevertDebuggerPluginTest {

  @Test
  void getName_isLockedToRevertDebugger() {
    assertThat(new RevertDebuggerPlugin().getName()).isEqualTo("RevertDebugger");
  }

  @Test
  void fullLifecycleSequenceCompletesWithoutThrowing() {
    final RevertDebuggerPlugin plugin = new RevertDebuggerPlugin();
    final ServiceManager services = new ServiceManager.SimpleServiceManager();

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
}
