package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceFeeRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MarketplaceFeeRuleJpaRepository extends JpaRepository<MarketplaceFeeRuleJpaEntity, UUID> {

    /**
     * Retorna todas as regras de taxa ativas do marketplace em uma data.
     * Ativa = valid_from <= date AND (valid_until IS NULL OR valid_until >= date)
     */
    @Query("""
            SELECT r FROM MarketplaceFeeRuleJpaEntity r
            WHERE r.marketplaceId = :marketplaceId
              AND r.validFrom <= :date
              AND (r.validUntil IS NULL OR r.validUntil >= :date)
              AND r.active = true
            ORDER BY r.priority DESC
            """)
    List<MarketplaceFeeRuleJpaEntity> findActiveAt(
            @Param("marketplaceId") UUID marketplaceId,
            @Param("date") LocalDate date);

    List<MarketplaceFeeRuleJpaEntity> findByMarketplaceId(UUID marketplaceId);
}
