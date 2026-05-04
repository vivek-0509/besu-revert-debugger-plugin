package io.github.vivek0509.besu.revertdebugger.decode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RevertReasonFormatTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void errorStringSerializesToParenthesisForm() throws Exception {
    assertThat(mapper.writeValueAsString(RevertReasonFormat.ERROR_STRING))
        .isEqualTo("\"Error(string)\"");
  }

  @Test
  void panicUint256SerializesToParenthesisForm() throws Exception {
    assertThat(mapper.writeValueAsString(RevertReasonFormat.PANIC_UINT256))
        .isEqualTo("\"Panic(uint256)\"");
  }

  @Test
  void unknownSerializesToUnknown() throws Exception {
    assertThat(mapper.writeValueAsString(RevertReasonFormat.UNKNOWN)).isEqualTo("\"Unknown\"");
  }

  @Test
  void deserializesByDisplayName() throws Exception {
    assertThat(mapper.readValue("\"Error(string)\"", RevertReasonFormat.class))
        .isEqualTo(RevertReasonFormat.ERROR_STRING);
  }

  @Test
  void fromDisplayNameRejectsUnknownInput() {
    assertThatThrownBy(() -> RevertReasonFormat.fromDisplayName("banana"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
