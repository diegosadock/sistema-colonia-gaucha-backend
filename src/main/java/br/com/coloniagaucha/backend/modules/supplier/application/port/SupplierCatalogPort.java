package br.com.coloniagaucha.backend.modules.supplier.application.port;

import java.util.UUID;

public interface SupplierCatalogPort {
    boolean isActive(UUID supplierId);
}
