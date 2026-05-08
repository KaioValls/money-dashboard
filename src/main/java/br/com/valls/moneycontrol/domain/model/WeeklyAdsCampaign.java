package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMethod;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Orçamento total de ADS de uma loja em uma semana ISO.
 * UNIQUE(store_id, iso_year, iso_week_number) — apenas uma campanha por loja/semana.
 * O rateio entre os anúncios é realizado pelo {@code WeeklyAdsCampaignUseCase.distributeAds}.
 */
@Getter
public class WeeklyAdsCampaign {

    private final UUID           id;
    private final UUID           storeId;
    private final int            isoYear;
    private final int            isoWeekNumber;
    private       BigDecimal     totalAdsBudget;
    private       AdsRatioMethod ratioMethod;
    private       boolean        active;
    private final Instant        createdAt;
    private       Instant        updatedAt;

    private WeeklyAdsCampaign(UUID id, UUID storeId, int isoYear, int isoWeekNumber,
                               BigDecimal totalAdsBudget, AdsRatioMethod ratioMethod,
                               boolean active, Instant createdAt) {
        this.id             = id;
        this.storeId        = storeId;
        this.isoYear        = isoYear;
        this.isoWeekNumber  = isoWeekNumber;
        this.totalAdsBudget = totalAdsBudget;
        this.ratioMethod    = ratioMethod;
        this.active         = active;
        this.createdAt      = createdAt;
        this.updatedAt      = createdAt;
    }

    public static WeeklyAdsCampaign create(UUID storeId, int isoYear, int isoWeekNumber,
                                            BigDecimal totalAdsBudget, AdsRatioMethod ratioMethod) {
        Objects.requireNonNull(storeId,        "storeId é obrigatório");
        Objects.requireNonNull(totalAdsBudget, "totalAdsBudget é obrigatório");
        Objects.requireNonNull(ratioMethod,    "ratioMethod é obrigatório");
        if (totalAdsBudget.signum() < 0)
            throw new IllegalArgumentException("totalAdsBudget não pode ser negativo");
        return new WeeklyAdsCampaign(UUID.randomUUID(), storeId, isoYear, isoWeekNumber,
                totalAdsBudget, ratioMethod, true, Instant.now());
    }

    public static WeeklyAdsCampaign reconstitute(UUID id, UUID storeId, int isoYear, int isoWeekNumber,
                                                  BigDecimal totalAdsBudget, AdsRatioMethod ratioMethod,
                                                  boolean active, Instant createdAt, Instant updatedAt) {
        WeeklyAdsCampaign c = new WeeklyAdsCampaign(id, storeId, isoYear, isoWeekNumber,
                totalAdsBudget, ratioMethod, active, createdAt);
        c.updatedAt = updatedAt;
        return c;
    }

    /**
     * Atualiza o orçamento e/ou método de rateio da campanha.
     * Campos {@code null} mantêm o valor atual.
     */
    public void update(BigDecimal newBudget, AdsRatioMethod newMethod) {
        if (newBudget != null) {
            if (newBudget.signum() < 0)
                throw new IllegalArgumentException("totalAdsBudget não pode ser negativo");
            this.totalAdsBudget = newBudget;
        }
        if (newMethod != null) {
            this.ratioMethod = newMethod;
        }
        this.updatedAt = Instant.now();
    }

    /** Desativa a campanha (sem exclusão física). */
    public void deactivate() {
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public WeekReference weekReference() {
        return new WeekReference(isoYear, isoWeekNumber);
    }
}
