package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.TaxConfigRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterTaxConfigCommand;
import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import br.com.valls.moneycontrol.domain.model.TaxConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaxConfigUseCaseImpl")
class TaxConfigUseCaseImplTest {

    @Mock TaxConfigRepository taxConfigRepository;
    @Mock CompanyRepository   companyRepository;

    TaxConfigUseCaseImpl useCase;

    private static final UUID      COMPANY_ID = UUID.randomUUID();
    private static final LocalDate JAN_2025   = LocalDate.of(2025, 1, 1);
    private static final LocalDate JUL_2025   = LocalDate.of(2025, 7, 1);

    @BeforeEach
    void setUp() {
        useCase = new TaxConfigUseCaseImpl(taxConfigRepository, companyRepository);
    }

    private Company stubCompany() {
        return Company.reconstitute(COMPANY_ID, "11222333000181", "Empresa",
                null, TaxRegime.SIMPLES_NACIONAL, null, true, Instant.now(), Instant.now());
    }

    private TaxConfig stubOpenConfig(LocalDate validFrom) {
        return TaxConfig.reconstitute(UUID.randomUUID(), COMPANY_ID, ChargeMoment.ON_SALE,
                CalculationBase.GROSS_REVENUE, new BigDecimal("6.00"), validFrom,
                null, true, Instant.now(), Instant.now());
    }

    private RegisterTaxConfigCommand cmd(LocalDate validFrom, BigDecimal rate) {
        return new RegisterTaxConfigCommand(COMPANY_ID, ChargeMoment.ON_SALE,
                CalculationBase.GROSS_REVENUE, rate, validFrom);
    }

    // ── createConfig ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createConfig cria nova config sem config anterior")
    void createFirstConfig() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(taxConfigRepository.findOpenByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
        when(taxConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaxConfig result = useCase.createConfig(cmd(JAN_2025, new BigDecimal("6.00")));

        assertThat(result.getRatePercentage()).isEqualByComparingTo("6.00");
        assertThat(result.getValidFrom()).isEqualTo(JAN_2025);
        assertThat(result.getValidUntil()).isNull();
        verify(taxConfigRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createConfig fecha config anterior com valid_until = validFrom - 1 dia")
    void createConfigClosesExisting() {
        TaxConfig existing = stubOpenConfig(JAN_2025);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(taxConfigRepository.findOpenByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existing));
        when(taxConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.createConfig(cmd(JUL_2025, new BigDecimal("8.00")));

        // anterior deve ter sido fechada em JUL_2025 - 1 dia
        assertThat(existing.getValidUntil()).isEqualTo(JUL_2025.minusDays(1));
        // save chamado 2x: fechar anterior + criar nova
        verify(taxConfigRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("createConfig lança CompanyNotFoundException para empresa inexistente")
    void createConfigCompanyNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createConfig(cmd(JAN_2025, new BigDecimal("6.00"))))
                .isInstanceOf(CompanyNotFoundException.class);
        verify(taxConfigRepository, never()).save(any());
    }

    // ── findActiveByCompanyAndDate ────────────────────────────────────────────

    @Test
    @DisplayName("findActiveByCompanyAndDate retorna Optional.empty para data antes da primeira config")
    void findActiveEmpty() {
        when(taxConfigRepository.findActiveAt(COMPANY_ID, JAN_2025))
                .thenReturn(Optional.empty());

        Optional<TaxConfig> result = useCase.findActiveByCompanyAndDate(COMPANY_ID, JAN_2025);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findActiveByCompanyAndDate retorna config vigente")
    void findActiveReturnsConfig() {
        TaxConfig config = stubOpenConfig(JAN_2025);
        when(taxConfigRepository.findActiveAt(COMPANY_ID, JUL_2025))
                .thenReturn(Optional.of(config));

        Optional<TaxConfig> result = useCase.findActiveByCompanyAndDate(COMPANY_ID, JUL_2025);

        assertThat(result).contains(config);
    }

    // ── findAllByCompany ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByCompany retorna lista imutável")
    void findAllImmutable() {
        when(taxConfigRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(List.of(stubOpenConfig(JAN_2025)));

        List<TaxConfig> result = useCase.findAllByCompany(COMPANY_ID);

        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.add(stubOpenConfig(JUL_2025)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
