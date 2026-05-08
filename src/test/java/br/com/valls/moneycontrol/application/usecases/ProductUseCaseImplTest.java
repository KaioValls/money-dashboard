package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.CompanyRepository;
import br.com.valls.moneycontrol.application.ports.out.ProductRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterProductCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateProductCommand;
import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.ProductNotFoundException;
import br.com.valls.moneycontrol.domain.model.Company;
import br.com.valls.moneycontrol.domain.model.Product;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductUseCaseImpl")
class ProductUseCaseImplTest {

    @Mock ProductRepository productRepository;
    @Mock CompanyRepository companyRepository;

    ProductUseCaseImpl useCase;

    private static final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ProductUseCaseImpl(productRepository, companyRepository);
    }

    private Company stubCompany() {
        return Company.reconstitute(COMPANY_ID, "11222333000181", "Empresa",
                null, TaxRegime.SIMPLES_NACIONAL, null, true, Instant.now(), Instant.now());
    }

    private Product stubProduct(String sku) {
        return Product.reconstitute(UUID.randomUUID(), COMPANY_ID, sku, "Produto " + sku,
                "Cat", null, null, true, true, Instant.now(), Instant.now());
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register cria produto quando SKU único para a empresa")
    void registerSuccess() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(productRepository.existsByCompanyIdAndSku(COMPANY_ID, "SKU-001")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = useCase.register(new RegisterProductCommand(
                COMPANY_ID, "SKU-001", "Produto Teste", "Cat", null, 500));

        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getWeightGrams()).isEqualTo(500);
    }

    @Test
    @DisplayName("register lança DuplicateSkuException para SKU repetido na empresa")
    void registerDuplicateSku() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(stubCompany()));
        when(productRepository.existsByCompanyIdAndSku(COMPANY_ID, "SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(new RegisterProductCommand(
                COMPANY_ID, "SKU-001", "Produto", null, null, null)))
                .isInstanceOf(ProductUseCaseImpl.DuplicateSkuException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("register lança CompanyNotFoundException para empresa inexistente")
    void registerCompanyNotFound() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(new RegisterProductCommand(
                COMPANY_ID, "SKU-001", "Produto", null, null, null)))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    // ── findByCompanyAndSku ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByCompanyAndSku retorna Optional correto")
    void findByCompanyAndSku() {
        Product product = stubProduct("SKU-001");
        when(productRepository.findByCompanyAndSku(COMPANY_ID, "SKU-001"))
                .thenReturn(Optional.of(product));

        Optional<Product> result = useCase.findByCompanyAndSku(COMPANY_ID, "SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo("SKU-001");
    }

    // ── findAllByCompany ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByCompany retorna lista imutável")
    void findAllByCompanyImmutable() {
        when(productRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(List.of(stubProduct("A"), stubProduct("B")));

        List<Product> result = useCase.findAllByCompany(COMPANY_ID);

        assertThat(result).hasSize(2);
        assertThatThrownBy(() -> result.add(stubProduct("C")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update aplica apenas campos não-nulos")
    void updatePartial() {
        Product product = stubProduct("SKU-001");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = useCase.update(product.getId(),
                new UpdateProductCommand("Novo Nome", null, null, null));

        assertThat(result.getName()).isEqualTo("Novo Nome");
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate chama product.deactivate e salva")
    void deactivateSuccess() {
        Product product = stubProduct("SKU-001");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.deactivate(product.getId());

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("findById lança ProductNotFoundException quando não existe")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
