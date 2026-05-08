package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.domain.model.Product;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        return toDomain(jpaRepository.save(toEntity(product)));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(ProductRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Product> findByCompanyAndSku(UUID companyId, String sku) {
        return jpaRepository.findByCompanyIdAndSku(companyId, sku)
                .map(ProductRepositoryAdapter::toDomain);
    }

    @Override
    public List<Product> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(ProductRepositoryAdapter::toDomain).toList();
    }

    @Override
    public boolean existsByCompanyIdAndSku(UUID companyId, String sku) {
        return jpaRepository.existsByCompanyIdAndSku(companyId, sku);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static Product toDomain(ProductJpaEntity e) {
        return Product.reconstitute(e.getId(), e.getCompanyId(), e.getSku(), e.getName(),
                e.getCategory(), e.getEanGtin(), e.getWeightGrams(),
                e.isInStock(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static ProductJpaEntity toEntity(Product p) {
        return ProductJpaEntity.builder()
                .id(p.getId())
                .companyId(p.getCompanyId())
                .sku(p.getSku())
                .name(p.getName())
                .category(p.getCategory())
                .eanGtin(p.getEanGtin())
                .weightGrams(p.getWeightGrams())
                .inStock(p.isInStock())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
