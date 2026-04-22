package br.com.valls.moneycontrol.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class Product {

    private final UUID    id;
    private final UUID    companyId;
    private final String  sku;
    private       String  name;
    private       String  category;
    private       String  eanGtin;
    private       Integer weightGrams;
    private       boolean inStock;
    private       boolean active;
    private final Instant createdAt;
    private       Instant updatedAt;

    private Product(UUID id, UUID companyId, String sku, String name, Instant createdAt) {
        this.id        = id;
        this.companyId = companyId;
        this.sku       = sku;
        this.name      = name;
        this.inStock   = true;
        this.active    = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Product create(UUID companyId, String sku, String name) {
        Objects.requireNonNull(companyId, "companyId é obrigatório");
        if (sku  == null || sku.isBlank())  throw new IllegalArgumentException("SKU não pode ser vazio");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio");
        return new Product(UUID.randomUUID(), companyId, sku, name, Instant.now());
    }

    public static Product reconstitute(UUID id, UUID companyId, String sku, String name,
                                       String category, String eanGtin, Integer weightGrams,
                                       boolean inStock, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        Product p    = new Product(id, companyId, sku, name, createdAt);
        p.category   = category;
        p.eanGtin    = eanGtin;
        p.weightGrams = weightGrams;
        p.inStock    = inStock;
        p.active     = active;
        p.updatedAt  = updatedAt;
        return p;
    }

    public void update(String name, String category, String eanGtin, Integer weightGrams) {
        if (name != null && !name.isBlank()) this.name = name;
        if (category    != null) this.category    = category;
        if (eanGtin     != null) this.eanGtin     = eanGtin;
        if (weightGrams != null) this.weightGrams = weightGrams;
        this.updatedAt = Instant.now();
    }

    public void activate()   { this.active = true;  this.updatedAt = Instant.now(); }
    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
    public void markInStock()    { this.inStock = true;  this.updatedAt = Instant.now(); }
    public void markOutOfStock() { this.inStock = false; this.updatedAt = Instant.now(); }
}
