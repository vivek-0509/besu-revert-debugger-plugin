package io.github.vivek0509.besu.revertdebugger.capture;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RevertRecordTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static RevertRecord sample() {
    return new RevertRecord(
        "0x1111111111111111111111111111111111111111111111111111111111111111",
        12345L,
        "0x2222222222222222222222222222222222222222222222222222222222222222",
        "0x3333333333333333333333333333333333333333",
        "0x4444444444444444444444444444444444444444",
        "0x12345678",
        "0x12345678aabbccdd",
        RevertReasonFormat.ERROR_STRING,
        "Insufficient balance",
        "0x08c379a000000000000000000000000000000000000000000000000000000000",
        87432L,
        3,
        1714670400L);
  }

  @Test
  void roundTripsCleanlyThroughJackson() throws Exception {
    final RevertRecord original = sample();
    final String json = mapper.writeValueAsString(original);
    final RevertRecord deserialized = mapper.readValue(json, RevertRecord.class);
    assertThat(deserialized).isEqualTo(original);
  }

  @Test
  void serializesAllThirteenSpecFieldsInDeclarationOrder() throws Exception {
    final String json = mapper.writeValueAsString(sample());
    final JsonNode tree = mapper.readTree(json);

    final List<String> fieldNames = new ArrayList<>();
    tree.fieldNames().forEachRemaining(fieldNames::add);

    assertThat(fieldNames)
        .containsExactly(
            "txHash",
            "blockNumber",
            "blockHash",
            "contract",
            "from",
            "functionSelector",
            "calldata",
            "reasonFormat",
            "decodedReason",
            "rawRevertBytes",
            "gasUsed",
            "callDepth",
            "timestamp");
  }

  @Test
  void nullDecodedReasonSerializesAsJsonNull() throws Exception {
    final RevertRecord record =
        new RevertRecord(
            "0xabc",
            1L,
            "0xdef",
            "0x1",
            "0x2",
            "0x12345678",
            "0x",
            RevertReasonFormat.UNKNOWN,
            null,
            "0x",
            0L,
            0,
            0L);
    final JsonNode tree = mapper.readTree(mapper.writeValueAsString(record));
    assertThat(tree.has("decodedReason")).isTrue();
    assertThat(tree.get("decodedReason").isNull()).isTrue();
  }

  @Test
  void reasonFormatSerializesAsParenthesisForm() throws Exception {
    final JsonNode tree = mapper.readTree(mapper.writeValueAsString(sample()));
    assertThat(tree.get("reasonFormat").asText()).isEqualTo("Error(string)");
  }

  @Test
  void numericFieldsSerializeAsJsonNumbers() throws Exception {
    final JsonNode tree = mapper.readTree(mapper.writeValueAsString(sample()));
    assertThat(tree.get("blockNumber").isNumber()).isTrue();
    assertThat(tree.get("gasUsed").isNumber()).isTrue();
    assertThat(tree.get("callDepth").isNumber()).isTrue();
    assertThat(tree.get("timestamp").isNumber()).isTrue();
  }
}
