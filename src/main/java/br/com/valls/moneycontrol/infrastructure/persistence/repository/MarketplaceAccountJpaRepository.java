package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketplaceAccountJpaRepository extends JpaRepository<MarketplaceAccountJpaEntity, UUID> {

    Optional<MarketplaceAccountJpaEntity> findByCompanyIdAndMarketplaceId(
            UUID companyId, UUID marketplaceId);
}
