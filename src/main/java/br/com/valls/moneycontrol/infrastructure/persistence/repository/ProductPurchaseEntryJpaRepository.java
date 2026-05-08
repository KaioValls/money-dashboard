package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.ProductPurchaseEntryId;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.ProductPurchaseEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPurchaseEntryJpaRepository
        extends JpaRepository<ProductPurchaseEntryJpaEntity, ProductPurchaseEntryId> {

    /**
     * Busca por UUID ignorando o iso_year (necessário para operação findById do port).
     */
    @Query("SELECT e FROM ProductPurchaseEntryJpaEntity e WHERE e.id = :id")
    Optional<ProductPurchaseEntryJpaEntity> findByUuid(@Param("id") UUID id);

    /**
     * Retorna a compra mais recente do produto anterior à semana de referência.
     * Usado para capturar unit_cost_snapshot ao registrar venda.
     */
    @Query("""
            SELECT e FROM ProductPurchaseEntryJpaEntity e
            WHERE e.productId = :productId
              AND (e.isoYear < :year OR (e.isoYear = :year AND e.isoWeekNumber < :weekNumber))
            ORDER BY e.isoYear DESC, e.isoWeekNumber DESC
            LIMIT 1
            """)
    Optional<ProductPurchaseEntryJpaEntity> findMostRecentBeforeWeek(
            @Param("productId") UUID productId,
            @Param("year") int year,
            @Param("weekNumber") int weekNumber);

    List<ProductPurchaseEntryJpaEntity> findByProductId(UUID productId);

    List<ProductPurchaseEntryJpaEntity> findByCompanyIdAndIsoYearAndIsoWeekNumber(
            UUID companyId, int isoYear, int isoWeekNumber);
}
