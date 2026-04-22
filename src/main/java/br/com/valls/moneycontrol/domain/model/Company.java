package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade raiz — representa um CNPJ/empresa dona das lojas e configurações tributárias.
 * Construtor privado; uso exclusivo via {@link #create} ou {@link #reconstitute}.
 */
@Getter
public class Company {

    private final UUID      id;
    private final String    cnpjDigits;
    private       String    legalName;
    private       String    tradeName;
    private final TaxRegime taxRegime;
    private       String    simplesAnnex;
    private       boolean   active;
    private final Instant   createdAt;
    private       Instant   updatedAt;

    private Company(UUID id, String cnpjDigits, String legalName,
                    TaxRegime taxRegime, Instant createdAt) {
        this.id         = id;
        this.cnpjDigits = cnpjDigits;
        this.legalName  = legalName;
        this.taxRegime  = taxRegime;
        this.active     = true;
        this.createdAt  = createdAt;
        this.updatedAt  = createdAt;
    }

    /** Cria uma nova empresa — valida CNPJ e campos obrigatórios. */
    public static Company create(String cnpjDigits, String legalName, TaxRegime taxRegime) {
        if (!CnpjValidator.isValid(cnpjDigits)) {
            throw new IllegalArgumentException("CNPJ inválido: " + cnpjDigits);
        }
        if (legalName == null || legalName.isBlank()) {
            throw new IllegalArgumentException("Razão social não pode ser vazia");
        }
        Objects.requireNonNull(taxRegime, "Regime tributário é obrigatório");

        String digits = cnpjDigits.replaceAll("[^\\d]", "");
        return new Company(UUID.randomUUID(), digits, legalName, taxRegime, Instant.now());
    }

    /** Reconstitui a partir da persistência sem revalidar o CNPJ. */
    public static Company reconstitute(UUID id, String cnpjDigits, String legalName,
                                       String tradeName, TaxRegime taxRegime,
                                       String simplesAnnex, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        Company c       = new Company(id, cnpjDigits, legalName, taxRegime, createdAt);
        c.tradeName     = tradeName;
        c.simplesAnnex  = simplesAnnex;
        c.active        = active;
        c.updatedAt     = updatedAt;
        return c;
    }

    public boolean isSimples() {
        return TaxRegime.SIMPLES_NACIONAL == taxRegime;
    }

    public void updateLegalName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Razão social não pode ser vazia");
        }
        this.legalName  = newName;
        this.updatedAt  = Instant.now();
    }

    public void updateTradeName(String name) {
        this.tradeName = name;
        this.updatedAt = Instant.now();
    }

    public void updateSimplesAnnex(String annex) {
        this.simplesAnnex = annex;
        this.updatedAt    = Instant.now();
    }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active    = true;
        this.updatedAt = Instant.now();
    }
}
