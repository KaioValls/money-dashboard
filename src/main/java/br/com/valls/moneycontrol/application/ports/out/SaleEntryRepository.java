package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.SaleEntry;
import br.com.valls.moneycontrol.domain.model.WeekReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de lançamentos de venda.
 */
public interface SaleEntryRepository {

    /** Persiste ou atualiza um lançamento e retorna a instância salva. */
    SaleEntry save(SaleEntry entry);

    /** Busca lançamento pelo ID. */
    Optional<SaleEntry> findById(UUID id);

    /** Retorna lançamentos de uma loja em uma semana (roteia para partição correta). */
    List<SaleEntry> findByStoreAndWeek(UUID storeId, WeekReference week);

    /** Retorna lançamentos de uma empresa em uma semana. */
    List<SaleEntry> findByCompanyAndWeek(UUID companyId, WeekReference week);

    /** Retorna lançamentos de um anúncio em uma semana. */
    List<SaleEntry> findByListingAndWeek(UUID listingId, WeekReference week);

    /** Verifica se já existe lançamento para o anúncio na semana (detecta duplicata). */
    boolean existsByListingAndWeek(UUID listingId, WeekReference week);

    /** Remove um lançamento pelo ID. */
    void deleteById(UUID id);
}
