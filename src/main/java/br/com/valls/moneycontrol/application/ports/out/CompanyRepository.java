package br.com.valls.moneycontrol.application.ports.out;

import br.com.valls.moneycontrol.domain.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de saída para persistência de empresas.
 * Implementado exclusivamente por {@code CompanyRepositoryAdapter} (infrastructure layer).
 */
public interface CompanyRepository {

    /** Persiste ou atualiza uma empresa e retorna a instância salva. */
    Company save(Company company);

    /** Busca empresa pelo ID. */
    Optional<Company> findById(UUID id);

    /** Busca empresa pelo CNPJ (somente dígitos). */
    Optional<Company> findByCnpjDigits(String cnpjDigits);

    /** Retorna todas as empresas cadastradas. */
    List<Company> findAll();

    /** Verifica se já existe empresa com o CNPJ informado. */
    boolean existsByCnpjDigits(String cnpjDigits);
}
