package br.com.devjf.cashwise.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import

org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.exception.BusinessException;
import br.com.devjf.cashwise.repository.CategoryRepository;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Testes unitários para CategoryService.
 * Valida regras de negócio relacionadas ao gerenciamento de categorias.
 * 
 * Casos de teste cobertos:
 * - CT001: Cadastro de categoria com sucesso
 * - CT002: Cadastro de categoria com nome inválido
 * - CT003: Exclusão de categoria sem vínculo
 * - CT004: Bloqueio de exclusão de categoria com lançamentos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category validCategory;

    @BeforeEach
    void setUp() {
        // Arrange: Preparação de dados de teste
        validCategory = new Category();
        validCategory.setId(1L);
        validCategory.setName("Alimentação");
        validCategory.setCreatedAt(LocalDateTime.now());
        validCategory.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== CT001: Cadastro de Categoria com Sucesso
    // ====================

    @Test
    @DisplayName("CT001 - Deve cadastrar categoria com sucesso quando dados válidos")
    void shouldRegisterCategorySuccessfully() {
        // Arrange
        Category newCategory = new Category();
        newCategory.setName("Alimentação");

        when(categoryRepository.existsByName(anyString())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(validCategory);

        // Act

        Category result = categoryService.registerCategory(newCategory);

        // Assert
        assertNotNull(result, "Categoria cadastrada não deve ser nula");
        assertNotNull(result.getId(), "ID deve ser gerado");
        assertEquals("Alimentação", result.getName(), "Nome deve corresponder ao informado");
        assertNotNull(result.getCreatedAt(), "Metadado criado_em deve estar presente");
        assertNotNull(result.getUpdatedAt(), "Metadado atualizado_em deve estar presente");

        // Verify: Verifica interações com os mocks
        verify(categoryRepository, times(1)).existsByName("Alimentação");
        verify(categoryRepository, times(1)).save(newCategory);
    }

    @Test
    @DisplayName("CT001 - Deve validar nome antes de cadastrar categoria")
    void shouldValidateNameBeforeRegister() {
        // Arrange
        Category newCategory = new Category();
        newCategory.setName("Transporte");

        when(categoryRepository.existsByName("Transporte")).thenReturn(false);

        when(categoryRepository.save(any(Category.class))).thenReturn(validCategory);

        // Act
        categoryService.registerCategory(newCategory);

        // Assert
        verify(categoryRepository).existsByName("Transporte");
        verify(categoryRepository).save(newCategory);
    }

    // ==================== CT002: Cadastro de Categoria com Nome Inválido
    // ====================

    @Test
    @DisplayName("CT002 - Deve lançar exceção quando categoria for nula")
    void shouldThrowExceptionWhenCategoryIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.registerCategory(null),
                "Deve lançar IllegalArgumentException para categoria nula");

        assertEquals("Categoria não pode ser nula", exception.getMessage());

        // Verify: Nenhuma interação com repositório deve ocorrer
        verify(categoryRepository, never()).save

        (any());
    }

    @Test
    @DisplayName("CT002 - Deve lançar exceção quando nome for vazio")
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Arrange
        Category categoryWithEmptyName = new Category();
        categoryWithEmptyName.setName("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.registerCategory(categoryWithEmptyName),
                "Deve lançar IllegalArgumentException para nome vazio");

        assertEquals("Nome da categoria é obrigatório", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT002 - Deve lançar exceção quando nome for apenas espaços em branco")
    void shouldThrowExceptionWhenNameIsBlank() {
        // Arrange
        Category categoryWithBlankName = new Category();
        categoryWithBlankName.setName("   ");

        // Act & Assert

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.registerCategory(categoryWithBlankName),
                "Deve lançar IllegalArgumentException para nome com apenas espaços");

        assertEquals("Nome da categoria é obrigatório", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT002 - Deve lançar exceção quando nome for nulo")
    void shouldThrowExceptionWhenNameIsNull() {
        // Arrange
        Category categoryWithNullName = new Category();
        categoryWithNullName.setName(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.registerCategory(categoryWithNullName),
                "Deve lançar IllegalArgumentException para nome nulo");

        assertEquals("Nome da categoria é obrigatório", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT002 - Deve lançar exceção quando nome já existir (duplicado)")
    void shouldThrowExceptionWhenNameAlreadyExists() {
        // Arrange
        Category duplicateCategory = new Category();
        duplicateCategory.setName("Alimentação");

        when(categoryRepository.existsByName("Alimentação")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.registerCategory(duplicateCategory),
                "Deve lançar BusinessException para nome duplicado");

        assertTrue(exception.getMessage().contains("Já existe uma categoria com o nome"));
        verify(categoryRepository, times(1)).existsByName("Alimentação");
        verify(categoryRepository, never()).save(any());
    }

    // ==================== CT003: Exclusão de Categoria sem Vínculo
    // ====================

    @Test
    @DisplayName("CT003 - Deve excluir categoria sem lançamentos vinculados")
    void shouldDeleteCategoryWithoutTransactions() {
        // Arrange
        Long categoryId = 1L;

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(false);
        doNothing().when(categoryRepository).deleteById(categoryId);

        // Act
        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId));

        // Assert
        verify(categoryRepository, times(1)).existsById(categoryId);
        verify(transactionRepository, times(1)).existsByCategoryId(categoryId);
        verify(categoryRepository, times(1)).deleteById(categoryId);
    }

    @Test
    @DisplayName("CT003 - Deve lançar exceção ao tentar excluir categoria inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentCategory() {
        // Arrange
        Long nonExistentId = 999L;

        when(categoryRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.deleteCategory(nonExistentId),
                "Deve lançar EntityNotFoundException para categoria inexistente");

        assertTrue(exception.getMessage().contains("não encontrada"));
        verify(categoryRepository, times(1)).existsById(nonExistentId);
        verify(transactionRepository, never()).existsByCategoryId(any());
        verify(categoryRepository, never()).deleteById(any());
    }

    // ==================== CT004: Bloqueio de Exclusão de Categoria com Lançamentos
    // ====================

    @Test
    @DisplayName("CT004 - Deve bloquear exclusão de categoria com lançamentos vinculados")
    void shouldBlockDeletionOfCategoryWithTransactions() {
        // Arrange
        Long categoryId = 1L

        ;

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.deleteCategory(categoryId),
                "Deve lançar BusinessException ao tentar excluir categoria com vínculos");

        assertTrue(exception.getMessage().contains("existem lançamentos vinculados"));

        // Verify: Validações foram feitas mas exclusão não foi executada
        verify(categoryRepository, times(1)).existsById(categoryId);
        verify(transactionRepository, times(1)).existsByCategoryId(categoryId);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("CT004 - Deve validar integridade referencial antes de excluir")
    void shouldValidateReferentialIntegrityBeforeDelete() {

        // Arrange
        Long categoryId = 1L;

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> categoryService.deleteCategory(categoryId));

        // Verify: Ordem de validações
        verify(categoryRepository).existsById(categoryId);
        verify(transactionRepository).existsByCategoryId(categoryId);
    }

    // ==================== Testes Adicionais - Listagem ====================

    @Test
    @DisplayName("Deve listar todas as categorias cadastradas")
    void shouldListAllCategories() {
        // Arrange
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Alimentação");

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Transporte");

        List<Category> categories = Arrays.asList(category1, category2);

        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<Category> result = categoryService.listAllCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Alimentação", result.get(0).getName());
        assertEquals("Transporte", result.get(1).getName());

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver categorias")
    void shouldReturnEmptyListWhenNoCategoriesExist() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<Category> result = categoryService.listAllCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar categoria por ID com sucesso")
    void shouldFindCategoryByIdSuccessfully() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));

        // Act
        Category result = categoryService.findCategoryById(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Alimentação", result.getName());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar categoria inexistente por ID")
    void shouldThrowExceptionWhenCategoryNotFoundById() {
        // Arrange
        Long nonExistentId = 999L;
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.findCategoryById(nonExistentId));

        assertTrue(exception

                .getMessage().contains("não encontrada"));
        verify(categoryRepository, times(1)).findById(nonExistentId);
    }
}