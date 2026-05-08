package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Chave composta para {@link ProductPurchaseEntryJpaEntity}.
 * Necessária porque a tabela product_purchase_entries é particionada por iso_year
 * e a PK do PostgreSQL é (id, iso_year).
 */
public class ProductPurchaseEntryId implements Serializable {

    private UUID id;
    private int  isoYear;

    public ProductPurchaseEntryId() {}

    public ProductPurchaseEntryId(UUID id, int isoYear) {
        this.id      = id;
        this.isoYear = isoYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductPurchaseEntryId that)) return false;
        return isoYear == that.isoYear && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, isoYear);
    }
}
