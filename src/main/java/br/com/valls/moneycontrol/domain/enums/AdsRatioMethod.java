package br.com.valls.moneycontrol.domain.enums;

/**
 * Método de rateio do orçamento de ADS entre os lançamentos de venda da semana.
 * BY_REVENUE: proporcional à receita bruta de cada anúncio.
 * BY_UNITS:   proporcional ao número de unidades vendidas.
 */
public enum AdsRatioMethod {
    BY_REVENUE,
    BY_UNITS
}
