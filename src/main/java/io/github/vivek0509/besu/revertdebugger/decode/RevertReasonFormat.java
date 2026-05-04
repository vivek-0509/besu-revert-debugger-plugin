package io.github.vivek0509.besu.revertdebugger.decode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tags how a revert payload is shaped on the wire.
 *
 * <p>Solidity defines two well-known revert formats: {@code Error(string)} (selector {@code
 * 0x08c379a0}) and {@code Panic(uint256)} (selector {@code 0x4e487b71}). Anything else, including
 * custom errors and bare {@code revert()} calls, is tagged {@link #UNKNOWN}; the raw bytes are
 * preserved so a downstream tool can decode them externally.
 *
 * <p>The {@code @JsonValue} and {@code @JsonCreator} pair maps the enum to and from its
 * spec-defined display string ({@code "Error(string)"}, {@code "Panic(uint256)"}, {@code
 * "Unknown"}) rather than the Java enum constant name.
 */
public enum RevertReasonFormat {
  ERROR_STRING("Error(string)"),
  PANIC_UINT256("Panic(uint256)"),
  UNKNOWN("Unknown");

  private final String displayName;

  RevertReasonFormat(final String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String displayName() {
    return displayName;
  }

  @JsonCreator
  public static RevertReasonFormat fromDisplayName(final String displayName) {
    for (final RevertReasonFormat format : values()) {
      if (format.displayName.equals(displayName)) {
        return format;
      }
    }
    throw new IllegalArgumentException("Unknown revert reason format: " + displayName);
  }
}
