package br.com.valls.moneycontrol.infrastructure.persistence.repository;

import br.com.valls.moneycontrol.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByCompanyIdAndSku(UUID companyId, String sku);

    List<ProductJpaEntity> findByCompanyId(UUID companyId);

    boolean existsByCompanyIdAndSku(UUID companyId, String sku);
}
