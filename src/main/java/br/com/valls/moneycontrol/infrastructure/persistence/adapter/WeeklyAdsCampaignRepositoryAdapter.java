package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.WeeklyAdsCampaignRepository;
import br.com.valls.moneycontrol.domain.model.WeeklyAdsCampaign;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.WeeklyAdsCampaignJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.WeeklyAdsCampaignJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WeeklyAdsCampaignRepositoryAdapter implements WeeklyAdsCampaignRepository {

    private final WeeklyAdsCampaignJpaRepository jpaRepository;

    @Override
    public WeeklyAdsCampaign save(WeeklyAdsCampaign campaign) {
        return toDomain(jpaRepository.save(toEntity(campaign)));
    }

    @Override
    public Optional<WeeklyAdsCampaign> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(WeeklyAdsCampaignRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<WeeklyAdsCampaign> findByStoreAndWeek(UUID storeId, WeekReference week) {
        return jpaRepository.findByStoreIdAndIsoYearAndIsoWeekNumber(
                        storeId, week.year(), week.weekNumber())
                .map(WeeklyAdsCampaignRepositoryAdapter::toDomain);
    }

    @Override
    public List<WeeklyAdsCampaign> findByStoreId(UUID storeId) {
        return jpaRepository.findByStoreIdOrderByIsoYearDescIsoWeekNumberDesc(storeId).stream()
                .map(WeeklyAdsCampaignRepositoryAdapter::toDomain).toList();
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static WeeklyAdsCampaign toDomain(WeeklyAdsCampaignJpaEntity e) {
        return WeeklyAdsCampaign.reconstitute(e.getId(), e.getStoreId(),
                e.getIsoYear(), e.getIsoWeekNumber(), e.getTotalAdsBudget(),
                e.getRatioMethod(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static WeeklyAdsCampaignJpaEntity toEntity(WeeklyAdsCampaign c) {
        return WeeklyAdsCampaignJpaEntity.builder()
                .id(c.getId())
                .storeId(c.getStoreId())
                .isoYear(c.getIsoYear())
                .isoWeekNumber(c.getIsoWeekNumber())
                .totalAdsBudget(c.getTotalAdsBudget())
                .ratioMethod(c.getRatioMethod())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
