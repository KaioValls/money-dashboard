package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterTaxConfigCommand;
import br.com.valls.moneycontrol.domain.model.TaxConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de configurações tributárias.
 */
public interface TaxConfigUseCase {

    /**
     * Cria nova configuração tributária para a empresa.
     * Se houver config ativa com vigência em aberto, fecha-a automaticamente
     * (valid_until = validFrom - 1 dia) antes de criar a nova.
     */
    TaxConfig createConfig(RegisterTaxConfigCommand command);

    /**
     * Retorna a config tributária ativa para a empresa na data informada.
     * Critério: valid_from <= date AND (valid_until IS NULL OR valid_until >= date).
     */
    Optional<TaxConfig> findActiveByCompanyAndDate(UUID companyId, LocalDate date);

    /** Retorna todo o histórico de configs tributárias de uma empresa. */
    List<TaxConfig> findAllByCompany(UUID companyId);

    /** Desativa a config sem alterar a vigência. */
    void deactivate(UUID id);
}
