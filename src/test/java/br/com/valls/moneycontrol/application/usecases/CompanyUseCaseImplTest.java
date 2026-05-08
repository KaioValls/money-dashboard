package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterCompanyCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateCompanyCommand;
import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyUseCaseImpl")
class CompanyUseCaseImplTest {

    @Mock CompanyRepository companyRepository;

    CompanyUseCaseImpl useCase;

    // CNPJ válido para testes: 11.222.333/0001-81
    private static final String VALID_CNPJ = "11222333000181";

    @BeforeEach
    void setUp() {
        useCase = new CompanyUseCaseImpl(companyRepository);
    }

    private Company stubCompany() {
        return Company.reconstitute(UUID.randomUUID(), VALID_CNPJ, "Empresa Teste",
                null, TaxRegime.SIMPLES_NACIONAL, null, true,
                Instant.now(), Instant.now());
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register salva company quando CNPJ único")
    void registerSuccess() {
        when(companyRepository.existsByCnpjDigits(VALID_CNPJ)).thenReturn(false);
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Company result = useCase.register(new RegisterCompanyCommand(
                VALID_CNPJ, "Empresa Teste", null, TaxRegime.SIMPLES_NACIONAL, null));

        assertThat(result.getCnpjDigits()).isEqualTo(VALID_CNPJ);
        assertThat(result.getLegalName()).isEqualTo("Empresa Teste");
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("register lança DuplicateCnpjException para CNPJ já existente")
    void registerDuplicateCnpj() {
        when(companyRepository.existsByCnpjDigits(VALID_CNPJ)).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(new RegisterCompanyCommand(
                VALID_CNPJ, "Empresa Teste", null, TaxRegime.SIMPLES_NACIONAL, null)))
                .isInstanceOf(CompanyUseCaseImpl.DuplicateCnpjException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("register lança IllegalArgumentException para CNPJ inválido")
    void registerInvalidCnpj() {
        when(companyRepository.existsByCnpjDigits(anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.register(new RegisterCompanyCommand(
                "00000000000000", "X", null, TaxRegime.SIMPLES_NACIONAL, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("register aceita CNPJ formatado (normaliza dígitos)")
    void registerFormattedCnpj() {
        when(companyRepository.existsByCnpjDigits(VALID_CNPJ)).thenReturn(false);
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
                useCase.register(new RegisterCompanyCommand(
                        "11.222.333/0001-81", "Empresa", null,
                        TaxRegime.SIMPLES_NACIONAL, null)));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById retorna company quando existe")
    void findByIdSuccess() {
        Company company = stubCompany();
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

        assertThat(useCase.findById(company.getId())).isEqualTo(company);
    }

    @Test
    @DisplayName("findById lança CompanyNotFoundException quando não existe")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll retorna lista imutável")
    void findAllImmutable() {
        when(companyRepository.findAll()).thenReturn(List.of(stubCompany()));

        List<Company> result = useCase.findAll();

        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.add(stubCompany()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update aplica apenas campos não-nulos")
    void updatePartial() {
        Company company = stubCompany();
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Company result = useCase.update(company.getId(),
                new UpdateCompanyCommand("Nova Razão", null, null, null));

        assertThat(result.getLegalName()).isEqualTo("Nova Razão");
        verify(companyRepository).save(company);
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate chama company.deactivate e salva")
    void deactivateSuccess() {
        Company company = stubCompany();
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.deactivate(company.getId());

        assertThat(company.isActive()).isFalse();
        verify(companyRepository).save(company);
    }
}
