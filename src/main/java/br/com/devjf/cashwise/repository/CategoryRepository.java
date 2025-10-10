package br.com.devjf.cashwise.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.devjf.cashwise.domain.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Verifica se já existe uma categoria com o nome especificado.
     * Útil para validação e prevenção de duplicatas.
     * 
     * @param name o nome da categoria a ser verificado
     * @return true se existir categoria com esse nome, false caso contrário
     */
    boolean existsByName(String name);

    /**
     * Busca uma categoria pelo nome ignorando diferenças entre maiúsculas e
     * minúsculas.
     * 
     * @param name o nome da categoria a ser buscado (case-insensitive)
     * @return Optional contendo a categoria encontrada, ou Optional vazio se não
     *         existir
     */
    Optional<Category> findByNameIgnoreCase(String name);
}
