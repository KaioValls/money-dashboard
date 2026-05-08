package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.StoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreJpaEntity, UUID> {

    List<StoreJpaEntity> findByCompanyId(UUID companyId);

    List<StoreJpaEntity> findByCompanyIdAndStatus(UUID companyId, StoreStatus status);

    List<StoreJpaEntity> findByMarketplaceId(UUID marketplaceId);
}
