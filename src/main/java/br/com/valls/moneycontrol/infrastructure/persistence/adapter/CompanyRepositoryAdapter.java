package br.com.valls.moneycontrol.infrastructure.persistence.adapter;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.domain.model.Company;
import br.com.valls.moneycontrol.infrastructure.persistence.entity.CompanyJpaEntity;
import br.com.valls.moneycontrol.infrastructure.persistence.repository.CompanyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyJpaRepository jpaRepository;

    @Override
    public Company save(Company company) {
        return toDomain(jpaRepository.save(toEntity(company)));
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return jpaRepository.findById(id).map(CompanyRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Company> findByCnpjDigits(String cnpjDigits) {
        return jpaRepository.findByCnpjDigits(cnpjDigits)
                .map(CompanyRepositoryAdapter::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return jpaRepository.findAll().stream()
                .map(CompanyRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCnpjDigits(String cnpjDigits) {
        return jpaRepository.existsByCnpjDigits(cnpjDigits);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private static Company toDomain(CompanyJpaEntity e) {
        return Company.reconstitute(
                e.getId(), e.getCnpjDigits(), e.getLegalName(),
                e.getTradeName(), e.getTaxRegime(), e.getSimplesAnnex(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static CompanyJpaEntity toEntity(Company c) {
        return CompanyJpaEntity.builder()
                .id(c.getId())
                .cnpjDigits(c.getCnpjDigits())
                .legalName(c.getLegalName())
                .tradeName(c.getTradeName())
                .taxRegime(c.getTaxRegime())
                .simplesAnnex(c.getSimplesAnnex())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
