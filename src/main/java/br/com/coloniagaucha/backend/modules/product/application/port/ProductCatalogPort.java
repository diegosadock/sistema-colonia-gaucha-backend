package br.com.coloniagaucha.backend.modules.product.application.port;

import java.util.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Shared read/locking contract used by stock; it does not expose product persistence. */
public interface ProductCatalogPort {
    Optional<ProductReference> lockActiveProduct(UUID productId);
    Map<UUID, String> unitCodes(Collection<UUID> productIds);
    record ProductReference(UUID id, String unitCode) { }
}
