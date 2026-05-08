package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_price_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ListingPriceHistoryJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
