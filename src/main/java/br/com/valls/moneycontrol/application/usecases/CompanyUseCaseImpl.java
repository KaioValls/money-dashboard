package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.CompanyUseCase;
import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterCompanyCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateCompanyCommand;
import br.com.valls.moneycontrol.domain.exception.BusinessException;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de empresas.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class CompanyUseCaseImpl implements CompanyUseCase {

    private final CompanyRepository companyRepository;

    @Override
    public Company register(RegisterCompanyCommand command) {
        if (companyRepository.existsByCnpjDigits(normalise(command.cnpjDigits()))) {
            throw new DuplicateCnpjException("CNPJ já cadastrado: " + command.cnpjDigits());
        }
        Company company = Company.create(command.cnpjDigits(), command.legalName(), command.taxRegime());
        if (command.tradeName()    != null) company.updateTradeName(command.tradeName());
        if (command.simplesAnnex() != null) company.updateSimplesAnnex(command.simplesAnnex());
        return companyRepository.save(company);
    }

    @Override
    public Company findById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    @Override
    public List<Company> findAll() {
        return List.copyOf(companyRepository.findAll());
    }

    @Override
    public Company update(UUID id, UpdateCompanyCommand command) {
        Company company = findById(id);
        if (command.legalName()    != null) company.updateLegalName(command.legalName());
        if (command.tradeName()    != null) company.updateTradeName(command.tradeName());
        if (command.simplesAnnex() != null) company.updateSimplesAnnex(command.simplesAnnex());
        return companyRepository.save(company);
    }

    @Override
    public void deactivate(UUID id) {
        Company company = findById(id);
        company.deactivate();
        companyRepository.save(company);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String normalise(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("[^\\d]", "");
    }

    // ── exceção local ─────────────────────────────────────────────────────────

    public static class DuplicateCnpjException extends BusinessException {
        public DuplicateCnpjException(String message) { super(message); }
    }
}
