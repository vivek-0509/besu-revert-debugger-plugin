# besu-revert-debugger-plugin

A Hyperledger Besu plugin that captures EVM transaction reverts during block import and exposes them via JSON-RPC and Prometheus metrics.

The plugin is a node-local diagnostic tool. It attaches an operation tracer to the EVM, records every reverted transaction into an in-memory ring buffer, decodes Solidity's two well-known revert formats (`Error(string)` and `Panic(uint256)`), preserves raw bytes for everything else, and exposes the captured records over a custom JSON-RPC namespace and Prometheus metrics.

## Build

Requires Java 21 or later.

```
./gradlew build
```

The plugin distribution is produced at `build/distributions/besu-revert-debugger-plugin-0.1.0.zip`. The archive contains the plugin jar plus any third-party dependencies that Besu does not already provide at runtime.

## Install

```
unzip -j build/distributions/besu-revert-debugger-plugin-0.1.0.zip -d $BESU_HOME/plugins/
```

Then start Besu with `--metrics-category=REVERT` enabled if you want the metrics scraped.

## CLI flags

All flags are registered under the `--plugin-revert-*` prefix.

| Flag | Default | Purpose |
|---|---|---|
| `--plugin-revert-enabled` | `true` | Master toggle. When `false`, the tracer becomes a no-op and no records are captured. |
| `--plugin-revert-buffer-size` | `10000` | In-memory ring buffer capacity. Once full, the oldest record is evicted on each new capture. |
| `--plugin-revert-contracts` | `""` | Comma-separated contract addresses to capture. Empty means capture all contracts. Case-insensitive. |

## JSON-RPC methods

The plugin contributes three methods under the `revert_` namespace.

### `revert_inspect`

Look up a captured revert record by transaction hash.

Parameters: a single 32-byte transaction hash as a hex string.

Returns: a `RevertRecord` object, or `null` if the transaction was not captured (either it did not revert, or the buffer no longer contains it).

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"revert_inspect","params":["0xabc123..."],"id":1}' \
  http://localhost:8545
```

### `revert_recent`

Return the most recent captured records, optionally filtered by contract or reason format.

Parameters:
- `limit` (required): integer, maximum number of records to return
- `contractFilter` (optional): contract address; null, missing, or empty string mean no filter
- `reasonFilter` (optional): one of `"Error(string)"`, `"Panic(uint256)"`, `"Unknown"`; null, missing, or empty string mean no filter

Returns: an array of `RevertRecord` objects, ordered newest-first.

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"revert_recent","params":["10", "0xaabb...", "Error(string)"],"id":1}' \
  http://localhost:8545
```

### `revert_stats`

Aggregate captured reverts over a time window.

Parameters: a single non-negative integer `windowSeconds`. Records whose timestamp falls within `[now - windowSeconds, now]` are included.

Returns: an object with three fields:
- `perContract`: map from contract address to revert count
- `perReasonFormat`: map from reason format display name to revert count
- `total`: total revert count in the window

```
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"revert_stats","params":["3600"],"id":1}' \
  http://localhost:8545
```

## RevertRecord shape

```json
{
  "txHash":           "0x...",
  "blockNumber":      12345,
  "blockHash":        "0x...",
  "contract":         "0x...",
  "from":             "0x...",
  "functionSelector": "0x12345678",
  "calldata":         "0x...",
  "reasonFormat":     "Error(string)",
  "decodedReason":    "Insufficient balance",
  "rawRevertBytes":   "0x...",
  "gasUsed":          87432,
  "callDepth":        3,
  "timestamp":        1714670400
}
```

`reasonFormat` is one of `"Error(string)"`, `"Panic(uint256)"`, or `"Unknown"`. `decodedReason` is the decoded human-readable string for the first two formats, and `null` for `"Unknown"`. The raw bytes always survive in `rawRevertBytes` so downstream tooling can attempt its own decoding for custom errors and bare `revert()` calls.

## Prometheus metrics

Registered under the metric category `REVERT` with application prefix `plugin_`. Enable scrape exposure with `--metrics-category=REVERT`. Besu composes the rendered Prometheus name as `applicationPrefix + categoryName + "_" + metricShortName`, so the table below shows the rendered names.

| Metric | Type | Labels | Description |
|---|---|---|---|
| `plugin_revert_count_total` | counter | `contract`, `reason_format` | Total reverted transactions captured. |
| `plugin_revert_gas_used_total` | counter | `contract` | Total gas used by reverted transactions. |
| `plugin_revert_buffer_depth` | gauge | none | Current depth of the in-memory ring buffer. |
| `plugin_revert_capture_overhead_seconds` | histogram | none | Tracer capture overhead per record, in seconds. Buckets: 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05. |

## Out of scope

The plugin is deliberately scoped to in-memory capture and standard-format decoding. Out of scope:

- Custom error decoding via ABI files or external selector databases.
- Persistent storage of any kind.
- Webhook, Slack, or Discord alerting.
- World state capture at the revert point.
- Pre-execution simulation.

Custom errors and bare `revert()` calls are tagged `Unknown` with `rawRevertBytes` preserved; downstream tooling can decode them externally.

## Plugin API surface

The plugin touches six Plugin API surfaces.

Consumed (looked up via `ServiceManager.getService` during the plugin's lifecycle):

- `PicoCLIOptions`: used during `register()` to add the three `--plugin-revert-*` CLI flags.
- `MetricCategoryRegistry`: used during `register()` to register the custom `REVERT` metric category.
- `RpcEndpointService`: used during `register()` to wire the three `revert_*` JSON-RPC methods.
- `MetricsSystem`: used during `start()` to create the four metric handles (two counters, one gauge, one histogram).

Published (an implementation we register so Besu uses it):

- `BlockImportTracerProvider`: registered via `ServiceManager.addService` during `start()`. The plugin's `RevertTracerProvider` becomes Besu's block-import tracer source.

Lifecycle entry point:

- `ServiceManager`: received as the parameter to `register(...)`. Stashed in a field and used throughout for `getService` and `addService` calls; the dispatcher through which every other Plugin API surface above is reached.
