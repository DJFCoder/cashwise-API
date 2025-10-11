package br.com.devjf.cashwise.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devjf.cashwise.domain.dto.category.CategoryRequest;
import br.com.devjf.cashwise.domain.dto.category.CategoryResponse;
import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.mapper.CategoryMapper;
import br.com.devjf.cashwise.service.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para gerenciamento de categorias.
 * <p>
 * Fornece endpoints para operações CRUD (Create, Read, Update, Delete) de categorias.
 * Todas as operações seguem os padrões REST e retornam respostas padronizadas.
 * </p>
 * <p>
 * Endpoints disponíveis:
 * - POST /api/categoria - Cadastrar nova categoria
 * - GET /api/categoria/listar - Listar todas as categorias
 * - GET /api/categoria/{id} - Buscar categoria por ID
 * - PUT /api/categoria/{id} - Atualizar categoria existente
 * - DELETE /api/categoria/{id} - Excluir categoria
 * </p>
 * 
 * @author devjf
 */
@RestController
@RequestMapping("/api/categoria")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    /**
     * Cadastra uma nova categoria.
     * <p>
     * Endpoint: POST /api/categories
     * </p>
     * <p>
     * Validações aplicadas:
     * - Nome não pode ser vazio
     * - Nome deve ter no máximo 100 caracteres
     * - Nome não pode ser duplicado (validado no serviço)
     * </p>
     * 
     * @param request DTO com os dados da categoria a ser criada
     * @return ResponseEntity com status 201 (Created) e a categoria criada
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> registerCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("Recebida requisição para cadastrar categoria: {}", request.name());
        
        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryService.registerCategory(category);
        CategoryResponse response = categoryMapper.toResponse(savedCategory);
        
        log.info("Categoria cadastrada com sucesso - ID: {}", savedCategory.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todas as categorias cadastradas.
     * <p>
     * Endpoint: GET /api/categories
     * </p>
     * 
     * @return ResponseEntity com status 200 (OK) e lista de categorias
     */
    @GetMapping("/listar")
    public ResponseEntity<List<CategoryResponse>> listAllCategories() {
        log.info("Recebida requisição para listar todas as categorias");
        
        List<Category> categories = categoryService.listAllCategories();
        List<CategoryResponse> response = categoryMapper.toResponseList(categories);
        
        log.info("Retornando {} categoria(s)", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Busca uma categoria por ID.
     * <p>
     * Endpoint: GET /api/categories/{id}
     * </p>
     * 
     * @param id identificador único da categoria
     * @return ResponseEntity com status 200 (OK) e a categoria encontrada
     * @throws EntityNotFoundException se a categoria não existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findCategoryById(@PathVariable Long id) {
        log.info("Recebida requisição para buscar categoria com ID: {}", id);
        
        Category category = categoryService.findCategoryById(id);
        CategoryResponse response = categoryMapper.toResponse(category);
        
        log.info("Categoria encontrada: {}", category.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza uma categoria existente.
     * <p>
     * Endpoint: PUT /api/categories/{id}
     * </p>
     * <p>
     * Validações aplicadas:
     * - Categoria deve existir
     * - Nome não pode ser vazio
     * - Nome deve ter no máximo 100 caracteres
     * - Nome não pode ser duplicado (validado no serviço)
     * </p>
     * 
     * @param id identificador único da categoria a ser atualizada
     * @param request DTO com os novos dados da categoria
     * @return ResponseEntity com status 200 (OK) e a categoria atualizada
     * @throws EntityNotFoundException se a categoria não existir
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        
        log.info("Recebida requisição para atualizar categoria ID: {} com nome: {}", id, request.name());
        
        Category existingCategory = categoryService.findCategoryById(id);
        categoryMapper.updateEntityFromRequest(existingCategory, request);
        Category updatedCategory = categoryService.registerCategory(existingCategory);
        CategoryResponse response = categoryMapper.toResponse(updatedCategory);
        
        log.info("Categoria atualizada com sucesso - ID: {}", updatedCategory.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Exclui uma categoria.
     * <p>
     * Endpoint: DELETE /api/categories/{id}
     * </p>
     * <p>
     * Validações aplicadas:
     * - Categoria deve existir
     * - Categoria não pode ter lançamentos associados (validado no serviço)
     * </p>
     * 
     * @param id identificador único da categoria a ser excluída
     * @return ResponseEntity com status 204 (No Content)
     * @throws EntityNotFoundException se a categoria não existir
     * @throws BusinessException se a categoria tiver lançamentos associados
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Recebida requisição para excluir categoria ID: {}", id);
        
        categoryService.deleteCategory(id);
        
        log.info("Categoria excluída com sucesso - ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
