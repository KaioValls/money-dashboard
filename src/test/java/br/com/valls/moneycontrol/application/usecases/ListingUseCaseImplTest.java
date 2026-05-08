package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.ListingRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.ports.out.StoreRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterListingCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateListingPriceCommand;
import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.ListingStatus;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import br.com.valls.moneycontrol.domain.exception.ListingNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.exception.StoreNotFoundException;
import br.com.valls.moneycontrol.domain.model.Listing;
import br.com.valls.moneycontrol.domain.model.ListingPriceHistory;
import br.com.valls.moneycontrol.domain.model.Product;
import br.com.valls.moneycontrol.domain.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingUseCaseImpl")
class ListingUseCaseImplTest {

    @Mock ListingRepository listingRepository;
    @Mock StoreRepository   storeRepository;
    @Mock ProductRepository productRepository;

    ListingUseCaseImpl useCase;

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID MP_ID      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ListingUseCaseImpl(listingRepository, storeRepository, productRepository);
    }

    private Store stubStore(UUID companyId) {
        return Store.reconstitute(UUID.randomUUID(), UUID.randomUUID(), companyId, MP_ID,
                "Loja Teste", StoreStatus.ACTIVE, AdsRatioMode.DIRECT,
                true, Instant.now(), Instant.now());
    }

    private Product stubProduct(UUID companyId) {
        return Product.reconstitute(UUID.randomUUID(), companyId, "SKU-001", "Produto",
                null, null, null, true, true, Instant.now(), Instant.now());
    }

    private Listing stubListing(UUID productId, UUID storeId, UUID companyId) {
        return Listing.reconstitute(UUID.randomUUID(), productId, storeId, MP_ID, companyId,
                null, "Título", new BigDecimal("50.00"), null, ListingStatus.ACTIVE,
                null, null, true, Instant.now(), Instant.now());
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register cria anúncio quando produto e loja da mesma empresa")
    void registerSuccess() {
        Product product = stubProduct(COMPANY_ID);
        Store store     = stubStore(COMPANY_ID);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Listing result = useCase.register(new RegisterListingCommand(
                product.getId(), store.getId(),
                "Título Anúncio", new BigDecimal("99.90"), null, null));

        assertThat(result.getProductId()).isEqualTo(product.getId());
        assertThat(result.getStoreId()).isEqualTo(store.getId());
        assertThat(result.getCompanyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    @DisplayName("register lança CrossCompanyListingException para produto e loja de empresas diferentes")
    void registerCrossCompany() {
        UUID otherCompany = UUID.randomUUID();
        Product product   = stubProduct(COMPANY_ID);
        Store store       = stubStore(otherCompany);  // empresa diferente
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> useCase.register(new RegisterListingCommand(
                product.getId(), store.getId(), "Título", new BigDecimal("50.00"), null, null)))
                .isInstanceOf(ListingUseCaseImpl.CrossCompanyListingException.class);
    }

    @Test
    @DisplayName("register lança ProductNotFoundException quando produto não existe")
    void registerProductNotFound() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(new RegisterListingCommand(
                productId, UUID.randomUUID(), "Título", BigDecimal.TEN, null, null)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("register lança StoreNotFoundException quando loja não existe")
    void registerStoreNotFound() {
        Product product = stubProduct(COMPANY_ID);
        UUID storeId = UUID.randomUUID();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(new RegisterListingCommand(
                product.getId(), storeId, "Título", BigDecimal.TEN, null, null)))
                .isInstanceOf(StoreNotFoundException.class);
    }

    // ── updatePrice ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePrice persiste novo histórico e fecha o anterior")
    void updatePriceClosesOldHistory() {
        Product product = stubProduct(COMPANY_ID);
        Store store     = stubStore(COMPANY_ID);
        Listing listing = stubListing(product.getId(), store.getId(), COMPANY_ID);
        UUID changedBy  = UUID.randomUUID();

        // histórico anterior com valid_until=null (preço vigente)
        ListingPriceHistory previous = ListingPriceHistory.reconstitute(
                UUID.randomUUID(), listing.getId(), new BigDecimal("50.00"),
                Instant.now().minusSeconds(3600), null, changedBy, "inicial");

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.findPriceHistoryByListingId(listing.getId()))
                .thenReturn(List.of(previous));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepository.savePriceHistory(any())).thenAnswer(inv -> inv.getArgument(0));

        ListingPriceHistory result = useCase.updatePrice(listing.getId(),
                new UpdateListingPriceCommand(new BigDecimal("79.90"), changedBy, "promo"));

        // anterior foi fechado
        assertThat(previous.getValidUntil()).isNotNull();
        // novo preço gerado
        assertThat(result.getSalePrice()).isEqualByComparingTo("79.90");
        assertThat(listing.getSalePrice()).isEqualByComparingTo("79.90");

        // savePriceHistory chamado 2x: fechar anterior + salvar novo
        verify(listingRepository, times(2)).savePriceHistory(any());
    }

    // ── status transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("pause altera status para PAUSED")
    void pauseSuccess() {
        Product product = stubProduct(COMPANY_ID);
        Store store     = stubStore(COMPANY_ID);
        Listing listing = stubListing(product.getId(), store.getId(), COMPANY_ID);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Listing result = useCase.pause(listing.getId());

        assertThat(result.getStatus()).isEqualTo(ListingStatus.PAUSED);
    }

    @Test
    @DisplayName("pause em anúncio CLOSED lança ListingTransitionException")
    void pauseClosedListing() {
        Product product = stubProduct(COMPANY_ID);
        Store store     = stubStore(COMPANY_ID);
        Listing listing = stubListing(product.getId(), store.getId(), COMPANY_ID);
        listing.close();
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> useCase.pause(listing.getId()))
                .isInstanceOf(Listing.ListingTransitionException.class);
    }

    @Test
    @DisplayName("findById lança ListingNotFoundException quando não existe")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(listingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("findByStore retorna lista imutável")
    void findByStoreImmutable() {
        Store store     = stubStore(COMPANY_ID);
        Product product = stubProduct(COMPANY_ID);
        when(listingRepository.findByStoreId(store.getId()))
                .thenReturn(List.of(
                        stubListing(product.getId(), store.getId(), COMPANY_ID)));

        List<Listing> result = useCase.findByStore(store.getId());

        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.add(
                stubListing(product.getId(), store.getId(), COMPANY_ID)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
