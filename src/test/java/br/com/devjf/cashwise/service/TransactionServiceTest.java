package br.com.devjf.cashwise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.

        never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Testes unitários para TransactionService.
 * Valida regras de negócio relacionadas ao gerenciamento de lançamentos.
 * 
 * Casos de teste cobertos:
 * - CT005: Cadastro de lançamento único com sucesso
 * - CT007: Validação de valor negativo em lançamento
 * - CT008: Filtro de lançamentos por período
 * - CT009: Filtro por tipo e categoria
 * - CT012: Validação de campos obrigatórios em lançamento
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TransactionService")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private Category validCategory;
    private Transaction validTransaction;

    @BeforeEach
    void setUp() {
        // Arrange: Preparação de dados de teste
        validCategory = new Category();
        validCategory.setId(1L);
        validCategory.setName("Alimentação");

        validTransaction = new Transaction();
        validTransaction.setId(1L);
        validTransaction.setType(TransactionType.REVENUE);
        validTransaction.setCategory(validCategory);
        validTransaction.setAmount(new BigDecimal("1500.00"));
        validTransaction.setDescription("Salário");
        validTransaction.setRecurrency(RecurrencyType.UNIQUE);
        validTransaction.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CT005: Cadastro de Lançamento Único com Sucesso
    // ====================

    @Test
    @DisplayName("CT005 - Deve cadastrar lançamento único(RECEITA) com sucesso")
    void shouldRegisterSingleRevenueTransactionSuccessfully() {
        // Arrange
        Transaction newTransaction = new Transaction();
        newTransaction.setType(TransactionType.REVENUE);
        newTransaction.setCategory(validCategory);
        newTransaction.setAmount(new BigDecimal("1500.00"));
        newTransaction.setDescription("Salário");
        newTransaction.setRecurrency(RecurrencyType.UNIQUE);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(validTransaction);

        // Act
        Transaction result = transactionService.registerTransaction(newTransaction);

        // Assert
        assertNotNull(result, "Lançamento cadastrado não deve ser nulo");
        assertNotNull(result.getId(), "ID deve ser gerado");
        assertEquals(TransactionType.REVENUE, result.getType());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());

        assertEquals("Salário", result.getDescription());
        assertEquals(RecurrencyType.UNIQUE, result.getRecurrency());
        assertNotNull(result.getCreatedAt(), "Metadado criado_em deve estar presente");

        verify(transactionRepository, times(1)).save(newTransaction);
    }

    @Test
    @DisplayName("CT005 - Deve cadastrar lançamento único (DESPESA) com sucesso")
    void shouldRegisterSingleExpenseTransactionSuccessfully() {
        // Arrange
        Transaction expenseTransaction = new Transaction();
        expenseTransaction.setType(TransactionType.EXPENSE);
        expenseTransaction.setCategory(validCategory);
        expenseTransaction.setAmount(new BigDecimal("500.00"));
        expenseTransaction.setDescription("Aluguel");
        expenseTransaction.setRecurrency(RecurrencyType.UNIQUE);

        Transaction savedExpense = new Transaction();
        savedExpense.setId(2L);
        savedExpense.setType(TransactionType.EXPENSE);
        savedExpense.setAmount(new BigDecimal("500.00"));

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedExpense);

        // Act
        Transaction result = transactionService.registerTransaction(expenseTransaction);

        // Assert
        assertNotNull(result);
        assertEquals(TransactionType.EXPENSE, result.getType());
        verify(transactionRepository, times(1)).save(expenseTransaction);
    }

    @Test
    @DisplayName("CT005 - Deve definir data de criação automaticamente se não fornecida")
    void shouldSetCreatedAtAutomaticallyWhenNotProvided() {
        // Arrange
        Transaction transactionWithoutCreatedAt = new Transaction();
        transactionWithoutCreatedAt.setType(TransactionType.REVENUE);
        transactionWithoutCreatedAt.setCategory(validCategory);
        transactionWithoutCreatedAt.setAmount(new BigDecimal("1000.00"));
        transactionWithoutCreatedAt.setDescription("Freelance");
        transactionWithoutCreatedAt.setRecurrency(RecurrencyType.UNIQUE);
        transactionWithoutCreatedAt.setCreatedAt(null); // Sem data de criação

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            assertNotNull(saved.getCreatedAt(), "CreatedAt deve ser definido automaticamente");
            return saved;
        });

        // Act
        transactionService.registerTransaction(transactionWithoutCreatedAt);

        // Assert
        verify(transactionRepository, times(1)).save(transactionWithoutCreatedAt);
    }

    // ==================== CT007: Validação de Valor Negativo em Lançamento
    // ====================

    @Test
    @DisplayName("CT007 - Deve lançar exceção quando valor for negativo")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Arrange
        Transaction transactionWithNegativeAmount = new Transaction();
        transactionWithNegativeAmount.setType(TransactionType.EXPENSE);
        transactionWithNegativeAmount.setCategory(validCategory);
        transactionWithNegativeAmount.setAmount(new BigDecimal("-100.00"));
        transactionWithNegativeAmount.setDescription("Teste");
        transactionWithNegativeAmount.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithNegativeAmount),
                "Deve lançar IllegalArgumentException para valor negativo");

        assertTrue(exception.getMessage().contains("deve ser positivo"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT007 - Deve lançar exceção quando valor for zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        // Arrange
        Transaction transactionWithZeroAmount = new Transaction();
        transactionWithZeroAmount.setType(TransactionType.REVENUE);
        transactionWithZeroAmount.setCategory(validCategory);
        transactionWithZeroAmount.setAmount(BigDecimal.ZERO);
        transactionWithZeroAmount.setDescription("Teste");
        transactionWithZeroAmount.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithZeroAmount),
                "Deve lançar IllegalArgumentException para valor zero");

        assertTrue(exception.getMessage().contains("deve ser positivo"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT007 - Deve lançarexceção quando valor for nulo")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Arrange
        Transaction transactionWithNullAmount = new Transaction();
        transactionWithNullAmount.setType(TransactionType.REVENUE);
        transactionWithNullAmount.setCategory(validCategory);
        transactionWithNullAmount.setAmount(null);
        transactionWithNullAmount.setDescription("Teste");
        transactionWithNullAmount.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithNullAmount));

        assertTrue(exception.getMessage().contains("deve ser positivo"));
        verify(transactionRepository, never()).save(any());
    }

    // ==================== CT012: Validação de Campos Obrigatórios
    // ====================

    @Test
    @

    DisplayName("CT012 - Deve lançar exceção quando lançamento for nulo")
    void shouldThrowExceptionWhenTransactionIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(null),
                "Deve lançar IllegalArgumentException para lançamento nulo");

        assertEquals("Lançamento não pode ser nulo", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT012 - Deve lançar exceção quando tipo for nulo")
    void shouldThrowExceptionWhenTypeIsNull() {
        // Arrange
        Transaction transactionWithoutType = new Transaction();
        transactionWithoutType.setType(null);
        transactionWithoutType.setCategory(validCategory);
        transactionWithoutType.setAmount(new BigDecimal("100.00"));
        transactionWithoutType.setDescription("Teste");
        transactionWithoutType.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithoutType));

        assertTrue(exception.getMessage().contains("Tipo de lançamento é obrigatório"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT012 - Deve lançar exceção quando categoria for nula")
    void shouldThrowExceptionWhenCategoryIsNull() {
        // Arrange
        Transaction transactionWithoutCategory = new Transaction();
        transactionWithoutCategory.setType(TransactionType.REVENUE);
        transactionWithoutCategory.setCategory(null);
        transactionWithoutCategory.setAmount(new BigDecimal("100.00"));
        transactionWithoutCategory.setDescription("Teste");
        transactionWithoutCategory.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithoutCategory));

        assertTrue(exception.getMessage().contains("Categoria é obrigatória"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT012 - Deve lançar exceção quando descrição for nula")
    void shouldThrowExceptionWhenDescriptionIsNull() {
        // Arrange
        Transaction transactionWithoutDescription = new Transaction();
        transactionWithoutDescription.setType(TransactionType.REVENUE);
        transactionWithoutDescription.setCategory(validCategory);
        transactionWithoutDescription.setAmount(new BigDecimal("100.00"));
        transactionWithoutDescription.setDescription(null);
        transactionWithoutDescription.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithoutDescription));

        assertTrue(exception.getMessage().contains("Descrição é obrigatória"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT012 - Deve lançar exceção quando descrição for vazia")
    void shouldThrowExceptionWhenDescriptionIsEmpty() {
        // Arrange
        Transaction transactionWithEmptyDescription = new Transaction();
        transactionWithEmptyDescription.setType(TransactionType.REVENUE);
        transactionWithEmptyDescription.setCategory(validCategory);
        transactionWithEmptyDescription.setAmount(new BigDecimal("100.00"));
        transactionWithEmptyDescription.setDescription("");
        transactionWithEmptyDescription.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithEmptyDescription));

        assertTrue(exception.getMessage().contains("Descrição é obrigatória"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT012 - Deve lançar exceção quando descrição for apenas espaços")
    void shouldThrowExceptionWhenDescriptionIsBlank() {
        // Arrange
        Transaction transactionWithBlankDescription = new Transaction();
        transactionWithBlankDescription.setType(TransactionType.REVENUE);
        transactionWithBlankDescription.setCategory(validCategory);
        transactionWithBlankDescription.setAmount(new BigDecimal("100.00"));
        transactionWithBlankDescription.setDescription("   ");
        transactionWithBlankDescription.setRecurrency(RecurrencyType.UNIQUE);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.registerTransaction(transactionWithBlankDescription));

        assertTrue(exception.getMessage().contains("Descrição é obrigatória"));
        verify(transactionRepository, never()).save(any());
    }

    // ==================== CT008: Filtro de Lançamentos por Período
    // ====================

    @Test
    @DisplayName("CT008 - Deve filtrar lançamentos por período com sucesso")
    void shouldFilterTransactionsByPeriodSuccessfully() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 31);
        Pageable pageable = PageRequest.of(0, 10);

        List<Transaction> transactions = Arrays.asList(validTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(transactionRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactions(startDate, endDate, pageable);

        // Assert
        assertNotNull(result, "Resultado não deve ser nulo");
        assertEquals(1, result.getTotalElements(), "Deve retornar 1 lançamento");
        assertEquals(validTransaction.getId(), result.getContent().get(0).getId());

        verify(transactionRepository, times(1))
                .findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable));
    }

    @Test
    @DisplayName("CT008 - Deve retornar todos os lançamentos quando não houver filtro de período")
    void shouldReturnAllTransactionsWhenNoPeriodFilter() {
        // Arrange

        Pageable pageable = PageRequest.of(0, 10);
        List<Transaction> transactions = Arrays.asList(validTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(transactionRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactions(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, times(1)).findAll(pageable);
        verify(transactionRepository, never()).findByCreatedAtBetween(any(), any(), any());
    }

    @Test
    @DisplayName("CT008 - Deve retornar página vazia quando não houver lançamentos no período")
    void shouldReturnEmptyPageWhenNoTransactionsInPeriod() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Transaction> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);

        when(transactionRepository.findByCreatedAtBetween(any(), any(), any()))
                .thenReturn(emptyPage);

        // Act
        Page<Transaction> result = transactionService.listTransactions(startDate, endDate, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Página deve estar vazia");
        assertEquals(0, result.getTotalElements());
    }

    // ==================== CT009: Filtro por Tipo e Categoria ====================

    @Test
    @DisplayName("CT009 - Deve filtrar lançamentos por tipo (DESPESA)")
    void shouldFilterTransactionsByTypeExpense() {
        // Arrange
        TransactionType type = TransactionType.EXPENSE;
        Pageable pageable = PageRequest.of(0, 10);

        Transaction expenseTransaction = new Transaction();
        expenseTransaction.setType(TransactionType.EXPENSE);
        expenseTransaction.setAmount(new BigDecimal("500.00"));

        List<Transaction> transactions = Arrays.asList(expenseTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(transactionRepository.findByType(type, pageable)).thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactionsWithFilters(
                null, null, type, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.EXPENSE, result.getContent().get(0).getType());
        verify(transactionRepository, times(1)).findByType(type, pageable);
    }

    @Test
    @DisplayName("CT009 - Deve filtrar lançamentos por categoria")
    void shouldFilterTransactionsByCategory() {
        // Arrange
        Long categoryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Transaction> transactions = Arrays.asList(validTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(categoryService.findCategoryById(categoryId)).thenReturn(validCategory);
        when(transactionRepository.findByCategory(validCategory, pageable)).thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactionsWithFilters(
                null, null, null, categoryId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(validCategory.getId(), result.getContent().get(0).getCategory().getId());

        verify(categoryService, times(1)).findCategoryById(categoryId);
        verify(transactionRepository, times(1)).findByCategory(validCategory, pageable);
    }

    @Test
    @DisplayName("CT009 - Deve filtrar lançamentos combinando tipo e categoria")
    void shouldFilterTransactionsByTypeAndCategory() {
        // Arrange
        LocalDate startDate = LocalDate.of

        (2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 31);
        TransactionType type = TransactionType.EXPENSE;
        Long categoryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Transaction> transactions = Arrays.asList(validTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(categoryService.findCategoryById(categoryId)).thenReturn(validCategory);
        when(transactionRepository.findByCreatedAtBetweenAndTypeAndCategory(
                any(), any(), eq(type), eq(validCategory), eq(pageable))).thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactionsWithFilters(
                startDate, endDate, type, categoryId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(categoryService, times(1)).findCategoryById(categoryId);
        verify(transactionRepository, times(1))
                .findByCreatedAtBetweenAndTypeAndCategory(any(), any(), eq(type), eq(validCategory), eq(pageable));
    }

    @Test
    @DisplayName("CT009 - Deve filtrar lançamentos por período e tipo")
    void shouldFilterTransactionsByPeriodAndType() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 31);
        TransactionType type = TransactionType.REVENUE;
        Pageable pageable = PageRequest.of(0, 10);

        List<Transaction> transactions = Arrays.asList(validTransaction);
        Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, transactions.size());

        when(transactionRepository.findByCreatedAtBetweenAndType(any(), any(), eq(type), eq(pageable)))
                .thenReturn(expectedPage);

        // Act
        Page<Transaction> result = transactionService.listTransactionsWithFilters(startDate, endDate, type, null,
                pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository, times(1))
                .findByCreatedAtBetweenAndType(any(), any(), eq(type), eq(pageable));
    }

    // ==================== Testes Adicionais - Exclusão ====================

    @Test
    @DisplayName("Deve excluir lançamento existente com sucesso")
    void shouldDeleteExistingTransactionSuccessfully() {
        // Arrange
        Long transactionId = 1L;

        when(transactionRepository.existsById(transactionId)).thenReturn(true);
        doNothing().when(transactionRepository).deleteById(transactionId);

        // Act
        assertDoesNotThrow(() -> transactionService.deleteTransaction(transactionId));

        // Assert
        verify(transactionRepository, times(1)).existsById(transactionId);
        verify(transactionRepository, times(1)).deleteById(transactionId);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar excluir lançamento inexistente")

    void shouldThrowExceptionWhenDeletingNonExistentTransaction() {
        // Arrange
        Long nonExistentId = 999L;

        when(transactionRepository.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> transactionService.deleteTransaction(nonExistentId));

        assertTrue(exception.getMessage().contains("não encontrada"));
        verify(transactionRepository, times(1)).existsById(nonExistentId);
        verify(transactionRepository, never()).deleteById(any());
    }
}