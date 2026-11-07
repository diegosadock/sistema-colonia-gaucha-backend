package br.com.coloniagaucha.backend.modules.stock.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockBalanceProjection {
    UUID getProductId();
    BigDecimal getQuantity();
}
