package br.com.coloniagaucha.backend.modules.supplier.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, UUID>, JpaSpecificationExecutor<SupplierJpaEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select supplier from SupplierJpaEntity supplier where supplier.id = :id")
    Optional<SupplierJpaEntity> findByIdForUpdate(@Param("id") UUID id);
    Optional<SupplierJpaEntity> findFirstByIdAndActiveTrue(UUID id);
}
