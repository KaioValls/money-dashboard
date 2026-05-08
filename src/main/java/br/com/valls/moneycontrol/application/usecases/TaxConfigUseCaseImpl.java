package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.TaxConfigUseCase;
import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.TaxConfigRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterTaxConfigCommand;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.TaxConfigNotFoundException;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do use case de configuração tributária.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 *
 * <p>Regra de negócio: nunca duas configs ativas para o mesmo CNPJ.
 * Ao criar nova config, a anterior com vigência em aberto é fechada automaticamente.</p>
 */
@RequiredArgsConstructor
public class TaxConfigUseCaseImpl implements TaxConfigUseCase {

    private final TaxConfigRepository taxConfigRepository;
    private final CompanyRepository   companyRepository;

    @Override
    public TaxConfig createConfig(RegisterTaxConfigCommand command) {
        companyRepository.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        // Fechar config anterior com vigência em aberto
        taxConfigRepository.findOpenByCompanyId(command.companyId())
                .ifPresent(existing -> {
                    // valid_until = validFrom - 1 dia
                    LocalDate closingDate = command.validFrom().minusDays(1);
                    existing.closeAt(closingDate);
                    taxConfigRepository.save(existing);
                });

        TaxConfig newConfig = TaxConfig.create(
                command.companyId(),
                command.chargeMoment(),
                command.calculationBase(),
                command.ratePercentage(),
                command.validFrom()
        );
        return taxConfigRepository.save(newConfig);
    }

    @Override
    public Optional<TaxConfig> findActiveByCompanyAndDate(UUID companyId, LocalDate date) {
        return taxConfigRepository.findActiveAt(companyId, date);
    }

    @Override
    public List<TaxConfig> findAllByCompany(UUID companyId) {
        return List.copyOf(taxConfigRepository.findByCompanyId(companyId));
    }

    @Override
    public void deactivate(UUID id) {
        TaxConfig config = taxConfigRepository.findById(id)
                .orElseThrow(() -> new TaxConfigNotFoundException(id));
        config.deactivate();
        taxConfigRepository.save(config);
    }
}
