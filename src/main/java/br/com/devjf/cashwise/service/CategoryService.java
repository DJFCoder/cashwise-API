package br.com.devjf.cashwise.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.repository.CategoryRepository;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;

import br.com.devjf.cashwise.exception.BusinessException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Registra uma nova categoria no sistema.
     * Valida se a categoria é válida e se o nome não está duplicado.
     * 
     * @param category entidade Category a ser cadastrada
     * @return categoria cadastrada com ID gerado
     * @throws IllegalArgumentException se a categoria for nula ou nome inválido
     * @throws BusinessException        se já existir categoria com o mesmo nome
     */
    public Category registerCategory(Category category) {
        validateCategory(category);
        validateDuplicateName(category.getName());
        return categoryRepository.save(category);
    }

    /**
     * Busca uma categoria no banco de dados pelo seu identificador.
     * <p>
     *
     * @param categoryId identificador único da categoria
     * @return entidade Category encontrada
     * @throws EntityNotFoundException se nenhuma categoria com o ID fornecido
     *                                 for encontrada
     */
    public Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoria com ID " + categoryId + " não encontrada"));
    }

    /**
     * Lista todas as categorias cadastradas no sistema.
     * Operação somente leitura.
     * 
     * @return lista contendo todas as categorias
     */
    @Transactional(readOnly = true)
    public List<Category> listAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Exclui uma categoria do sistema.
     * Valida se a categoria existe e se não possui lançamentos vinculados.
     * 
     * @param id identificador da categoria a ser excluída
     * @throws EntityNotFoundException se a categoria não existir
     * @throws BusinessException       se existirem lançamentos vinculados à
     *                                 categoria
     */
    public void deleteCategory(Long id) {
        // Valida se categoria existe
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Categoria com ID " + id + " não encontrada");
        }

        // REGRA DE NEGÓCIO: Não pode excluir se houver lançamentos vinculados
        if (transactionRepository.existsByCategoryId(id)) {
            throw new BusinessException(
                    "Não é possível excluir a categoria pois existem lançamentos vinculados a ela");
        }

        categoryRepository.deleteById(id);
    }

    /**
     * Valida se a categoria possui dados obrigatórios preenchidos.
     * 
     * @param category categoria a ser validada
     * @throws IllegalArgumentException se categoria for nula ou nome vazio
     */
    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Categoria não pode ser nula");
        }
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório");
        }
    }

    /**
     * Valida se já existe uma categoria com o nome informado.
     * 
     * @param name nome da categoria a ser verificado
     * @throws BusinessException se já existir categoria com o mesmo nome
     */
    private void validateDuplicateName(String name) {
        if (categoryRepository.existsByName(name.trim())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + name);
        }
    }

}
