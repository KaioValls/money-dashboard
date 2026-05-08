package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceAccountRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterStoreCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import br.com.valls.moneycontrol.domain.model.Marketplace;
import br.com.valls.moneycontrol.domain.model.MarketplaceAccount;
import br.com.valls.moneycontrol.domain.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreUseCaseImpl")
class StoreUseCaseImplTest {

    @Mock StoreRepository              storeRepository;
    @Mock MarketplaceAccountRepository accountRepository;
    @Mock CompanyRepository            companyRepository;
    @Mock MarketplaceRepository        marketplaceRepository;

    StoreUseCaseImpl useCase;

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID MP_ID      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new StoreUseCaseImpl(storeRepository, accountRepository,
                companyRepository, marketplaceRepository);
    }

    private Company stubCompany() {
        return Company.reconstitute(COMPANY_ID, "11222333000181", "Empresa",
                null, TaxRegime.SIMPLES_NACIONAL, null, true, Instant.now(), Instant.now());
    }

    private Marketplace stubMarketplace() {
        return Marketplace.reconstitute(MP_ID, "ML", "mercado-livre",
                null, null, 14, true, Instant.now(), Instant.now());
    }

    private MarketplaceAccount stubAccount() {
        return MarketplaceAccount.reconstitute(UUID.randomUUID(), COMPANY_ID, MP_ID,
                null, true, Instant.now(), Instant.now());
    }

    private Store stubStore(UUID accountId) {
        return Store.reconstitute(UUID.randomUUID(), accountId, COMPANY_ID, MP_ID,
                "Loja Teste", StoreStatus.ACTIVE, AdsRatioMode.DIRECT,
                true, Instant.now(), Instant.now());
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register cria MarketplaceAccount se não existir")
    void registerCreatesAccount() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(accountRepository.findByCompanyAndMarketplace(COMPANY_ID, MP_ID))
                .thenReturn(Optional.empty());
        MarketplaceAccount newAccount = stubAccount();
        when(accountRepository.save(any())).thenReturn(newAccount);
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = useCase.register(
                new RegisterStoreCommand(COMPANY_ID, MP_ID, "Loja Teste", AdsRatioMode.DIRECT));

        assertThat(result.getCompanyId()).isEqualTo(COMPANY_ID);
        verify(accountRepository).save(any(MarketplaceAccount.class));
    }

    @Test
    @DisplayName("register reutiliza MarketplaceAccount existente")
    void registerReusesAccount() {
        MarketplaceAccount existing = stubAccount();
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.of(stubMarketplace()));
        when(accountRepository.findByCompanyAndMarketplace(COMPANY_ID, MP_ID))
                .thenReturn(Optional.of(existing));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.register(new RegisterStoreCommand(COMPANY_ID, MP_ID, "Loja", AdsRatioMode.DIRECT));

        verify(accountRepository, never()).save(any(MarketplaceAccount.class));
    }

    @Test
    @DisplayName("register lança CompanyNotFoundException para company inexistente")
    void registerCompanyNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(
                new RegisterStoreCommand(COMPANY_ID, MP_ID, "Loja", null)))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    @DisplayName("register lança MarketplaceNotFoundException para marketplace inexistente")
    void registerMarketplaceNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(marketplaceRepository.findById(MP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(
                new RegisterStoreCommand(COMPANY_ID, MP_ID, "Loja", null)))
                .isInstanceOf(MarketplaceNotFoundException.class);
    }

    // ── status transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("pause altera status para PAUSED")
    void pauseSuccess() {
        MarketplaceAccount account = stubAccount();
        Store store = stubStore(account.getId());
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = useCase.pause(store.getId());

        assertThat(result.getStatus()).isEqualTo(StoreStatus.PAUSED);
    }

    @Test
    @DisplayName("pause em loja CLOSED lança StoreTransitionException")
    void pauseClosedStore() {
        MarketplaceAccount account = stubAccount();
        Store store = stubStore(account.getId());
        store.close();
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> useCase.pause(store.getId()))
                .isInstanceOf(Store.StoreTransitionException.class);
    }

    @Test
    @DisplayName("reactivate muda PAUSED para ACTIVE")
    void reactivateSuccess() {
        MarketplaceAccount account = stubAccount();
        Store store = stubStore(account.getId());
        store.pause();
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = useCase.reactivate(store.getId());

        assertThat(result.getStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    @Test
    @DisplayName("findById lança StoreNotFoundException quando não existe")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(storeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    @DisplayName("updateAdsRatioMode altera o modo e salva")
    void updateAdsRatioMode() {
        MarketplaceAccount account = stubAccount();
        Store store = stubStore(account.getId());
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = useCase.updateAdsRatioMode(store.getId(), AdsRatioMode.CAMPAIGN);

        assertThat(result.getAdsRatioMode()).isEqualTo(AdsRatioMode.CAMPAIGN);
    }
}
