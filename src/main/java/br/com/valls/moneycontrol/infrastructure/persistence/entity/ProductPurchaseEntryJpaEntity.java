package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA para a tabela particionada {@code product_purchase_entries}.
 * Usa {@link IdClass} com chave composta (id, iso_year) — obrigatória para
 * tabelas particionadas no PostgreSQL.
 */
@Entity
@Table(name = "product_purchase_entries")
@IdClass(ProductPurchaseEntryId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductPurchaseEntryJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Id
    @Column(name = "iso_year", nullable = false)
    private int isoYear;

    @Column(name = "iso_week_number", nullable = false)
    private int isoWeekNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "has_freight", nullable = false)
    private boolean hasFreight;

    @Column(name = "freight_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightCost;

    @Column(name = "freight_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal freightPerUnit;

    @Column(name = "total_landed_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLandedCost;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
