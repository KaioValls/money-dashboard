package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.ProductPurchaseEntryRepository;
import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.ProductPurchaseEntryJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.ProductPurchaseEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductPurchaseEntryRepositoryAdapter implements ProductPurchaseEntryRepository {

    private final ProductPurchaseEntryJpaRepository jpaRepository;

    @Override
    public ProductPurchaseEntry save(ProductPurchaseEntry entry) {
        return toDomain(jpaRepository.save(toEntity(entry)));
    }

    @Override
    public Optional<ProductPurchaseEntry> findById(UUID id) {
        return jpaRepository.findByUuid(id).map(ProductPurchaseEntryRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ProductPurchaseEntry> findMostRecentBeforeWeek(UUID productId, WeekReference week) {
        return jpaRepository.findMostRecentBeforeWeek(productId, week.year(), week.weekNumber())
                .map(ProductPurchaseEntryRepositoryAdapter::toDomain);
    }

    @Override
    public List<ProductPurchaseEntry> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId).stream()
                .map(ProductPurchaseEntryRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ProductPurchaseEntry> findByCompanyAndWeek(UUID companyId, WeekReference week) {
        return jpaRepository.findByCompanyIdAndIsoYearAndIsoWeekNumber(
                        companyId, week.year(), week.weekNumber()).stream()
                .map(ProductPurchaseEntryRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static ProductPurchaseEntry toDomain(ProductPurchaseEntryJpaEntity e) {
        return ProductPurchaseEntry.reconstitute(e.getId(), e.getCompanyId(), e.getProductId(),
                e.getIsoYear(), e.getIsoWeekNumber(), e.getQuantity(), e.getUnitCost(),
                e.isHasFreight(), e.getFreightCost(), e.getSupplierName(), e.getInvoiceNumber(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static ProductPurchaseEntryJpaEntity toEntity(ProductPurchaseEntry e) {
        return ProductPurchaseEntryJpaEntity.builder()
                .id(e.getId())
                .companyId(e.getCompanyId())
                .productId(e.getProductId())
                .isoYear(e.getIsoYear())
                .isoWeekNumber(e.getIsoWeekNumber())
                .quantity(e.getQuantity())
                .unitCost(e.getUnitCost())
                .totalCost(e.totalCost())
                .hasFreight(e.isHasFreight())
                .freightCost(e.getFreightCost())
                .freightPerUnit(e.freightPerUnit())
                .totalLandedCost(e.totalLandedCost())
                .supplierName(e.getSupplierName())
                .invoiceNumber(e.getInvoiceNumber())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
