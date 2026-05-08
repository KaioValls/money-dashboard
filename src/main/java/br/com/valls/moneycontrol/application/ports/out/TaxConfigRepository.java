package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.TaxConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de configurações tributárias.
 */
public interface TaxConfigRepository {

    /** Persiste ou atualiza uma configuração tributária. */
    TaxConfig save(TaxConfig taxConfig);

    /** Busca config tributária pelo ID. */
    Optional<TaxConfig> findById(UUID id);

    /**
     * Busca a config tributária ativa para a empresa na data informada.
     * Critério: valid_from <= date AND (valid_until IS NULL OR valid_until >= date)
     * ORDER BY valid_from DESC LIMIT 1.
     */
    Optional<TaxConfig> findActiveAt(UUID companyId, LocalDate date);

    /** Retorna todo o histórico de configs tributárias de uma empresa. */
    List<TaxConfig> findByCompanyId(UUID companyId);

    /**
     * Busca a config com vigência em aberto (valid_until IS NULL) para a empresa.
     * Usada para fechar automaticamente antes de criar nova config.
     */
    Optional<TaxConfig> findOpenByCompanyId(UUID companyId);
}
