package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de compra de produto (CMV).
 * freightPerUnit e totalLandedCost são calculados pelo domínio — não armazenados como campos.
 * O adapter de persistência persiste os valores computados via os métodos abaixo.
 */
@Getter
public class ProductPurchaseEntry {

    private final UUID       id;
    private final UUID       companyId;
    private final UUID       productId;
    private final int        isoYear;
    private final int        isoWeekNumber;
    private final int        quantity;
    private final BigDecimal unitCost;
    private final boolean    hasFreight;
    private final BigDecimal freightCost;
    private       String     supplierName;   // snapshot — não FK
    private       String     invoiceNumber;
    private final Instant    createdAt;
    private       Instant    updatedAt;

    private ProductPurchaseEntry(UUID id, UUID companyId, UUID productId,
                                 int isoYear, int isoWeekNumber,
                                 int quantity, BigDecimal unitCost,
                                 boolean hasFreight, BigDecimal freightCost,
                                 Instant createdAt) {
        this.id            = id;
        this.companyId     = companyId;
        this.productId     = productId;
        this.isoYear       = isoYear;
        this.isoWeekNumber = isoWeekNumber;
        this.quantity      = quantity;
        this.unitCost      = unitCost;
        this.hasFreight    = hasFreight;
        this.freightCost   = hasFreight ? freightCost : BigDecimal.ZERO;
        this.createdAt     = createdAt;
        this.updatedAt     = createdAt;
    }

    public static ProductPurchaseEntry create(UUID companyId, UUID productId,
                                              int isoYear, int isoWeekNumber,
                                              int quantity, BigDecimal unitCost,
                                              boolean hasFreight, BigDecimal freightCost) {
        Objects.requireNonNull(companyId,  "companyId é obrigatório");
        Objects.requireNonNull(productId,  "productId é obrigatório");
        if (quantity <= 0)                                  throw new IllegalArgumentException("quantity deve ser > 0");
        if (unitCost == null || unitCost.signum() <= 0)     throw new IllegalArgumentException("unitCost deve ser positivo");
        if (hasFreight && (freightCost == null || freightCost.signum() < 0))
            throw new IllegalArgumentException("freightCost deve ser >= 0 quando hasFreight=true");
        return new ProductPurchaseEntry(UUID.randomUUID(), companyId, productId,
                                        isoYear, isoWeekNumber, quantity, unitCost,
                                        hasFreight, freightCost != null ? freightCost : BigDecimal.ZERO,
                                        Instant.now());
    }

    public static ProductPurchaseEntry reconstitute(UUID id, UUID companyId, UUID productId,
                                                    int isoYear, int isoWeekNumber,
                                                    int quantity, BigDecimal unitCost,
                                                    boolean hasFreight, BigDecimal freightCost,
                                                    String supplierName, String invoiceNumber,
                                                    Instant createdAt, Instant updatedAt) {
        ProductPurchaseEntry e = new ProductPurchaseEntry(id, companyId, productId,
                                                          isoYear, isoWeekNumber, quantity,
                                                          unitCost, hasFreight, freightCost, createdAt);
        e.supplierName  = supplierName;
        e.invoiceNumber = invoiceNumber;
        e.updatedAt     = updatedAt;
        return e;
    }

    /** Custo total sem frete: unitCost × quantity. */
    public BigDecimal totalCost() {
        return unitCost.multiply(BigDecimal.valueOf(quantity))
                       .setScale(2, RoundingMode.HALF_UP);
    }

    /** Custo do frete por unidade: freightCost / quantity. Zero se hasFreight=false. */
    public BigDecimal freightPerUnit() {
        if (!hasFreight || freightCost.signum() == 0) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        return freightCost.divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP);
    }

    /** Custo total desembarcado: totalCost + freightCost. */
    public BigDecimal totalLandedCost() {
        return totalCost().add(freightCost).setScale(2, RoundingMode.HALF_UP);
    }

    public void annotate(String supplierName, String invoiceNumber) {
        this.supplierName  = supplierName;
        this.invoiceNumber = invoiceNumber;
        this.updatedAt     = Instant.now();
    }

    public WeekReference weekReference() {
        return new WeekReference(isoYear, isoWeekNumber);
    }
}
