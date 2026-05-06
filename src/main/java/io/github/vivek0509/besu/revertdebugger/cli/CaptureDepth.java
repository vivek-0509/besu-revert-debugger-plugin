package io.github.vivek0509.besu.revertdebugger.cli;

import java.util.Locale;

import picocli.CommandLine.ITypeConverter;

/**
 * Tracer capture depth selected via {@code --plugin-revert-capture-depth}. The three values are
 * documented in the spec for forward compatibility:
 *
 * <ul>
 *   <li>{@link #MINIMAL}: end-of-transaction data only.
 *   <li>{@link #STANDARD}: end-of-transaction plus deepest revert-site contract and call depth (the
 *       level the current tracer captures).
 *   <li>{@link #FULL}: standard plus a per-frame call-stack snapshot.
 * </ul>
 *
 * <p>The flag is parsed and validated for spec compliance but the tracer currently uses one capture
 * path for all three values. The path matches what {@link #STANDARD} would describe.
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
