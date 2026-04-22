package br.com.valls.moneycontrol.domain.model;

import br.com.valls.moneycontrol.domain.enums.TaxRegime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyTest {

    private static final String VALID_CNPJ = "11222333000181";

    // ── critério: CNPJ inválido lança IllegalArgumentException ────────────────
    @Test
    void create_with_invalid_cnpj_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Company.create("00000000000000", "Empresa X", TaxRegime.SIMPLES_NACIONAL));
    }

    @Test
    void create_with_null_cnpj_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Company.create(null, "Empresa X", TaxRegime.LUCRO_REAL));
    }

    @Test
    void create_with_blank_legal_name_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Company.create(VALID_CNPJ, "  ", TaxRegime.LUCRO_REAL));
    }

    @Test
    void create_with_null_tax_regime_throws() {
        assertThrows(NullPointerException.class,
                () -> Company.create(VALID_CNPJ, "Empresa X", null));
    }

    // ── critério: empresa criada com sucesso ──────────────────────────────────
    @Test
    void create_valid_company() {
        var c = Company.create(VALID_CNPJ, "Empresa Válida", TaxRegime.SIMPLES_NACIONAL);
        assertNotNull(c.getId());
        assertEquals("11222333000181", c.getCnpjDigits());
        assertEquals("Empresa Válida", c.getLegalName());
        assertEquals(TaxRegime.SIMPLES_NACIONAL, c.getTaxRegime());
        assertTrue(c.isActive());
    }

    @Test
    void create_strips_cnpj_formatting() {
        var c = Company.create("11.222.333/0001-81", "Empresa X", TaxRegime.LUCRO_PRESUMIDO);
        assertEquals("11222333000181", c.getCnpjDigits());
    }

    // ── critério: isSimples() ─────────────────────────────────────────────────
    @Test
    void is_simples_true_for_simples_nacional() {
        var c = Company.create(VALID_CNPJ, "Empresa S", TaxRegime.SIMPLES_NACIONAL);
        assertTrue(c.isSimples());
    }

    @Test
    void is_simples_false_for_lucro_real() {
        var c = Company.create(VALID_CNPJ, "Empresa LR", TaxRegime.LUCRO_REAL);
        assertFalse(c.isSimples());
    }

    // ── deactivate/activate ───────────────────────────────────────────────────
    @Test
    void deactivate_sets_active_false() {
        var c = Company.create(VALID_CNPJ, "Empresa D", TaxRegime.LUCRO_REAL);
        c.deactivate();
        assertFalse(c.isActive());
    }

    @Test
    void activate_after_deactivate() {
        var c = Company.create(VALID_CNPJ, "Empresa A", TaxRegime.LUCRO_REAL);
        c.deactivate();
        c.activate();
        assertTrue(c.isActive());
    }

    // ── nenhum import Spring/JPA ──────────────────────────────────────────────
    @Test
    void no_spring_or_jpa_imports() {
        for (var field : Company.class.getDeclaredFields()) {
            String type = field.getType().getName();
            assertFalse(type.startsWith("org.springframework"), "campo " + field.getName());
            assertFalse(type.startsWith("jakarta.persistence"),  "campo " + field.getName());
        }
    }
}
