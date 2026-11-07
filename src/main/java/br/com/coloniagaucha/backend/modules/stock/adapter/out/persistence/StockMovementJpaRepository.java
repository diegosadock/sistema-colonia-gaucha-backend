package br.com.coloniagaucha.backend.modules.stock.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, UUID>, JpaSpecificationExecutor<StockMovementJpaEntity> {
    @Query(value = "select coalesce(sum(case when movement_type = 'ENTRY' then quantity when movement_type = 'EXIT' then -quantity else quantity end), 0) from stock_movements where product_id = :productId", nativeQuery = true)
    BigDecimal balance(@Param("productId") UUID productId);

    boolean existsByProductId(UUID productId);

    @Query(value = """
            select p.id as \"productId\",
                   coalesce(sum(case when m.movement_type = 'ENTRY' then m.quantity
                                     when m.movement_type = 'EXIT' then -m.quantity
                                     else m.quantity end), 0) as \"quantity\"
              from products p
              left join stock_movements m on m.product_id = p.id
             group by p.id
            """,
            countQuery = "select count(*) from products",
            nativeQuery = true)
    Page<StockBalanceProjection> balances(Pageable pageable);
}
