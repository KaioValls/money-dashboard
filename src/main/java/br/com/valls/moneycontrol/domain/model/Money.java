package br.com.valls.moneycontrol.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object imutável que representa um valor monetário.
 * Operações internas usam escala 4 com HALF_UP.
 * Saída formatada para apresentação usa escala 2 via {@link #toScaled()}.
 */
public record Money(BigDecimal amount, String currency) {

    private static final String DEFAULT_CURRENCY = "BRL";
    private static final int    INTERNAL_SCALE   = 4;
    private static final int    OUTPUT_SCALE     = 2;
    private static final RoundingMode MODE        = RoundingMode.HALF_UP;

    public Money {
        if (amount == null)                    throw new IllegalArgumentException("amount não pode ser nulo");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency não pode ser nulo ou vazio");
    }

    /** Cria Money em BRL. */
    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    /** Cria Money zero em BRL. */
    public static Money zero() {
        return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /** Multiplica pelo fator; resultado com escala interna 4 e HALF_UP. */
    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor).setScale(INTERNAL_SCALE, MODE), currency);
    }

    /**
     * Aplica uma taxa percentual decimal (ex: 0.0600 para 6%).
     * Resultado com escala 4 e HALF_UP.
     */
    public Money percentage(BigDecimal rate) {
        return new Money(amount.multiply(rate).setScale(INTERNAL_SCALE, MODE), currency);
    }

    /** Retorna o valor arredondado para 2 casas decimais (uso em saída/serialização). */
    public BigDecimal toScaled() {
        return amount.setScale(OUTPUT_SCALE, MODE);
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Operação entre moedas diferentes: " + this.currency + " vs " + other.currency);
        }
    }
}
