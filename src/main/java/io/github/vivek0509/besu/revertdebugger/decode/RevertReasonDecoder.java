package io.github.vivek0509.besu.revertdebugger.decode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;

/**
 * Stateless decoder for Solidity's two well-known revert payload formats.
 *
 * <ul>
 *   <li>{@code Error(string)}: selector {@code 0x08c379a0}, ABI-encoded payload {@code [offset =
 *       0x20][length][N bytes UTF-8 string padded to 32]}.
 *   <li>{@code Panic(uint256)}: selector {@code 0x4e487b71}, payload {@code [32-byte uint256]}.
 * </ul>
 *
 * <p>Anything else, including custom errors and bare {@code revert()} calls, is tagged {@link
 * RevertReasonFormat#UNKNOWN}; the raw bytes survive elsewhere on the {@code RevertRecord} for
 * downstream tooling. The decoder never throws: malformed inputs map to {@code UNKNOWN} too,
 * because this method runs on the block-import hot path and exceptions would just push the recovery
 * decision into the caller without changing what the wire output looks like.
 */
public final class RevertReasonDecoder {

  private static final Bytes ERROR_STRING_SELECTOR = Bytes.fromHexString("0x08c379a0");
  private static final Bytes PANIC_UINT256_SELECTOR = Bytes.fromHexString("0x4e487b71");

  private static final int ERROR_STRING_HEADER_BYTES = 4 + 32 + 32;
  private static final int PANIC_PAYLOAD_BYTES = 4 + 32;
  private static final int MAX_REASONABLE_STRING_LENGTH = 1_000_000;

  private static final Map<BigInteger, String> PANIC_CODES =
      Map.ofEntries(
          Map.entry(BigInteger.valueOf(0x01), "Assertion failed"),
          Map.entry(BigInteger.valueOf(0x11), "Arithmetic over/underflow"),
          Map.entry(BigInteger.valueOf(0x12), "Division by zero"),
          Map.entry(BigInteger.valueOf(0x21), "Invalid enum value"),
          Map.entry(BigInteger.valueOf(0x22), "Storage byte array incorrectly encoded"),
          Map.entry(BigInteger.valueOf(0x31), "Pop on empty array"),
          Map.entry(BigInteger.valueOf(0x32), "Array index out of bounds"),
          Map.entry(BigInteger.valueOf(0x41), "Memory allocation overflow"),
          Map.entry(BigInteger.valueOf(0x51), "Zero-initialized function pointer called"));

  private RevertReasonDecoder() {}

  /**
   * Decoder result. {@code reason} is null when {@code format} is {@link
   * RevertReasonFormat#UNKNOWN}.
   */
  public record Decoded(RevertReasonFormat format, String reason) {}

  /**
   * Decodes a revert payload into a tagged reason. Never throws: malformed inputs return {@link
   * RevertReasonFormat#UNKNOWN}.
   */
  public static Decoded decode(final Bytes revertBytes) {
    if (revertBytes == null || revertBytes.size() < 4) {
      return unknown();
    }

    final Bytes selector = revertBytes.slice(0, 4);

    if (selector.equals(ERROR_STRING_SELECTOR)) {
      final String reason = decodeErrorString(revertBytes);
      if (reason != null) {
        return new Decoded(RevertReasonFormat.ERROR_STRING, reason);
      }
    } else if (selector.equals(PANIC_UINT256_SELECTOR)) {
      final String reason = decodePanic(revertBytes);
      if (reason != null) {
        return new Decoded(RevertReasonFormat.PANIC_UINT256, reason);
      }
    }

    return unknown();
  }

  private static Decoded unknown() {
    return new Decoded(RevertReasonFormat.UNKNOWN, null);
  }

  private static String decodeErrorString(final Bytes revertBytes) {
    if (revertBytes.size() < ERROR_STRING_HEADER_BYTES) {
      return null;
    }

    final int length;
    try {
      length = revertBytes.slice(36, 32).toUnsignedBigInteger().intValueExact();
    } catch (final ArithmeticException e) {
      return null;
    }
    if (length < 0 || length > MAX_REASONABLE_STRING_LENGTH) {
      return null;
    }
    if (revertBytes.size() < ERROR_STRING_HEADER_BYTES + length) {
      return null;
    }

    return new String(
        revertBytes.slice(ERROR_STRING_HEADER_BYTES, length).toArrayUnsafe(),
        StandardCharsets.UTF_8);
  }

  private static String decodePanic(final Bytes revertBytes) {
    if (revertBytes.size() != PANIC_PAYLOAD_BYTES) {
      return null;
    }

    final BigInteger code = revertBytes.slice(4, 32).toUnsignedBigInteger();
    final String name = PANIC_CODES.get(code);
    return name != null ? name : "Panic 0x" + code.toString(16);
  }
}
