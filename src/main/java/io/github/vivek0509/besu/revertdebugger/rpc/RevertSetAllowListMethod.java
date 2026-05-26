package io.github.vivek0509.besu.revertdebugger.rpc;

import io.github.vivek0509.besu.revertdebugger.tracer.RevertTracerProvider;

import org.hyperledger.besu.plugin.services.rpc.PluginRpcRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Handler for {@code revert_setAllowList}. Atomically replaces the live contract allow-list. */
public class RevertSetAllowListMethod {

  private final Supplier<RevertTracerProvider> providerSupplier;

  public RevertSetAllowListMethod(final Supplier<RevertTracerProvider> providerSupplier) {
    this.providerSupplier = providerSupplier;
  }

  public int execute(final PluginRpcRequest request) {
    final Object[] params = request.getParams();
    if (params == null || params.length != 1 || params[0] == null) {
      throw new IllegalArgumentException(
          "revert_setAllowList expects exactly one parameter: an array of contract addresses");
    }
    if (!(params[0] instanceof Collection<?>)) {
      throw new IllegalArgumentException(
          "revert_setAllowList: parameter must be a JSON array of contract addresses");
    }
    final List<String> addresses = new ArrayList<>();
    for (final Object element : (Collection<?>) params[0]) {
      if (element == null) {
        throw new IllegalArgumentException("revert_setAllowList: address entries cannot be null");
      }
      addresses.add(element.toString());
    }
    providerSupplier.get().setContractAllowList(addresses);
    return addresses.size();
  }
}
