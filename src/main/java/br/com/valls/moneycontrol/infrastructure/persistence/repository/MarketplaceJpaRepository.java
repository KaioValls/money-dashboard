package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.MarketplaceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketplaceJpaRepository extends JpaRepository<MarketplaceJpaEntity, UUID> {

    Optional<MarketplaceJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
