package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.ListingPriceHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingPriceHistoryJpaRepository extends JpaRepository<ListingPriceHistoryJpaEntity, UUID> {

    List<ListingPriceHistoryJpaEntity> findByListingId(UUID listingId);
}
