package br.com.valls.moneycontrol.domain.model;

/**
 * Utilitário de domínio para validação de CNPJ.
 * Algoritmo módulo 11 — sem dependências externas.
 */
public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST  = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {}

    /**
     * Valida um CNPJ em formato numérico (14 dígitos) ou formatado (XX.XXX.XXX/XXXX-XX).
     * Rejeita sequências com todos os dígitos iguais (ex: "11111111111111").
     */
    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;

        String digits = cnpj.replaceAll("[^\\d]", "");

        if (digits.length() != 14) return false;
        if (allSameDigits(digits)) return false;

        return matchesCheckDigit(digits, WEIGHTS_FIRST,  12)
            && matchesCheckDigit(digits, WEIGHTS_SECOND, 13);
    }

    private static boolean allSameDigits(String digits) {
        char first = digits.charAt(0);
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != first) return false;
        }
        return true;
    }

    private static boolean matchesCheckDigit(String digits, int[] weights, int position) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        int expected  = remainder < 2 ? 0 : 11 - remainder;
        return Character.getNumericValue(digits.charAt(position)) == expected;
    }
}
