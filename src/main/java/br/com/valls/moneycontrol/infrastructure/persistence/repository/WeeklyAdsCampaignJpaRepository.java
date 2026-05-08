package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.WeeklyAdsCampaignJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyAdsCampaignJpaRepository
        extends JpaRepository<WeeklyAdsCampaignJpaEntity, UUID> {

    Optional<WeeklyAdsCampaignJpaEntity> findByStoreIdAndIsoYearAndIsoWeekNumber(
            UUID storeId, int isoYear, int isoWeekNumber);

    List<WeeklyAdsCampaignJpaEntity> findByStoreIdOrderByIsoYearDescIsoWeekNumberDesc(
            UUID storeId);
}
