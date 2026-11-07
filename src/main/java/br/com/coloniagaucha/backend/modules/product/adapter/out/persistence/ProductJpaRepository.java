package br.com.coloniagaucha.backend.modules.product.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID>, JpaSpecificationExecutor<ProductJpaEntity> {
    Optional<ProductJpaEntity> findByNameKey(String nameKey);

    List<ProductJpaEntity> findAllByIdIn(Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductJpaEntity product where product.id = :id")
    Optional<ProductJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductJpaEntity product where product.id = :id and product.active = true")
    Optional<ProductJpaEntity> findActiveByIdForUpdate(@Param("id") UUID id);
}
