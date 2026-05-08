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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketplaceAccountJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "marketplace_id", nullable = false)
    private UUID marketplaceId;

    @Column(name = "seller_id_external", length = 100)
    private String sellerIdExternal;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
