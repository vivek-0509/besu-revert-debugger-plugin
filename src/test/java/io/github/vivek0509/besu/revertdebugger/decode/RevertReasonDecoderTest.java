package io.github.vivek0509.besu.revertdebugger.decode;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonDecoder.Decoded;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class RevertReasonDecoderTest {

  @Test
  void nullInputReturnsUnknown() {
    final Decoded result = RevertReasonDecoder.decode(null);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
    assertThat(result.reason()).isNull();
  }

  @Test
  void emptyInputReturnsUnknown() {
    final Decoded result = RevertReasonDecoder.decode(Bytes.EMPTY);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
    assertThat(result.reason()).isNull();
  }

  @Test
  void inputShorterThanSelectorReturnsUnknown() {
    final Decoded result = RevertReasonDecoder.decode(Bytes.fromHexString("0x123456"));
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
  }

  @Test
  void unknownSelectorReturnsUnknown() {
    final Decoded result = RevertReasonDecoder.decode(Bytes.fromHexString("0x1234567800000000"));
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
    assertThat(result.reason()).isNull();
  }

  @Test
  void errorStringDecodesInsufficientBalance() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x08c379a0"
                + "0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000014"
                + "496e73756666696369656e742062616c616e6365000000000000000000000000");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.ERROR_STRING);
    assertThat(result.reason()).isEqualTo("Insufficient balance");
  }

  @Test
  void errorStringDecodesEmptyMessage() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x08c379a0"
                + "0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000000");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.ERROR_STRING);
    assertThat(result.reason()).isEmpty();
  }

  @Test
  void errorStringWithMalformedLengthReturnsUnknown() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x08c379a0"
                + "0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000064"
                + "deadbeef");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
    assertThat(result.reason()).isNull();
  }

  @Test
  void errorStringWithImpossiblyLargeLengthReturnsUnknown() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x08c379a0"
                + "0000000000000000000000000000000000000000000000000000000000000020"
                + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
  }

  @Test
  void panicAssertionFailedReturnsKnownName() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x4e487b71" + "0000000000000000000000000000000000000000000000000000000000000001");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.PANIC_UINT256);
    assertThat(result.reason()).isEqualTo("Assertion failed");
  }

  @Test
  void panicArithmeticOverflowReturnsKnownName() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x4e487b71" + "0000000000000000000000000000000000000000000000000000000000000011");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.PANIC_UINT256);
    assertThat(result.reason()).isEqualTo("Arithmetic over/underflow");
  }

  @Test
  void panicWithUnknownCodeReturnsRawHexName() {
    final Bytes payload =
        Bytes.fromHexString(
            "0x4e487b71" + "0000000000000000000000000000000000000000000000000000000000000099");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.PANIC_UINT256);
    assertThat(result.reason()).isEqualTo("Panic 0x99");
  }

  @Test
  void panicWithWrongSizeReturnsUnknown() {
    final Bytes payload = Bytes.fromHexString("0x4e487b7100000000000000000000000000000001");
    final Decoded result = RevertReasonDecoder.decode(payload);
    assertThat(result.format()).isEqualTo(RevertReasonFormat.UNKNOWN);
  }
}
