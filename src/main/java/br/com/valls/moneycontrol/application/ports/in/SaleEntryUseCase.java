package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.application.usecases.command.RegisterSaleEntryCommand;
import br.com.valls.moneycontrol.application.usecases.command.UpdateSaleEntryCommand;
import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.UUID;

/**
 * Port de entrada para gerenciamento de lançamentos de venda semanal.
 */
public interface SaleEntryUseCase {

    /**
     * Registra um lançamento de venda semanal com todos os snapshots capturados no momento:
     * sale_price_snapshot (do listing), unit_cost_snapshot (compra mais recente antes da semana),
     * tax_rate_snapshot e fee_rule snapshots. Campos calculados derivados via
     * {@code ProfitCalculatorService}.
     */
    SaleEntry register(RegisterSaleEntryCommand command);

    /**
     * Atualiza somente os custos operacionais (ads, coupon, freight, return, other)
     * e recalcula os campos derivados mantendo os snapshots originais.
     */
    SaleEntry update(UUID id, UpdateSaleEntryCommand command);

    /** Remove um lançamento de venda. */
    void delete(UUID id);

    /** Retorna lançamentos de uma loja em uma semana. */
    List<SaleEntry> findByStoreAndWeek(UUID storeId, WeekReference week);

    /** Retorna lançamentos de um anúncio em uma semana. */
    List<SaleEntry> findByListingAndWeek(UUID listingId, WeekReference week);
}
