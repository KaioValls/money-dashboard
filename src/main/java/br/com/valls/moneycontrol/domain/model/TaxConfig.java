package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.CalculationBase;
import br.com.valls.moneycontrol.domain.enums.ChargeMoment;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
public class TaxConfig {

    private final UUID            id;
    private final UUID            companyId;
    private final ChargeMoment    chargeMoment;
    private final CalculationBase calculationBase;
    private final BigDecimal      ratePercentage;
    private final LocalDate       validFrom;
    private       LocalDate       validUntil;     // NULL = vigência em aberto
    private       boolean         active;
    private final Instant         createdAt;
    private       Instant         updatedAt;

    private TaxConfig(UUID id, UUID companyId, ChargeMoment chargeMoment,
                      CalculationBase calculationBase, BigDecimal ratePercentage,
                      LocalDate validFrom, Instant createdAt) {
        this.id              = id;
        this.companyId       = companyId;
        this.chargeMoment    = chargeMoment;
        this.calculationBase = calculationBase;
        this.ratePercentage  = ratePercentage;
        this.validFrom       = validFrom;
        this.active          = true;
        this.createdAt       = createdAt;
        this.updatedAt       = createdAt;
    }

    public static TaxConfig create(UUID companyId, ChargeMoment chargeMoment,
                                   CalculationBase calculationBase,
                                   BigDecimal ratePercentage, LocalDate validFrom) {
        Objects.requireNonNull(companyId,       "companyId é obrigatório");
        Objects.requireNonNull(chargeMoment,    "chargeMoment é obrigatório");
        Objects.requireNonNull(calculationBase, "calculationBase é obrigatório");
        Objects.requireNonNull(validFrom,       "validFrom é obrigatório");
        if (ratePercentage == null || ratePercentage.signum() <= 0) {
            throw new IllegalArgumentException("ratePercentage deve ser positivo");
        }
        return new TaxConfig(UUID.randomUUID(), companyId, chargeMoment,
                             calculationBase, ratePercentage, validFrom, Instant.now());
    }

    public static TaxConfig reconstitute(UUID id, UUID companyId, ChargeMoment chargeMoment,
                                         CalculationBase calculationBase, BigDecimal ratePercentage,
                                         LocalDate validFrom, LocalDate validUntil,
                                         boolean active, Instant createdAt, Instant updatedAt) {
        TaxConfig t    = new TaxConfig(id, companyId, chargeMoment, calculationBase,
                                       ratePercentage, validFrom, createdAt);
        t.validUntil   = validUntil;
        t.active       = active;
        t.updatedAt    = updatedAt;
        return t;
    }

    /**
     * Retorna true se esta configuração está vigente na data fornecida.
     * Critério: validFrom <= date AND (validUntil == null OR validUntil >= date).
     */
    public boolean isActiveAt(LocalDate date) {
        Objects.requireNonNull(date, "date é obrigatório");
        if (validFrom.isAfter(date)) return false;
        return validUntil == null || !validUntil.isBefore(date);
    }

    /** Fecha a vigência desta configuração (usado ao criar nova config). */
    public void closeAt(LocalDate until) {
        Objects.requireNonNull(until, "until é obrigatório");
        this.validUntil = until;
        this.active     = false;
        this.updatedAt  = Instant.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = Instant.now();
    }
}
