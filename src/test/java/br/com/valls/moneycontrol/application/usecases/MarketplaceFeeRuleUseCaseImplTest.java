package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.MarketplaceFeeRuleRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterFeeRuleCommand;
import br.com.valls.moneycontrol.domain.exception.FeeRuleOverlapException;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.model.FeeResult;
import br.com.valls.moneycontrol.domain.model.Marketplace;
import br.com.valls.moneycontrol.domain.model.MarketplaceFeeRule;
import br.com.valls.moneycontrol.domain.service.FeeCalculatorService;
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
@DisplayName("MarketplaceFeeRuleUseCaseImpl")
class MarketplaceFeeRuleUseCaseImplTest {

    @Mock MarketplaceFeeRuleRepository feeRuleRepository;
    @Mock MarketplaceRepository        marketplaceRepository;

    MarketplaceFeeRuleUseCaseImpl useCase;

    private static final UUID      MP_ID    = UUID.randomUUID();
    private static final LocalDate JAN_2025 = LocalDate.of(2025, 1, 1);

    @BeforeEach
    void setUp() {
        useCase = new MarketplaceFeeRuleUseCaseImpl(
                feeRuleRepository, marketplaceRepository, new FeeCalculatorService());
    }

    private Marketplace stubMarketplace() {
        return Marketplace.reconstitute(MP_ID, "ML", "mercado-livre",
                null, null, 14, true, Instant.now(), Instant.now());
    }

    private MarketplaceFeeRule stubRule(BigDecimal min, BigDecimal max) {
        return MarketplaceFeeRule.create(MP_ID, min, max,
                new BigDecimal("10.00"), BigDecimal.ZERO, JAN_2025, 1);
    }

    private RegisterFeeRuleCommand cmd(BigDecimal min, BigDecimal max) {
        return new RegisterFeeRuleCommand(MP_ID, min, max,
                new BigDecimal("16.00"), BigDecimal.ZERO, JAN_2025, 1);
    }

    // ── createRule ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRule salva regra quando não há sobreposição")
    void createRuleNoOverlap() {
        // regra existente: 0–49.99
        MarketplaceFeeRule existing = stubRule(BigDecimal.ZERO, new BigDecimal("49.99"));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of(existing));
        when(feeRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // nova regra: 50–100 (não sobrepõe)
        MarketplaceFeeRule result = useCase.createRule(
                cmd(new BigDecimal("50.00"), new BigDecimal("100.00")));

        assertThat(result.getMinPrice()).isEqualByComparingTo("50.00");
        verify(feeRuleRepository).save(any(MarketplaceFeeRule.class));
    }

    @Test
    @DisplayName("createRule lança FeeRuleOverlapException quando faixas se intersectam")
    void createRuleOverlap() {
        // regra existente: 30–80
        MarketplaceFeeRule existing = stubRule(new BigDecimal("30.00"), new BigDecimal("80.00"));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of(existing));

        // nova regra: 10–50 — sobrepõe em 30–50
        assertThatThrownBy(() -> useCase.createRule(
                cmd(new BigDecimal("10.00"), new BigDecimal("50.00"))))
                .isInstanceOf(FeeRuleOverlapException.class)
                .hasMessageContaining("conflita");

        verify(feeRuleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRule lança FeeRuleOverlapException com nova regra de faixa aberta (max=null)")
    void createRuleOpenMaxOverlap() {
        // regra existente: 50–∞
        MarketplaceFeeRule existing = stubRule(new BigDecimal("50.00"), null);
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of(existing));

        // nova regra: 100–∞ — sobrepõe com existente em 100–∞
        assertThatThrownBy(() -> useCase.createRule(cmd(new BigDecimal("100.00"), null)))
                .isInstanceOf(FeeRuleOverlapException.class);
    }

    @Test
    @DisplayName("createRule permite regra com max=null quando não há sobreposição")
    void createRuleOpenMaxNoOverlap() {
        // regra existente: 0–49.99
        MarketplaceFeeRule existing = stubRule(BigDecimal.ZERO, new BigDecimal("49.99"));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of(existing));
        when(feeRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // nova regra: 50–∞ — não sobrepõe
        assertThatNoException().isThrownBy(() ->
                useCase.createRule(cmd(new BigDecimal("50.00"), null)));
    }

    @Test
    @DisplayName("createRule lança MarketplaceNotFoundException para marketplace inexistente")
    void createRuleMarketplaceNotFound() {
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createRule(
                cmd(BigDecimal.ZERO, new BigDecimal("100.00"))))
                .isInstanceOf(MarketplaceNotFoundException.class);
    }

    @Test
    @DisplayName("createRule aceita primeira regra sem regras anteriores")
    void createFirstRule() {
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of());
        when(feeRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
                useCase.createRule(cmd(BigDecimal.ZERO, null)));
    }

    // ── findApplicableRule ────────────────────────────────────────────────────

    @Test
    @DisplayName("findApplicableRule retorna FeeResult para preço e data válidos")
    void findApplicableRule() {
        MarketplaceFeeRule rule = stubRule(BigDecimal.ZERO, null);
        when(feeRuleRepository.findActiveAt(MP_ID, JAN_2025)).thenReturn(List.of(rule));

        FeeResult result = useCase.findApplicableRule(MP_ID, new BigDecimal("50.00"), JAN_2025);

        // 10% de 50 = 5
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("5.0000");
        assertThat(result.feeRuleId()).isEqualTo(rule.getId());
    }

    // ── findAllByMarketplace ──────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByMarketplace retorna lista imutável")
    void findAllImmutable() {
        when(feeRuleRepository.findAllByMarketplaceId(MP_ID))
                .thenReturn(List.of(stubRule(BigDecimal.ZERO, null)));

        List<MarketplaceFeeRule> result = useCase.findAllByMarketplace(MP_ID);

        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.add(stubRule(BigDecimal.ZERO, null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
