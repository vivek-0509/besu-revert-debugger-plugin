# Design

## Plugin name and identity

The plugin registers with Besu as `RevertDebugger`. The distribution archive is `besu-revert-debugger-plugin-<version>.zip`.

## CLI flags

Flags are registered under the namespace `plugin-revert`. To be documented per flag as they are added.

## JSON-RPC methods

Custom methods are registered under the `revert_*` namespace via `RpcEndpointService`. To be documented per method as they are added.

## Metrics

Custom metric category `PLUGIN_REVERT` with category prefix `plugin_`. Metric names are documented as they are created.

## Plugin API services consumed

Six services from `org.hyperledger.besu.plugin.services`:

- `BlockImportTracerProvider` (the centerpiece, registered via `ServiceManager.addService`)
- `RpcEndpointService`
- `MetricsSystem`
- `MetricCategoryRegistry`
- `PicoCLIOptions`
- `BesuConfiguration`

The plugin does not consume any other Plugin API service.
