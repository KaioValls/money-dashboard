package br.com.valls.moneycontrol.application.usecases;

import br.com.valls.moneycontrol.application.ports.out.MarketplaceRepository;
import br.com.valls.moneycontrol.application.usecases.command.RegisterMarketplaceCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateMarketplaceCommand;
import br.com.valls.moneycontrol.domain.exception.MarketplaceNotFoundException;
import br.com.valls.moneycontrol.domain.model.Marketplace;
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
@DisplayName("MarketplaceUseCaseImpl")
class MarketplaceUseCaseImplTest {

    @Mock MarketplaceRepository marketplaceRepository;

    MarketplaceUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new MarketplaceUseCaseImpl(marketplaceRepository);
    }

    private Marketplace stubMarketplace() {
        return Marketplace.reconstitute(UUID.randomUUID(), "Mercado Livre",
                "mercado-livre", null, null, 14, true, Instant.now(), Instant.now());
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register salva marketplace quando slug único")
    void registerSuccess() {
        when(marketplaceRepository.existsBySlug("mercado-livre")).thenReturn(false);
        when(marketplaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Marketplace result = useCase.register(new RegisterMarketplaceCommand(
                "Mercado Livre", "mercado-livre", null, null, 14));

        assertThat(result.getSlug()).isEqualTo("mercado-livre");
        verify(marketplaceRepository).save(any(Marketplace.class));
    }

    @Test
    @DisplayName("register lança DuplicateSlugException para slug já existente")
    void registerDuplicateSlug() {
        when(marketplaceRepository.existsBySlug("mercado-livre")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(new RegisterMarketplaceCommand(
                "Mercado Livre", "mercado-livre", null, null, 14)))
                .isInstanceOf(MarketplaceUseCaseImpl.DuplicateSlugException.class);

        verify(marketplaceRepository, never()).save(any());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById lança MarketplaceNotFoundException quando não existe")
    void findByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(marketplaceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id))
                .isInstanceOf(MarketplaceNotFoundException.class);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll retorna lista imutável")
    void findAllImmutable() {
        when(marketplaceRepository.findAll()).thenReturn(List.of(stubMarketplace()));

        List<Marketplace> result = useCase.findAll();

        assertThat(result).hasSize(1);
        assertThatThrownBy(() -> result.add(stubMarketplace()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update aplica campos não-nulos e salva")
    void updateName() {
        Marketplace mp = stubMarketplace();
        when(marketplaceRepository.findById(mp.getId())).thenReturn(Optional.of(mp));
        when(marketplaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Marketplace result = useCase.update(mp.getId(),
                new UpdateMarketplaceCommand("Novo Nome", null, null, null));

        assertThat(result.getName()).isEqualTo("Novo Nome");
    }

    // ── activate / deactivate ─────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate chama marketplace.deactivate e salva")
    void deactivateSuccess() {
        Marketplace mp = stubMarketplace();
        when(marketplaceRepository.findById(mp.getId())).thenReturn(Optional.of(mp));
        when(marketplaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.deactivate(mp.getId());

        assertThat(mp.isActive()).isFalse();
        verify(marketplaceRepository).save(mp);
    }

    @Test
    @DisplayName("activate chama marketplace.activate e salva")
    void activateSuccess() {
        Marketplace mp = stubMarketplace();
        mp.deactivate(); // começa inativo
        when(marketplaceRepository.findById(mp.getId())).thenReturn(Optional.of(mp));
        when(marketplaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.activate(mp.getId());

        assertThat(mp.isActive()).isTrue();
    }
}
