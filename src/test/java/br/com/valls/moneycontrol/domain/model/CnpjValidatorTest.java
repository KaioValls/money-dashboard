package br.com.valls.moneycontrol.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjValidatorTest {

    @Test void valid_cnpj_digits_only()     { assertTrue(CnpjValidator.isValid("11222333000181")); }
    @Test void valid_cnpj_formatted()        { assertTrue(CnpjValidator.isValid("11.222.333/0001-81")); }
    @Test void invalid_all_same_digits()     { assertFalse(CnpjValidator.isValid("11111111111111")); }
    @Test void invalid_all_zeros()           { assertFalse(CnpjValidator.isValid("00000000000000")); }
    @Test void invalid_too_short()           { assertFalse(CnpjValidator.isValid("1122233300018")); }
    @Test void invalid_too_long()            { assertFalse(CnpjValidator.isValid("112223330001810")); }
    @Test void invalid_null()                { assertFalse(CnpjValidator.isValid(null)); }
    @Test void invalid_wrong_check_digit()   { assertFalse(CnpjValidator.isValid("11222333000182")); }
    @Test void valid_real_cnpj_b()           { assertTrue(CnpjValidator.isValid("45997418000153")); }
}
