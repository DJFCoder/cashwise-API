package br.com.devjf.cashwise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Testes unitários para RecurrencyService.
 * Valida regras de negócio relacionadas à geração automática de lançamentos recorrentes.
 * 
 * Casos de teste cobertos:
 * - CT006: Geração automática de lançamentos recorrentes mensais
 * - Processamento de recorrências ativas
 * - Ativação e desativação de recorrências
 * - Definição de data de término
 * - Validações de lançamentos originais vs filhos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - RecurrencyService")
class RecurrencyServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RecurrencyService recurrencyService;

    private Category validCategory;
    private Transaction originalMonthlyTransaction;
    private Transaction originalDailyTransaction;
    private Transaction originalWeeklyTransaction;
    private Transaction originalQuarterlyTransaction;
    private Transaction originalAnnualTransaction;

    @BeforeEach
    void setUp() {
        // Arrange: Preparação de dados de teste
        validCategory = new Category();
        validCategory.setId(1L);
        validCategory.setName("Moradia");

        // Lançamento recorrente mensal original
        originalMonthlyTransaction = new Transaction();
        originalMonthlyTransaction.setId(1L);
        originalMonthlyTransaction.setType(TransactionType.EXPENSE);
        originalMonthlyTransaction.setCategory(validCategory);
        originalMonthlyTransaction.setAmount(new BigDecimal("500.00"));
        originalMonthlyTransaction.setDescription("Aluguel");
        originalMonthlyTransaction.setRecurrency(RecurrencyType.MONTHLY);
        originalMonthlyTransaction.setRecurrencyActive(true);
        originalMonthlyTransaction.setParentTransactionId(null);
        originalMonthlyTransaction.setCreatedAt(LocalDate.of(2025, 10, 11).atStartOfDay());

        // Lançamento recorrente diário
        originalDailyTransaction = new Transaction();
        originalDailyTransaction.setId(2L);
        originalDailyTransaction.setType(TransactionType.EXPENSE);
        originalDailyTransaction.setCategory(validCategory);
        originalDailyTransaction.setAmount(new BigDecimal("50.00"));
        originalDailyTransaction.setDescription("Transporte");
        originalDailyTransaction.setRecurrency(RecurrencyType.DAILY);
        originalDailyTransaction.setRecurrencyActive(true);
        originalDailyTransaction.setParentTransactionId(null);
        originalDailyTransaction.setCreatedAt(LocalDate.now().minusDays(2).atStartOfDay());

        // Lançamento recorrente semanal
        originalWeeklyTransaction = new Transaction();
        originalWeeklyTransaction.setId(3L);
        originalWeeklyTransaction.setType(TransactionType.EXPENSE);
        originalWeeklyTransaction.setCategory(validCategory);
        originalWeeklyTransaction.setAmount(new BigDecimal("100.00"));
        originalWeeklyTransaction.setDescription("Academia");
        originalWeeklyTransaction.setRecurrency(RecurrencyType.WEEKLY);
        originalWeeklyTransaction.setRecurrencyActive(true);
        originalWeeklyTransaction.setParentTransactionId(null);
        originalWeeklyTransaction.setCreatedAt(LocalDate.now().minusWeeks(2).atStartOfDay());

        // Lançamento recorrente trimestral
        originalQuarterlyTransaction = new Transaction();
        originalQuarterlyTransaction.setId(4L);
        originalQuarterlyTransaction.setType(TransactionType.EXPENSE);
        originalQuarterlyTransaction.setCategory(validCategory);
        originalQuarterlyTransaction.setAmount(new BigDecimal("300.00"));
        originalQuarterlyTransaction.setDescription("Seguro");
        originalQuarterlyTransaction.setRecurrency(RecurrencyType.QUARTERLY);
        originalQuarterlyTransaction.setRecurrencyActive(true);
        originalQuarterlyTransaction.setParentTransactionId(null);
        originalQuarterlyTransaction.setCreatedAt(LocalDate.now().minusMonths(4).atStartOfDay());

        // Lançamento recorrente anual
        originalAnnualTransaction = new Transaction();
        originalAnnualTransaction.setId(5L);
        originalAnnualTransaction.setType(TransactionType.EXPENSE);
        originalAnnualTransaction.setCategory(validCategory);
        originalAnnualTransaction.setAmount(new BigDecimal("1200.00"));
        originalAnnualTransaction.setDescription("IPTU");
        originalAnnualTransaction.setRecurrency(RecurrencyType.ANNUAL);
        originalAnnualTransaction.setRecurrencyActive(true);
        originalAnnualTransaction.setParentTransactionId(null);
        originalAnnualTransaction.setCreatedAt(LocalDate.now().minusYears(2).atStartOfDay());
    }

    // ==================== CT006: Geração Automática de Lançamentos Recorrentes Mensais
    // ====================

    @Test
    @DisplayName("CT006 - Deve processar lançamento recorrente mensal e gerar próximo lançamento")
    void shouldProcessMonthlyRecurrencyAndGenerateNextTransaction() {
        // Arrange
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(originalMonthlyTransaction.getId()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, times(1)).findLastChildTransaction(originalMonthlyTransaction.getId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Deve gerar lançamento filho com mesmos dados do original")
    void shouldGenerateChildTransactionWithSameDataAsOriginal() {
        // Arrange
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Validar dados do filho
            assertEquals(originalMonthlyTransaction.getType(), child.getType(), 
                "Tipo deve ser igual ao original");
            assertEquals(originalMonthlyTransaction.getAmount(), child.getAmount(), 
                "Valor deve ser igual ao original");
            assertEquals(originalMonthlyTransaction.getDescription(), child.getDescription(), 
                "Descrição deve ser igual ao original");
            assertEquals(originalMonthlyTransaction.getRecurrency(), child.getRecurrency(), 
                "Recorrência deve ser igual ao original");
            assertEquals(originalMonthlyTransaction.getCategory(), child.getCategory(), 
                "Categoria deve ser igual ao original");
            assertEquals(originalMonthlyTransaction.getId(), child.getParentTransactionId(),
                "Parent ID deve apontar para o original");
            assertFalse(child.getRecurrencyActive(),
                "Filho não deve ter recorrência ativa");
            assertNull(child.getRecurrencyEndDate(),
                "Filho não deve ter data de término");
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Deve calcular próxima data corretamente para recorrência mensal")
    void shouldCalculateNextDateCorrectlyForMonthlyRecurrency() {
        // Arrange
        LocalDate originalDate = LocalDate.of(2025, 10, 11);
        LocalDate expectedNextDate = LocalDate.of(2025, 11, 11);
        
        originalMonthlyTransaction.setCreatedAt(originalDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Validar data calculada
            assertEquals(expectedNextDate, child.getCreatedAt().toLocalDate(), 
                "Data deve ser incrementada em 1 mês");
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Deve processar múltiplos lançamentos recorrentes ativos")
    void shouldProcessMultipleActiveRecurrentTransactions() {
        // Arrange
        List<Transaction> activeRecurrencies = Arrays.asList(
            originalMonthlyTransaction,
            originalDailyTransaction,
            originalWeeklyTransaction
        );
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(System.currentTimeMillis());
            return saved;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Não deve gerar lançamento quando próxima data ainda não chegou")
    void shouldNotGenerateTransactionWhenNextDateHasNotArrived() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusMonths(1);
        originalMonthlyTransaction.setCreatedAt(futureDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Deve usar data do último filho como base para calcular próxima data")
    void shouldUseLastChildDateAsBaseForNextDate() {
        // Arrange
        LocalDate lastChildDate = LocalDate.of(2025, 11, 11);
        LocalDate expectedNextDate = LocalDate.of(2025, 12, 11);
        
        Transaction lastChild = new Transaction();
        lastChild.setId(50L);
        lastChild.setCreatedAt(lastChildDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(originalMonthlyTransaction.getId()))
                .thenReturn(Optional.of(lastChild));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Validar que usou data do último filho
            assertEquals(expectedNextDate, child.getCreatedAt().toLocalDate(), 
                "Deve calcular a partir do último filho, não do original");
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).findLastChildTransaction(originalMonthlyTransaction.getId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("CT006 - Deve respeitar data de término da recorrência")
    void shouldRespectRecurrencyEndDate() {
        // Arrange
        LocalDate endDate = LocalDate.of(2025, 10, 15);
        originalMonthlyTransaction.setRecurrencyEndDate(endDate);
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalMonthlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    // ==================== Testes de Recorrência Diária ====================

    @Test
    @DisplayName("Deve calcular próxima data corretamente para recorrência diária")
    void shouldCalculateNextDateCorrectlyForDailyRecurrency() {
        // Arrange
        LocalDate baseDate = LocalDate.now().minusDays(2);
        originalDailyTransaction.setCreatedAt(baseDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalDailyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Data deve ser incrementada em 1 dia
            LocalDate expectedDate = baseDate.plusDays(1);
            assertEquals(expectedDate, child.getCreatedAt().toLocalDate());
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==================== Testes de Recorrência Semanal ====================

    @Test
    @DisplayName("Deve calcular próxima data corretamente para recorrência semanal")
    void shouldCalculateNextDateCorrectlyForWeeklyRecurrency() {
        // Arrange
        LocalDate baseDate = LocalDate.now().minusWeeks(2);
        originalWeeklyTransaction.setCreatedAt(baseDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalWeeklyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Data deve ser incrementada em 1 semana
            LocalDate expectedDate = baseDate.plusWeeks(1);
            assertEquals(expectedDate, child.getCreatedAt().toLocalDate());
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==================== Testes de Recorrência Trimestral ====================

    @Test
    @DisplayName("Deve calcular próxima data corretamente para recorrência trimestral")
    void shouldCalculateNextDateCorrectlyForQuarterlyRecurrency() {
        // Arrange
        LocalDate baseDate = LocalDate.now().minusMonths(4);
        originalQuarterlyTransaction.setCreatedAt(baseDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalQuarterlyTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Data deve ser incrementada em 3 meses
            LocalDate expectedDate = baseDate.plusMonths(3);
            assertEquals(expectedDate, child.getCreatedAt().toLocalDate());
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==================== Testes de Recorrência Anual ====================

    @Test
    @DisplayName("Deve calcular próxima data corretamente para recorrência anual")
    void shouldCalculateNextDateCorrectlyForAnnualRecurrency() {
        // Arrange
        LocalDate baseDate = LocalDate.now().minusYears(2);
        originalAnnualTransaction.setCreatedAt(baseDate.atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(originalAnnualTransaction);
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(anyLong()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction child = invocation.getArgument(0);
            
            // Assert: Data deve ser incrementada em 1 ano
            LocalDate expectedDate = baseDate.plusYears(1);
            assertEquals(expectedDate, child.getCreatedAt().toLocalDate());
            
            return child;
        });

        // Act
        recurrencyService.processAllActiveRecurrencies();

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==================== Testes de Ativação/Desativação ====================

    @Test
    @DisplayName("Deve desativar recorrência de lançamento original com sucesso")
    void shouldDeactivateRecurrencySuccessfully() {
        // Arrange
        Long transactionId = 1L;
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(originalMonthlyTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(originalMonthlyTransaction);

        // Act
        assertDoesNotThrow(() -> recurrencyService.deactivateRecurrency(transactionId));

        // Assert
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(originalMonthlyTransaction);
    }

    @Test
    @DisplayName("Deve ativar recorrência de lançamento original com sucesso")
    void shouldActivateRecurrencySuccessfully() {
        // Arrange
        Long transactionId = 1L;
        originalMonthlyTransaction.setRecurrencyActive(false);
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(originalMonthlyTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(originalMonthlyTransaction);

        // Act
        assertDoesNotThrow(() -> recurrencyService.activateRecurrency(transactionId));

        // Assert
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(originalMonthlyTransaction);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar desativar recorrência de lançamento inexistente")
    void shouldThrowExceptionWhenDeactivatingNonExistentTransaction() {
        // Arrange
        Long nonExistentId = 999L;
        
        when(transactionRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> recurrencyService.deactivateRecurrency(nonExistentId));

        assertTrue(exception.getMessage().contains("não encontrado"));
        verify(transactionRepository, times(1)).findById(nonExistentId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar ativar recorrência de lançamento filho")
    void shouldThrowExceptionWhenActivatingChildTransaction() {
        // Arrange
        Long childId = 100L;
        Transaction childTransaction = new Transaction();
        childTransaction.setId(childId);
        childTransaction.setParentTransactionId(1L); // É um filho
        childTransaction.setRecurrency(RecurrencyType.MONTHLY);
        
        when(transactionRepository.findById(childId))
                .thenReturn(Optional.of(childTransaction));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> recurrencyService.activateRecurrency(childId));

        assertTrue(exception.getMessage().contains("lançamentos originais"));
        verify(transactionRepository, times(1)).findById(childId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar ativar recorrência de lançamento único")
    void shouldThrowExceptionWhenActivatingUniqueTransaction() {
        // Arrange
        Long transactionId = 1L;
        Transaction uniqueTransaction = new Transaction();
        uniqueTransaction.setId(transactionId);
        uniqueTransaction.setRecurrency(RecurrencyType.UNIQUE);
        uniqueTransaction.setParentTransactionId(null);
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(uniqueTransaction));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> recurrencyService.activateRecurrency(transactionId));

        assertTrue(exception.getMessage().contains("UNIQUE não possui recorrência"));
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, never()).save(any());
    }

    // ==================== Testes de Data de Término ====================

    @Test
    @DisplayName("Deve definir data de término da recorrência com sucesso")
    void shouldSetRecurrencyEndDateSuccessfully() {
        // Arrange
        Long transactionId = 1L;
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(originalMonthlyTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(originalMonthlyTransaction);

        // Act
        assertDoesNotThrow(() -> recurrencyService.setRecurrencyEndDate(transactionId, endDate));

        // Assert
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(originalMonthlyTransaction);
    }

    @Test
    @DisplayName("Deve lançar exceção ao definir data de término anterior à data do lançamento")
    void shouldThrowExceptionWhenEndDateIsBeforeTransactionDate() {
        // Arrange
        Long transactionId = 1L;
        LocalDate endDate = LocalDate.of(2025, 9, 1); // Anterior a 2025-10-11
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(originalMonthlyTransaction));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> recurrencyService.setRecurrencyEndDate(transactionId, endDate));

        assertTrue(exception.getMessage().contains("não pode ser anterior"));
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir remover data de término definindo como null")
    void shouldAllowRemovingEndDateBySettingNull() {
        // Arrange
        Long transactionId = 1L;
        originalMonthlyTransaction.setRecurrencyEndDate(LocalDate.of(2025, 12, 31));
        
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(originalMonthlyTransaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(originalMonthlyTransaction);

        // Act
        assertDoesNotThrow(() -> recurrencyService.setRecurrencyEndDate(transactionId, null));

        // Assert
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(originalMonthlyTransaction);
    }

    // ==================== Testes de Consulta ====================

    @Test
    @DisplayName("Deve buscar lançamentos filhos de um lançamento original")
    void shouldFindChildTransactionsOfOriginal() {
        // Arrange
        Long parentId = 1L;
        
        Transaction child1 = new Transaction();
        child1.setId(10L);
        child1.setParentTransactionId(parentId);
        
        Transaction child2 = new Transaction();
        child2.setId(11L);
        child2.setParentTransactionId(parentId);
        
        List<Transaction> children = Arrays.asList(child1, child2);
        
        when(transactionRepository.findAllChildTransactions(parentId))
                .thenReturn(children);

        // Act
        List<Transaction> result = recurrencyService.findChildTransactions(parentId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository, times(1)).findAllChildTransactions(parentId);
    }

    @Test
    @DisplayName("Deve contar lançamentos filhos gerados")
    void shouldCountChildTransactions() {
        // Arrange
        Long parentId = 1L;
        Long expectedCount = 5L;
        
        when(transactionRepository.countChildTransactions(parentId))
                .thenReturn(expectedCount);

        // Act
        Long result = recurrencyService.countChildTransactions(parentId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
        verify(transactionRepository, times(1)).countChildTransactions(parentId);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver filhos")
    void shouldReturnEmptyListWhenNoChildren() {
        // Arrange
        Long parentId = 1L;
        
        when(transactionRepository.findAllChildTransactions(parentId))
                .thenReturn(Arrays.asList());

        // Act
        List<Transaction> result = recurrencyService.findChildTransactions(parentId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionRepository, times(1)).findAllChildTransactions(parentId);
    }

    @Test
    @DisplayName("Deve retornar zero quando não houver filhos para contar")
    void shouldReturnZeroWhenNoChildrenToCount() {
        // Arrange
        Long parentId = 1L;
        
        when(transactionRepository.countChildTransactions(parentId))
                .thenReturn(0L);

        // Act
        Long result = recurrencyService.countChildTransactions(parentId);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result);
        verify(transactionRepository, times(1)).countChildTransactions(parentId);
    }

    // ==================== Testes de Tratamento de Erros ====================

    @Test
    @DisplayName("Deve continuar processamento mesmo se um lançamento falhar")
    void shouldContinueProcessingEvenIfOneTransactionFails() {
        // Arrange
        Transaction problematicTransaction = new Transaction();
        problematicTransaction.setId(99L);
        problematicTransaction.setRecurrency(RecurrencyType.MONTHLY);
        problematicTransaction.setRecurrencyActive(true);
        problematicTransaction.setParentTransactionId(null);
        problematicTransaction.setCreatedAt(LocalDate.now().minusMonths(1).atStartOfDay());
        
        List<Transaction> activeRecurrencies = Arrays.asList(
            problematicTransaction,
            originalMonthlyTransaction
        );
        
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(activeRecurrencies);
        when(transactionRepository.findLastChildTransaction(problematicTransaction.getId()))
                .thenThrow(new RuntimeException("Erro simulado"));
        when(transactionRepository.findLastChildTransaction(originalMonthlyTransaction.getId()))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        assertDoesNotThrow(() -> recurrencyService.processAllActiveRecurrencies());

        // Assert: Deve ter tentado processar ambos
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, times(1)).findLastChildTransaction(problematicTransaction.getId());
        verify(transactionRepository, times(1)).findLastChildTransaction(originalMonthlyTransaction.getId());
        // Deve ter salvo apenas o que não falhou
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve processar corretamente quando não houver recorrências ativas")
    void shouldProcessCorrectlyWhenNoActiveRecurrencies() {
        // Arrange
        when(transactionRepository.findOriginalActiveRecurrentTransactions())
                .thenReturn(Arrays.asList());

        // Act
        assertDoesNotThrow(() -> recurrencyService.processAllActiveRecurrencies());

        // Assert
        verify(transactionRepository, times(1)).findOriginalActiveRecurrentTransactions();
        verify(transactionRepository, never()).save(any());
    }
}
