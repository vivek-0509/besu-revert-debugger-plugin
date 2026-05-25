package io.github.vivek0509.besu.revertdebugger.decode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Tags how a revert payload is shaped on the wire. */
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
