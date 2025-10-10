package br.com.devjf.cashwise.service;

import org.springframework.stereotype.Service;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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

}
