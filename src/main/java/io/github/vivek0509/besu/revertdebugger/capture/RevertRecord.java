package io.github.vivek0509.besu.revertdebugger.capture;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

/**
 * One captured EVM transaction revert. {@code decodedReason} is null when {@code reasonFormat} is
 * {@link RevertReasonFormat#UNKNOWN}.
 */
public record RevertRecord(
    String txHash,
    long blockNumber,
    String blockHash,
    String contract,
    String from,
    String functionSelector,
    String calldata,
    RevertReasonFormat reasonFormat,
    String decodedReason,
    String rawRevertBytes,
    long gasUsed,
    int callDepth,
    long timestamp) {}
