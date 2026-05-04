# besu-revert-debugger-plugin

A Hyperledger Besu plugin that captures EVM transaction reverts during block import and exposes them via JSON-RPC and Prometheus metrics.

## Status

Early development. CLI flags, JSON-RPC methods, and metric names are documented in `DESIGN.md` as they are added.

## Build

Requires Java 21 or later.

```
./gradlew build
```

The plugin distribution is produced under `build/distributions/`.
