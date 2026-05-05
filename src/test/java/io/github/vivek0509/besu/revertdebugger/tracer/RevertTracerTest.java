package io.github.vivek0509.besu.revertdebugger.tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.Transaction;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.plugin.data.BlockHeader;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RevertTracerTest {

  private static final Hash TX_HASH =
      Hash.fromHexString("0x1111111111111111111111111111111111111111111111111111111111111111");
  private static final Hash BLOCK_HASH =
      Hash.fromHexString("0x2222222222222222222222222222222222222222222222222222222222222222");
  private static final Address SENDER =
      Address.fromHexString("0x4444444444444444444444444444444444444444");
  private static final Address CONTRACT =
      Address.fromHexString("0x3333333333333333333333333333333333333333");

  private static final Bytes ERROR_INSUFFICIENT_BALANCE =
      Bytes.fromHexString(
          "0x08c379a0"
              + "0000000000000000000000000000000000000000000000000000000000000020"
              + "0000000000000000000000000000000000000000000000000000000000000014"
              + "496e73756666696369656e742062616c616e6365000000000000000000000000");

  private static final Bytes PANIC_OVERFLOW =
      Bytes.fromHexString(
          "0x4e487b71" + "0000000000000000000000000000000000000000000000000000000000000011");

  private static final Supplier<Set<String>> CAPTURE_ALL = Collections::emptySet;

  private RingBuffer buffer;
  private RevertMetrics metrics;
  private BlockHeader header;
  private RevertTracer tracer;

  @BeforeEach
  void setUp() {
    buffer = new RingBuffer(100);
    metrics = mock(RevertMetrics.class);
    header = mock(BlockHeader.class);
    when(header.getNumber()).thenReturn(12345L);
    when(header.getBlockHash()).thenReturn(BLOCK_HASH);
    when(header.getTimestamp()).thenReturn(1714670400L);
    tracer = new RevertTracer(header, buffer, metrics, CAPTURE_ALL);
  }

  private static Transaction transaction(final Hash hash, final Address to, final Bytes payload) {
    final Transaction tx = mock(Transaction.class);
    when(tx.getHash()).thenReturn(hash);
    when(tx.getSender()).thenReturn(SENDER);
    // doReturn rather than when().thenReturn() because Transaction.getTo() returns
    // Optional<? extends Address>, which the latter cannot infer cleanly.
    doReturn(Optional.of(to)).when(tx).getTo();
    when(tx.getPayload()).thenReturn(payload);
    return tx;
  }

  private static MessageFrame frameAt(
      final int depth, final MessageFrame.State state, final Address recipient) {
    final MessageFrame frame = mock(MessageFrame.class);
    when(frame.getDepth()).thenReturn(depth);
    when(frame.getState()).thenReturn(state);
    when(frame.getRecipientAddress()).thenReturn(recipient);
    return frame;
  }

  private void traceEnd(
      final Transaction tx, final boolean status, final Bytes output, final long gasUsed) {
    tracer.traceEndTransaction(null, tx, status, output, List.of(), gasUsed, Set.of(), 0L);
  }

  @Test
  void revertedTransactionWithErrorStringIsCaptured() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx, false, ERROR_INSUFFICIENT_BALANCE, 21_000L);

    final RevertRecord record = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    assertThat(record.reasonFormat()).isEqualTo(RevertReasonFormat.ERROR_STRING);
    assertThat(record.decodedReason()).isEqualTo("Insufficient balance");
  }

  @Test
  void revertedTransactionWithPanicIsCaptured() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.EMPTY);
    tracer.traceStartTransaction(null, tx);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx, false, PANIC_OVERFLOW, 30_000L);

    final RevertRecord record = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    assertThat(record.reasonFormat()).isEqualTo(RevertReasonFormat.PANIC_UINT256);
    assertThat(record.decodedReason()).isEqualTo("Arithmetic over/underflow");
  }

  @Test
  void revertedTransactionWithUnknownPayloadIsCapturedAsUnknown() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx, false, Bytes.fromHexString("0xdeadbeef"), 21_000L);

    final RevertRecord record = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    assertThat(record.reasonFormat()).isEqualTo(RevertReasonFormat.UNKNOWN);
    assertThat(record.decodedReason()).isNull();
  }

  @Test
  void successfulTransactionIsNotCaptured() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);
    traceEnd(tx, true, ERROR_INSUFFICIENT_BALANCE, 21_000L);

    assertThat(buffer.size()).isZero();
  }

  @Test
  void failedTransactionWithEmptyOutputIsNotCaptured() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);
    traceEnd(tx, false, Bytes.EMPTY, 21_000L);

    assertThat(buffer.size()).isZero();
  }

  @Test
  void innerCallRevertCapturesDeepestDepth() {
    final Address innerContract =
        Address.fromHexString("0x9999999999999999999999999999999999999999");
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);

    tracer.traceContextExit(frameAt(2, MessageFrame.State.REVERT, innerContract));
    tracer.traceContextExit(frameAt(1, MessageFrame.State.REVERT, CONTRACT));
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, SENDER));

    traceEnd(tx, false, ERROR_INSUFFICIENT_BALANCE, 50_000L);

    final RevertRecord record = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    assertThat(record.callDepth()).isEqualTo(2);
    assertThat(record.contract()).isEqualTo(innerContract.toHexString());
  }

  @Test
  void recordCarriesAllExpectedFields() {
    final Transaction tx =
        transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678aabbccdd"));
    tracer.traceStartTransaction(null, tx);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx, false, ERROR_INSUFFICIENT_BALANCE, 87_432L);

    final RevertRecord record = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    assertThat(record.txHash()).isEqualTo(TX_HASH.toHexString());
    assertThat(record.blockNumber()).isEqualTo(12345L);
    assertThat(record.blockHash()).isEqualTo(BLOCK_HASH.toHexString());
    assertThat(record.contract()).isEqualTo(CONTRACT.toHexString());
    assertThat(record.from()).isEqualTo(SENDER.toHexString());
    assertThat(record.functionSelector()).isEqualTo("0x12345678");
    assertThat(record.calldata()).isEqualTo("0x12345678aabbccdd");
    assertThat(record.reasonFormat()).isEqualTo(RevertReasonFormat.ERROR_STRING);
    assertThat(record.decodedReason()).isEqualTo("Insufficient balance");
    assertThat(record.rawRevertBytes()).isEqualTo(ERROR_INSUFFICIENT_BALANCE.toHexString());
    assertThat(record.gasUsed()).isEqualTo(87_432L);
    assertThat(record.callDepth()).isZero();
    assertThat(record.timestamp()).isEqualTo(1714670400L);
  }

  @Test
  void multipleTransactionsInSameBlockGetIndependentRecords() {
    final Transaction tx1 = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x11111111"));
    tracer.traceStartTransaction(null, tx1);
    tracer.traceContextExit(frameAt(1, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx1, false, ERROR_INSUFFICIENT_BALANCE, 20_000L);

    final Hash tx2Hash =
        Hash.fromHexString("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    final Transaction tx2 = transaction(tx2Hash, CONTRACT, Bytes.fromHexString("0x22222222"));
    tracer.traceStartTransaction(null, tx2);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx2, false, PANIC_OVERFLOW, 30_000L);

    final RevertRecord r1 = buffer.findByTxHash(TX_HASH.toHexString()).orElseThrow();
    final RevertRecord r2 = buffer.findByTxHash(tx2Hash.toHexString()).orElseThrow();
    assertThat(r1.callDepth()).isEqualTo(1);
    assertThat(r2.callDepth()).isZero();
    assertThat(r1.reasonFormat()).isEqualTo(RevertReasonFormat.ERROR_STRING);
    assertThat(r2.reasonFormat()).isEqualTo(RevertReasonFormat.PANIC_UINT256);
  }

  @Test
  void metricsAreUpdatedOnCapture() {
    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    tracer.traceStartTransaction(null, tx);
    tracer.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    traceEnd(tx, false, ERROR_INSUFFICIENT_BALANCE, 87_432L);

    verify(metrics)
        .recordRevert(eq(CONTRACT.toHexString()), eq(RevertReasonFormat.ERROR_STRING), eq(87_432L));
    verify(metrics).recordCaptureOverheadSeconds(anyDouble());
  }

  @Test
  void contractInAllowListIsCaptured() {
    final Supplier<Set<String>> allowOnlyContract = () -> Set.of(CONTRACT.toHexString());
    final RevertTracer filtered = new RevertTracer(header, buffer, metrics, allowOnlyContract);

    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    filtered.traceStartTransaction(null, tx);
    filtered.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    filtered.traceEndTransaction(
        null, tx, false, ERROR_INSUFFICIENT_BALANCE, List.of(), 21_000L, Set.of(), 0L);

    assertThat(buffer.findByTxHash(TX_HASH.toHexString())).isPresent();
  }

  @Test
  void contractNotInAllowListIsSkipped() {
    final Supplier<Set<String>> allowOnlyOther =
        () -> Set.of("0x9999999999999999999999999999999999999999");
    final RevertTracer filtered = new RevertTracer(header, buffer, metrics, allowOnlyOther);

    final Transaction tx = transaction(TX_HASH, CONTRACT, Bytes.fromHexString("0x12345678"));
    filtered.traceStartTransaction(null, tx);
    filtered.traceContextExit(frameAt(0, MessageFrame.State.REVERT, CONTRACT));
    filtered.traceEndTransaction(
        null, tx, false, ERROR_INSUFFICIENT_BALANCE, List.of(), 21_000L, Set.of(), 0L);

    assertThat(buffer.size()).isZero();
  }
}
