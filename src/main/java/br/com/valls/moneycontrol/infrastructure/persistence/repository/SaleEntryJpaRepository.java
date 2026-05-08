package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.SaleEntryId;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.SaleEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleEntryJpaRepository extends JpaRepository<SaleEntryJpaEntity, SaleEntryId> {

    /**
     * Busca por UUID ignorando o iso_year (necessário para operação findById do port).
     */
    @Query("SELECT e FROM SaleEntryJpaEntity e WHERE e.id = :id")
    Optional<SaleEntryJpaEntity> findByUuid(@Param("id") UUID id);

    List<SaleEntryJpaEntity> findByStoreIdAndIsoYearAndIsoWeekNumber(
            UUID storeId, int isoYear, int isoWeekNumber);

    List<SaleEntryJpaEntity> findByCompanyIdAndIsoYearAndIsoWeekNumber(
            UUID companyId, int isoYear, int isoWeekNumber);

    List<SaleEntryJpaEntity> findByListingIdAndIsoYearAndIsoWeekNumber(
            UUID listingId, int isoYear, int isoWeekNumber);

    boolean existsByListingIdAndIsoYearAndIsoWeekNumber(
            UUID listingId, int isoYear, int isoWeekNumber);

    @Modifying
    @Query("DELETE FROM SaleEntryJpaEntity e WHERE e.id = :id")
    void deleteByUuid(@Param("id") UUID id);
}
