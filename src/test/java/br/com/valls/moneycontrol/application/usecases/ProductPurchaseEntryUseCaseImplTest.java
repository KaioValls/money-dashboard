package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.ProductPurchaseEntryRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterProductPurchaseEntryCommand;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.model.Product;
import br.com.valls.moneycontrol.domain.model.ProductPurchaseEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;
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
@DisplayName("ProductPurchaseEntryUseCaseImpl")
class ProductPurchaseEntryUseCaseImplTest {

    @Mock ProductPurchaseEntryRepository purchaseRepository;
    @Mock ProductRepository              productRepository;

    ProductPurchaseEntryUseCaseImpl useCase;

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ProductPurchaseEntryUseCaseImpl(purchaseRepository, productRepository);
    }

    private Product stubProduct() {
        return Product.reconstitute(PRODUCT_ID, COMPANY_ID, "SKU-001", "Produto Teste",
                null, null, null, true, true, Instant.now(), Instant.now());
    }

    private RegisterProductPurchaseEntryCommand cmd(int qty, boolean hasFreight, BigDecimal freight) {
        return new RegisterProductPurchaseEntryCommand(
                COMPANY_ID, PRODUCT_ID, 2025, 20, qty,
                new BigDecimal("30.00"), hasFreight, freight, null, null);
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register cria e persiste entrada com dados corretos")
    void registerCreatesEntry() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(stubProduct()));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductPurchaseEntry result = useCase.register(cmd(10, false, BigDecimal.ZERO));

        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getUnitCost()).isEqualByComparingTo("30.00");
        verify(purchaseRepository).save(any(ProductPurchaseEntry.class));
    }

    @Test
    @DisplayName("register com frete calcula totalLandedCost corretamente")
    void registerWithFreight() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(stubProduct()));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductPurchaseEntry result = useCase.register(
                cmd(10, true, new BigDecimal("50.00")));

        // totalCost = 10 × 30 = 300; totalLandedCost = 300 + 50 = 350
        assertThat(result.totalCost()).isEqualByComparingTo("300.00");
        assertThat(result.totalLandedCost()).isEqualByComparingTo("350.00");
        assertThat(result.freightPerUnit()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("register anota supplierName e invoiceNumber quando informados")
    void registerAnnotatesWhenProvided() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(stubProduct()));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new RegisterProductPurchaseEntryCommand(
                COMPANY_ID, PRODUCT_ID, 2025, 20, 5,
                new BigDecimal("30.00"), false, BigDecimal.ZERO,
                "Fornecedor ABC", "NF-001");

        ProductPurchaseEntry result = useCase.register(cmd);

        assertThat(result.getSupplierName()).isEqualTo("Fornecedor ABC");
        assertThat(result.getInvoiceNumber()).isEqualTo("NF-001");
    }

    @Test
    @DisplayName("register lança ProductNotFoundException para produto inexistente")
    void registerProductNotFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(cmd(5, false, BigDecimal.ZERO)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("register lança IllegalArgumentException quando produto não pertence à empresa")
    void registerProductWrongCompany() {
        UUID otherCompany = UUID.randomUUID();
        Product product = Product.reconstitute(PRODUCT_ID, otherCompany, "SKU-001", "Produto",
                null, null, null, true, true, Instant.now(), Instant.now());
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> useCase.register(cmd(5, false, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PRODUCT_ID.toString());
    }

    // ── annotate ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("annotate atualiza supplier e invoiceNumber")
    void annotateUpdatesFields() {
        UUID entryId = UUID.randomUUID();
        ProductPurchaseEntry entry = ProductPurchaseEntry.create(
                COMPANY_ID, PRODUCT_ID, 2025, 20, 5,
                new BigDecimal("30.00"), false, BigDecimal.ZERO);
        when(purchaseRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductPurchaseEntry result = useCase.annotate(entryId, "Fornecedor XYZ", "NF-999");

        assertThat(result.getSupplierName()).isEqualTo("Fornecedor XYZ");
        assertThat(result.getInvoiceNumber()).isEqualTo("NF-999");
        verify(purchaseRepository).save(any());
    }

    // ── findByCompanyAndWeek ──────────────────────────────────────────────────

    @Test
    @DisplayName("findByCompanyAndWeek retorna lista imutável")
    void findByCompanyAndWeekImmutable() {
        WeekReference week = WeekReference.of(java.time.LocalDate.of(2025, 5, 12));
        when(purchaseRepository.findByCompanyAndWeek(COMPANY_ID, week)).thenReturn(List.of());

        List<ProductPurchaseEntry> result = useCase.findByCompanyAndWeek(COMPANY_ID, week);

        assertThatThrownBy(() -> result.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
