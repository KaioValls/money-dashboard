package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterCompanyCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateCompanyCommand;
import br.com.valls.moneycontrol.domain.model.Company;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de empresas (CNPJ).
 * Implementado exclusivamente por {@code CompanyUseCaseImpl} e instanciado via {@code BeanConfig}.
 */
public interface CompanyUseCase {

    /** Registra uma nova empresa. Valida CNPJ e unicidade. */
    Company register(RegisterCompanyCommand command);

    /** Retorna a empresa pelo ID ou lança {@code CompanyNotFoundException}. */
    Company findById(UUID id);

    /** Retorna todas as empresas ativas como lista imutável. */
    List<Company> findAll();

    /** Atualiza campos não-nulos da empresa. */
    Company update(UUID id, UpdateCompanyCommand command);

    /** Desativa a empresa (soft delete). */
    void deactivate(UUID id);
}
