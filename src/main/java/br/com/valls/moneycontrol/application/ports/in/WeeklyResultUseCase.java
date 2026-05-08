package br.com.valls.moneycontrol.application.ports.in;

import br.com.valls.moneycontrol.domain.model.WeekOverWeekDelta;
import br.com.valls.moneycontrol.domain.model.WeekReference;
import br.com.valls.moneycontrol.domain.model.WeeklyResult;

import java.util.UUID;

/**
 * Port de entrada para apuração de resultados semanais.
 *
 * <p>Os resultados são computados sob demanda a partir dos {@code SaleEntry} persistidos —
 * não há snapshot armazenado separado para o resultado consolidado neste escopo.
 */
public interface WeeklyResultUseCase {

    /**
     * Agrega todos os lançamentos de venda de uma loja em uma semana e retorna
     * o resultado financeiro consolidado da loja no período.
     * Retorna um {@link WeeklyResult} com todos os campos zerados se não houver lançamentos.
     */
    WeeklyResult computeByStore(UUID storeId, WeekReference week);

    /**
     * Agrega todos os lançamentos de venda de todas as lojas de uma empresa em uma
     * semana e retorna o resultado financeiro consolidado da empresa no período.
     * O {@code storeId} do resultado é {@code null} (representa empresa inteira).
     */
    WeeklyResult computeConsolidated(UUID companyId, WeekReference week);

    /**
     * Retorna a variação semana-a-semana dos principais KPIs para uma loja,
     * comparando a semana informada com a semana imediatamente anterior
     * ({@link WeekReference#previous()}).
     */
    WeekOverWeekDelta weekOverWeekDelta(UUID storeId, WeekReference week);
}
