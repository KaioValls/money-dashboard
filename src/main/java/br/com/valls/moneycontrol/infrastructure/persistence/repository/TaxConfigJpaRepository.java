package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.TaxConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxConfigJpaRepository extends JpaRepository<TaxConfigJpaEntity, UUID> {

    /**
     * Retorna a configuração tributária ativa para a empresa na data informada.
     * Ativa = valid_from <= date AND (valid_until IS NULL OR valid_until >= date)
     */
    @Query("""
            SELECT t FROM TaxConfigJpaEntity t
            WHERE t.companyId = :companyId
              AND t.validFrom <= :date
              AND (t.validUntil IS NULL OR t.validUntil >= :date)
              AND t.active = true
            ORDER BY t.validFrom DESC
            LIMIT 1
            """)
    Optional<TaxConfigJpaEntity> findActiveAt(
            @Param("companyId") UUID companyId,
            @Param("date") LocalDate date);

    /**
     * Retorna config em aberto (valid_until IS NULL) da empresa — usada para
     * auto-fechamento ao criar nova configuração.
     */
    @Query("""
            SELECT t FROM TaxConfigJpaEntity t
            WHERE t.companyId = :companyId
              AND t.validUntil IS NULL
              AND t.active = true
            """)
    List<TaxConfigJpaEntity> findOpenByCompanyId(@Param("companyId") UUID companyId);

    List<TaxConfigJpaEntity> findByCompanyId(UUID companyId);
}
