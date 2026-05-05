package io.github.vivek0509.besu.revertdebugger.tracer;

import io.github.vivek0509.besu.revertdebugger.capture.RevertRecord;
import io.github.vivek0509.besu.revertdebugger.capture.RingBuffer;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonDecoder;
import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonDecoder.Decoded;
import io.github.vivek0509.besu.revertdebugger.metrics.RevertMetrics;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Log;
import org.hyperledger.besu.datatypes.Transaction;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.worldstate.WorldView;
import org.hyperledger.besu.plugin.data.BlockHeader;
import org.hyperledger.besu.plugin.services.tracer.BlockAwareOperationTracer;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;

/**
 * Block-import tracer that detects reverts during EVM execution and assembles {@link RevertRecord}
 * entries into the ring buffer. One instance per imported block; the same instance handles every
 * transaction in the block, so per-transaction state is reset on {@link #traceStartTransaction}.
 *
 * <p>Capture happens in two phases:
 *
 * <ol>
 *   <li>{@link #traceContextExit(MessageFrame)} fires for every call frame as it exits. The first
 *       exit with {@code state == REVERT} is the deepest revert site (in a bubble chain, inner
 *       frames exit before outer ones). We record its depth and recipient address there.
 *   <li>{@link #traceEndTransaction} sees the final transaction state. If {@code status == false}
 *       and {@code output} is non-empty, this is a revert: we decode the output, build the {@link
 *       RevertRecord} using the captured depth and contract, push to the buffer, and update
 *       metrics.
 * </ol>
 *
 * <p>The {@code --plugin-revert-capture-depth} flag is parsed but does not yet alter behaviour.
 * Decision 13 in {@code decisions.md} covers why.
 */
public class RevertTracer implements BlockAwareOperationTracer {

  private static final int UNCAPTURED = -1;

  private final BlockHeader blockHeader;
  private final RingBuffer ringBuffer;
  private final RevertMetrics metrics;
  private final Supplier<Set<String>> contractAllowListSupplier;

  private int revertDepth = UNCAPTURED;
  private String revertContract;

  public RevertTracer(
      final BlockHeader blockHeader,
      final RingBuffer ringBuffer,
      final RevertMetrics metrics,
      final Supplier<Set<String>> contractAllowListSupplier) {
    this.blockHeader = blockHeader;
    this.ringBuffer = ringBuffer;
    this.metrics = metrics;
    this.contractAllowListSupplier = contractAllowListSupplier;
  }

  @Override
  public void traceStartTransaction(final WorldView worldView, final Transaction transaction) {
    revertDepth = UNCAPTURED;
    revertContract = null;
  }

  @Override
  public void traceContextExit(final MessageFrame frame) {
    if (revertDepth == UNCAPTURED && frame.getState() == MessageFrame.State.REVERT) {
      revertDepth = frame.getDepth();
      revertContract = frame.getRecipientAddress().toHexString();
    }
  }

  @Override
  public void traceEndTransaction(
      final WorldView worldView,
      final Transaction tx,
      final boolean status,
      final Bytes output,
      final List<Log> logs,
      final long gasUsed,
      final Set<Address> selfDestructs,
      final long timeNs) {

    if (status || output == null || output.isEmpty()) {
      return;
    }

    final long captureStartNanos = System.nanoTime();

    final Decoded decoded = RevertReasonDecoder.decode(output);

    final String contract =
        revertContract != null ? revertContract : tx.getTo().map(Address::toHexString).orElse("0x");
    final int callDepth = revertDepth == UNCAPTURED ? 0 : revertDepth;

    final Set<String> allowList = contractAllowListSupplier.get();
    if (!allowList.isEmpty() && !allowList.contains(contract)) {
      return;
    }

    final Bytes payload = tx.getPayload();
    final String functionSelector = payload.size() >= 4 ? payload.slice(0, 4).toHexString() : "0x";

    final RevertRecord record =
        new RevertRecord(
            tx.getHash().toHexString(),
            blockHeader.getNumber(),
            blockHeader.getBlockHash().toHexString(),
            contract,
            tx.getSender().toHexString(),
            functionSelector,
            payload.toHexString(),
            decoded.format(),
            decoded.reason(),
            output.toHexString(),
            gasUsed,
            callDepth,
            blockHeader.getTimestamp());

    ringBuffer.add(record);
    metrics.recordRevert(contract, decoded.format(), gasUsed);
    metrics.recordCaptureOverheadSeconds((System.nanoTime() - captureStartNanos) / 1e9);
  }
}
