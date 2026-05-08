package br.com.valls.moneycontrol.infrastructure.persistence.entity;

import br.com.valls.moneycontrol.domain.enums.AdsRatioMode;
import br.com.valls.moneycontrol.domain.enums.StoreStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "marketplace_account_id", nullable = false)
    private UUID marketplaceAccountId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "marketplace_id", nullable = false)
    private UUID marketplaceId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StoreStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "ads_ratio_mode", nullable = false, length = 20)
    private AdsRatioMode adsRatioMode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
