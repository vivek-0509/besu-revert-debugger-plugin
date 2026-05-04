package io.github.vivek0509.besu.revertdebugger.cli;

import java.util.Locale;

import picocli.CommandLine.ITypeConverter;

/**
 * Tracer capture depth selected via {@code --plugin-revert-capture-depth}.
 *
 * <ul>
 *   <li>{@link #MINIMAL}: capture only what {@code traceEndTransaction} provides.
 *   <li>{@link #STANDARD}: also hook per-opcode tracing to record contract address and call depth
 *       at the REVERT site.
 *   <li>{@link #FULL}: also maintain a call-stack snapshot via {@code traceContextEnter}/{@code
 *       traceContextExit}.
 * </ul>
 *
 * <p>Subsequent commits read this value to decide which {@code OperationTracer} callbacks to wire.
 */
public enum CaptureDepth {
  MINIMAL,
  STANDARD,
  FULL;

  /**
   * Picocli converter that accepts {@code minimal|standard|full} in any case. Without this Picocli
   * would only accept the exact enum constant spelling, which contradicts the spec's lowercase
   * default.
   */
  public static class PicocliConverter implements ITypeConverter<CaptureDepth> {
    @Override
    public CaptureDepth convert(final String value) {
      return CaptureDepth.valueOf(value.toUpperCase(Locale.ROOT));
    }
  }
}
