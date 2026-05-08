package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.ListingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingJpaRepository extends JpaRepository<ListingJpaEntity, UUID> {

    List<ListingJpaEntity> findByStoreId(UUID storeId);

    List<ListingJpaEntity> findByProductId(UUID productId);
}
