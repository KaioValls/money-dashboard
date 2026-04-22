package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Marketplace {

    private final UUID    id;
    private       String  name;
    private final String  slug;
    private       String  logoUrl;
    private       String  websiteUrl;
    private       int     paymentCycleDays;
    private       boolean active;
    private final Instant createdAt;
    private       Instant updatedAt;

    private Marketplace(UUID id, String name, String slug, int paymentCycleDays, Instant createdAt) {
        this.id               = id;
        this.name             = name;
        this.slug             = slug;
        this.paymentCycleDays = paymentCycleDays;
        this.active           = true;
        this.createdAt        = createdAt;
        this.updatedAt        = createdAt;
    }

    public static Marketplace create(String name, String slug, int paymentCycleDays) {
        if (name == null || name.isBlank())   throw new IllegalArgumentException("Nome não pode ser vazio");
        if (slug == null || slug.isBlank())   throw new IllegalArgumentException("Slug não pode ser vazio");
        if (paymentCycleDays <= 0)            throw new IllegalArgumentException("Ciclo de pagamento deve ser positivo");
        return new Marketplace(UUID.randomUUID(), name, slug, paymentCycleDays, Instant.now());
    }

    public static Marketplace reconstitute(UUID id, String name, String slug, String logoUrl,
                                           String websiteUrl, int paymentCycleDays,
                                           boolean active, Instant createdAt, Instant updatedAt) {
        Marketplace m     = new Marketplace(id, name, slug, paymentCycleDays, createdAt);
        m.logoUrl         = logoUrl;
        m.websiteUrl      = websiteUrl;
        m.active          = active;
        m.updatedAt       = updatedAt;
        return m;
    }

    public boolean isActive() { return active; }

    public void update(String name, String logoUrl, String websiteUrl, int paymentCycleDays) {
        if (name != null && !name.isBlank())  this.name = name;
        if (logoUrl    != null)               this.logoUrl = logoUrl;
        if (websiteUrl != null)               this.websiteUrl = websiteUrl;
        if (paymentCycleDays > 0)             this.paymentCycleDays = paymentCycleDays;
        this.updatedAt = Instant.now();
    }

    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
    public void activate()   { this.active = true;  this.updatedAt = Instant.now(); }
}
