package io.github.vivek0509.besu.revertdebugger.capture;

import io.github.vivek0509.besu.revertdebugger.decode.RevertReasonFormat;

/**
 * One captured EVM transaction revert. Component order matches the spec's JSON shape so Jackson's
 * default serialization (which respects record declaration order) produces the wire format
 * directly.
 *
 * <p>Hex-encoded fields are typed as {@link String} rather than Besu's {@code Hash} or {@code
 * Address}: the producer (the tracer in commit 9) converts at capture time, and consumers (the
 * JSON-RPC methods) want hex strings on the wire anyway. Decision 8 in {@code decisions.md} covers
 * the trade-off.
 *
 * <p>{@code decodedReason} is nullable: it carries a human-readable string when {@code
 * reasonFormat} is {@link RevertReasonFormat#ERROR_STRING} or {@link
 * RevertReasonFormat#PANIC_UINT256}, and {@code null} when {@link RevertReasonFormat#UNKNOWN}.
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
