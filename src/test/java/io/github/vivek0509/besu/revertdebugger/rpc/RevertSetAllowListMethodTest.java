package io.github.vivek0509.besu.revertdebugger.rpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.cli.RevertDebuggerOptions;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;
import io.github.vivek0509.besu.revertdebugger.tracer.RevertTracerProvider;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Histogram;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.List;

import org.junit.jupiter.api.Test;

class RevertSetAllowListMethodTest {

  private static PluginRpcRequest requestWithParams(final Object... params) {
    final PluginRpcRequest request = mock(PluginRpcRequest.class);
    when(request.getParams()).thenReturn(params);
    return request;
  }

  @SuppressWarnings("unchecked")
  private static RevertTracerProvider freshProvider() {
    final MetricsSystem metricsSystem = mock(MetricsSystem.class);
    when(metricsSystem.createLabelledCounter(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.<String>any()))
        .thenReturn(mock(LabelledMetric.class));
    when(metricsSystem.createHistogram(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(double[].class)))
        .thenReturn(mock(Histogram.class));
    final RingBuffer buffer = new RingBuffer(10);
    final RevertMetrics metrics = new RevertMetrics(metricsSystem, buffer::size);
    final RevertDebuggerOptions options = new RevertDebuggerOptions();
    return new RevertTracerProvider(buffer, metrics, options);
  }

  @Test
  void replacesAllowList() {
    final RevertTracerProvider provider = freshProvider();
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(() -> provider);

    final int size = method.execute(requestWithParams(List.of("0xabc", "0xDEF")));

    assertThat(size).isEqualTo(2);
    assertThat(provider.getContractAllowList()).containsExactlyInAnyOrder("0xabc", "0xdef");
  }

  @Test
  void emptyListResetsToCaptureAll() {
    final RevertTracerProvider provider = freshProvider();
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(() -> provider);

    method.execute(requestWithParams(List.of("0xabc")));
    assertThat(provider.getContractAllowList()).hasSize(1);

    final int size = method.execute(requestWithParams(List.of()));

    assertThat(size).isZero();
    assertThat(provider.getContractAllowList()).isEmpty();
  }

  @Test
  void throwsOnZeroParams() {
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(this::neverCalled);
    assertThatThrownBy(() -> method.execute(requestWithParams()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("one parameter");
  }

  @Test
  void throwsOnNullParam() {
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(this::neverCalled);
    assertThatThrownBy(() -> method.execute(requestWithParams((Object) null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throwsOnNonArrayParam() {
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(this::neverCalled);
    assertThatThrownBy(() -> method.execute(requestWithParams("0xabc")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("JSON array");
  }

  @Test
  void throwsOnNullElementInArray() {
    final RevertSetAllowListMethod method = new RevertSetAllowListMethod(this::neverCalled);
    final List<Object> withNull = new java.util.ArrayList<>();
    withNull.add("0xabc");
    withNull.add(null);
    assertThatThrownBy(() -> method.execute(requestWithParams(withNull)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null");
  }

  private RevertTracerProvider neverCalled() {
    throw new AssertionError("provider must not be queried when validation fails");
  }
}
