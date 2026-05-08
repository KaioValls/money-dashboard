package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.SaleEntryRepository;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.SaleEntryJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.SaleEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SaleEntryRepositoryAdapter implements SaleEntryRepository {

    private final SaleEntryJpaRepository jpaRepository;

    @Override
    public SaleEntry save(SaleEntry entry) {
        return toDomain(jpaRepository.save(toEntity(entry)));
    }

    @Override
    public Optional<SaleEntry> findById(UUID id) {
        return jpaRepository.findByUuid(id).map(SaleEntryRepositoryAdapter::toDomain);
    }

    @Override
    public List<SaleEntry> findByStoreAndWeek(UUID storeId, WeekReference week) {
        return jpaRepository.findByStoreIdAndIsoYearAndIsoWeekNumber(
                        storeId, week.year(), week.weekNumber()).stream()
                .map(SaleEntryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<SaleEntry> findByCompanyAndWeek(UUID companyId, WeekReference week) {
        return jpaRepository.findByCompanyIdAndIsoYearAndIsoWeekNumber(
                        companyId, week.year(), week.weekNumber()).stream()
                .map(SaleEntryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<SaleEntry> findByListingAndWeek(UUID listingId, WeekReference week) {
        return jpaRepository.findByListingIdAndIsoYearAndIsoWeekNumber(
                        listingId, week.year(), week.weekNumber()).stream()
                .map(SaleEntryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByListingAndWeek(UUID listingId, WeekReference week) {
        return jpaRepository.existsByListingIdAndIsoYearAndIsoWeekNumber(
                listingId, week.year(), week.weekNumber());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteByUuid(id);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static SaleEntry toDomain(SaleEntryJpaEntity e) {
        return SaleEntry.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .storeId(e.getStoreId())
                .listingId(e.getListingId())
                .marketplaceId(e.getMarketplaceId())
                .taxConfigId(e.getTaxConfigId())
                .feeRuleId(e.getFeeRuleId())
                .dateKey(e.getDateKey())
                .isoYear(e.getIsoYear())
                .isoWeekNumber(e.getIsoWeekNumber())
                .unitsSold(e.getUnitsSold())
                .salePriceSnapshot(e.getSalePriceSnapshot())
                .unitCostSnapshot(e.getUnitCostSnapshot())
                .taxRateSnapshot(e.getTaxRateSnapshot())
                .taxChargeMomentSnapshot(e.getTaxChargeMomentSnapshot())
                .feePercentageSnapshot(e.getFeePercentageSnapshot())
                .fixedFeeSnapshot(e.getFixedFeeSnapshot())
                .adsCost(e.getAdsCost())
                .couponDiscount(e.getCouponDiscount())
                .freightCost(e.getFreightCost())
                .returnAmount(e.getReturnAmount())
                .otherCosts(e.getOtherCosts())
                .otherCostsDescription(e.getOtherCostsDescription())
                .grossRevenue(e.getGrossRevenue())
                .totalCogs(e.getTotalCogs())
                .marketplaceFeeTotal(e.getMarketplaceFeeTotal())
                .taxAmount(e.getTaxAmount())
                .grossProfit(e.getGrossProfit())
                .netProfit(e.getNetProfit())
                .netMarginPercentage(e.getNetMarginPercentage())
                .roiPercentage(e.getRoiPercentage())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private static SaleEntryJpaEntity toEntity(SaleEntry s) {
        return SaleEntryJpaEntity.builder()
                .id(s.getId())
                .companyId(s.getCompanyId())
                .storeId(s.getStoreId())
                .listingId(s.getListingId())
                .marketplaceId(s.getMarketplaceId())
                .taxConfigId(s.getTaxConfigId())
                .feeRuleId(s.getFeeRuleId())
                .dateKey(s.getDateKey())
                .isoYear(s.getIsoYear())
                .isoWeekNumber(s.getIsoWeekNumber())
                .unitsSold(s.getUnitsSold())
                .salePriceSnapshot(s.getSalePriceSnapshot())
                .unitCostSnapshot(s.getUnitCostSnapshot())
                .taxRateSnapshot(s.getTaxRateSnapshot())
                .taxChargeMomentSnapshot(s.getTaxChargeMomentSnapshot())
                .feePercentageSnapshot(s.getFeePercentageSnapshot())
                .fixedFeeSnapshot(s.getFixedFeeSnapshot())
                .adsCost(s.getAdsCost())
                .couponDiscount(s.getCouponDiscount())
                .freightCost(s.getFreightCost())
                .returnAmount(s.getReturnAmount())
                .otherCosts(s.getOtherCosts())
                .otherCostsDescription(s.getOtherCostsDescription())
                .grossRevenue(s.getGrossRevenue())
                .totalCogs(s.getTotalCogs())
                .marketplaceFeeTotal(s.getMarketplaceFeeTotal())
                .taxAmount(s.getTaxAmount())
                .grossProfit(s.getGrossProfit())
                .netProfit(s.getNetProfit())
                .netMarginPercentage(s.getNetMarginPercentage())
                .roiPercentage(s.getRoiPercentage())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
