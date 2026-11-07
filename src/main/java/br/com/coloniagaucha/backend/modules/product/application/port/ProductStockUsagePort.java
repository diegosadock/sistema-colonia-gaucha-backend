package br.com.coloniagaucha.backend.modules.product.application.port;

import java.util.UUID;

/** Cross-module guard that prevents changing a product's unit after stock history exists. */
public interface ProductStockUsagePort {
    boolean hasMovements(UUID productId);
}
