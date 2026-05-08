package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.in.StoreUseCase;
import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceAccountRepository;
import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterStoreCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateStoreCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.MarketplaceAccount;
import br.com.valls.moneycontrol.domain.model.Store;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de lojas.
 * Instanciada exclusivamente em {@code BeanConfig} — sem @Service.
 */
@RequiredArgsConstructor
public class StoreUseCaseImpl implements StoreUseCase {

    private final StoreRepository              storeRepository;
    private final MarketplaceAccountRepository accountRepository;
    private final CompanyRepository            companyRepository;
    private final MarketplaceRepository        marketplaceRepository;

    @Override
    public Store register(RegisterStoreCommand command) {
        // Garantir que company e marketplace existem
        companyRepository.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));
        marketplaceRepository.findById(command.marketplaceId())
                .orElseThrow(() -> new MarketplaceNotFoundException(command.marketplaceId()));

        // Obter ou criar MarketplaceAccount (vincula empresa ao marketplace)
        MarketplaceAccount account = accountRepository
                .findByCompanyAndMarketplace(command.companyId(), command.marketplaceId())
                .orElseGet(() -> accountRepository.save(
                        MarketplaceAccount.create(command.companyId(), command.marketplaceId())));

        Store store = Store.create(account.getId(), command.companyId(),
                command.marketplaceId(), command.name());

        if (command.adsRatioMode() != null) {
            store.updateAdsRatioMode(command.adsRatioMode());
        }
        return storeRepository.save(store);
    }

    @Override
    public Store findById(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException(id));
    }

    @Override
    public List<Store> findByCompany(UUID companyId, StoreStatus status) {
        return List.copyOf(storeRepository.findByCompanyId(companyId, status));
    }

    @Override
    public List<Store> findByMarketplace(UUID marketplaceId) {
        return List.copyOf(storeRepository.findByMarketplaceId(marketplaceId));
    }

    @Override
    public Store update(UUID id, UpdateStoreCommand command) {
        Store store = findById(id);
        if (command.name()         != null) store.updateName(command.name());
        if (command.adsRatioMode() != null) store.updateAdsRatioMode(command.adsRatioMode());
        return storeRepository.save(store);
    }

    @Override
    public Store pause(UUID id) {
        Store store = findById(id);
        store.pause();
        return storeRepository.save(store);
    }

    @Override
    public Store reactivate(UUID id) {
        Store store = findById(id);
        store.reactivate();
        return storeRepository.save(store);
    }

    @Override
    public Store close(UUID id) {
        Store store = findById(id);
        store.close();
        return storeRepository.save(store);
    }

    @Override
    public Store updateAdsRatioMode(UUID id, AdsRatioMode mode) {
        Store store = findById(id);
        store.updateAdsRatioMode(mode);
        return storeRepository.save(store);
    }
}
