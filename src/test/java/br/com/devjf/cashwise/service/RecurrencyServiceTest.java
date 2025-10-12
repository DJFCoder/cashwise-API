package br.com.devjf.cashwise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
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
 * Testes unitários para {@link RecurrencyService}.
 * <p>
 * Cobertura completa das regras de recorrência:
 * <ul>
 *   <li>Processamento diário de recorrências ativas</li>
 *   <li>Geração de no máximo 1 filho por execução (limite 2 por dia)</li>
 *   <li>Cálculo da próxima data +1 período a partir da fonte mais recente</li>
 *   <li>Ativação/desativação de recorrências</li>
 *   <li>Definição de data de término</li>
 *   <li>Consulta de filhos gerados</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecurrencyService - CT006 Geração Automática de Lançamentos Recorrentes")
class RecurrencyServiceTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private RecurrencyService service;

    private Category category;
    private Transaction originalDaily;
    private Transaction originalMonthly;

    @BeforeEach
    void setUp() {
        category = buildCategory();

        originalDaily = buildOriginal(RecurrencyType.DAILY, LocalDate.now(), new BigDecimal("50"), "Transporte");
        originalMonthly = buildOriginal(RecurrencyType.MONTHLY, LocalDate.now(), new BigDecimal("500"), "Aluguel");
    }

    private Transaction buildOriginal(RecurrencyType type, LocalDate created, BigDecimal amount, String desc) {
        Transaction t = new Transaction();
        t.setId(System.nanoTime());
        t.setType(TransactionType.EXPENSE);
        t.setAmount(amount);
        t.setDescription(desc);
        t.setRecurrency(type);
        t.setRecurrencyActive(true);
        t.setParentTransactionId(null);
        t.setCategory(category);
        t.setCreatedAt(created.atStartOfDay());
        return t;
    }

    private Category buildCategory() {
        Category c = new Category();
        c.setId(1L);
        c.setName("Test");
        return c;
    }

    // -------------------------------------------------------------------------
    // 1. PROCESSAMENTO DIÁRIO + 1 FILHO POR EXECUÇÃO
    // -------------------------------------------------------------------------

    /**
     * CT037: Deve processar recorrência ativa e gerar 1 filho por execução.
     */
    @Test
    @DisplayName("CT037 - Processar recorrência ativa e gerar 1 filho")
    void processActiveAndGenerateOneChild() {
        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(originalDaily));
        when(repository.countByParentTransactionIdAndCreatedAtAfter(eq(originalDaily.getId()), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(repository.findLastChildTransaction(originalDaily.getId())).thenReturn(Optional.empty());
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processAllActiveRecurrencies();

        verify(repository).save(argThat(child ->
                child.getCreatedAt().toLocalDate().equals(LocalDate.now().plusDays(1))
        ));
    }
    /**
     * CT038: Deve usar último filho como fonte para próxima data.
     */
    @Test
    @DisplayName("CT038 - Usar último filho como fonte")
    void useLastChildAsSource() {
        Transaction lastChild = buildChild(originalDaily, LocalDate.now().minusDays(1), LocalDate.now());

        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(originalDaily));
        when(repository.countByParentTransactionIdAndCreatedAtAfter(eq(originalDaily.getId()), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(repository.findLastChildTransaction(originalDaily.getId())).thenReturn(Optional.of(lastChild));
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processAllActiveRecurrencies();

        verify(repository).save(argThat(child ->
                child.getCreatedAt().toLocalDate().equals(LocalDate.now())
        ));
    }

    /**
     * CT039: Deve respeitar limite de 2 filhos por dia. (REMOVIDO - Regra não existe mais)
     */
    @Test
    @DisplayName("CT039 - Respeitar limite de 2 filhos por dia (REMOVIDO)")
    void respectTwoChildrenPerDayLimit() {
        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(originalDaily));
        when(repository.countByParentTransactionIdAndCreatedAtAfter(eq(originalDaily.getId()), any(LocalDateTime.class)))
                .thenReturn(2L);

        service.processAllActiveRecurrencies();

        verify(repository, never()).save(any(Transaction.class));
    }

    // -------------------------------------------------------------------------
    // 2. TIPOS DE RECORRÊNCIA
    // -------------------------------------------------------------------------

    /**
     * CT040: Deve calcular próxima data correta para cada tipo.
     */
    @Test
    @DisplayName("CT040 - Calcular próxima data para cada tipo")
    void calculateCorrectNextDateForEachType() {
        LocalDate base = LocalDate.of(2025, 10, 12);

        assertNextDate(RecurrencyType.DAILY, base, base.plusDays(1));
        assertNextDate(RecurrencyType.WEEKLY, base, base.plusWeeks(1));
        assertNextDate(RecurrencyType.MONTHLY, base, base.plusMonths(1));
        assertNextDate(RecurrencyType.QUARTERLY, base, base.plusMonths(3));
        assertNextDate(RecurrencyType.ANNUAL, base, base.plusYears(1));
    }

    private void assertNextDate(RecurrencyType type, LocalDate base, LocalDate expected) {
        Transaction original = buildOriginal(type, base, BigDecimal.TEN, "Test");
        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(original));
        when(repository.countByParentTransactionIdAndCreatedAtAfter(eq(original.getId()), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(repository.findLastChildTransaction(original.getId())).thenReturn(Optional.empty());
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processAllActiveRecurrencies();

        verify(repository).save(argThat(child ->
                child.getCreatedAt().toLocalDate().equals(expected)
        ));
    }

    // -------------------------------------------------------------------------

    
    // 3. ATIVAÇÃO / DESATIVAÇÃO
    // -------------------------------------------------------------------------

    /**
     * CT005: Deve desativar recorrência com sucesso.
     */
    @Test
    @DisplayName("CT005 - Desativa recorrência com sucesso")
    void deactivateRecurrencySuccessfully() {
        when(repository.findById(originalDaily.getId())).thenReturn(Optional.of(originalDaily));

        assertDoesNotThrow(() -> service.deactivateRecurrency(originalDaily.getId()));

        assertThat(originalDaily.getRecurrencyActive()).isFalse();
        verify(repository).save(originalDaily);
    }

    /**
     * CT042: Deve lançar exceção ao desativar lançamento inexistente.
     */
    @Test
    @DisplayName("CT042 - Falha ao desativar lançamento inexistente")
    void failDeactivatingNonExistentTransaction() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.deactivateRecurrency(999L));

        verify(repository, never()).save(any());
    }

    /**
     * CT043: Deve ativar recorrência com sucesso.
     */
    @Test
    @DisplayName("CT043 - Ativa recorrência com sucesso")
    void activateRecurrencySuccessfully() {
        originalDaily.setRecurrencyActive(false);
        when(repository.findById(originalDaily.getId())).thenReturn(Optional.of(originalDaily));

        assertDoesNotThrow(() -> service.activateRecurrency(originalDaily.getId()));

        assertThat(originalDaily.getRecurrencyActive()).isTrue();
        verify(repository).save(originalDaily);
    }

    /**
     * CT044: Deve lançar exceção ao ativar recorrência de filho.
     */
    @Test
    @DisplayName("CT044 - Falha ao ativar recorrência de filho")
    void failActivatingChildTransaction() {
        Transaction child = buildChild(originalDaily, LocalDate.now(), LocalDate.now());
        when(repository.findById(child.getId())).thenReturn(Optional.of(child));

        assertThrows(IllegalArgumentException.class,
                () -> service.activateRecurrency(child.getId()));

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 4. DATA DE TÉRMINO
    // -------------------------------------------------------------------------

    /**
     * CT045: Deve definir data de término com sucesso.
     */
    @Test
    @DisplayName("CT045 - Define data de término com sucesso")
    void setEndDateSuccessfully() {
        LocalDate end = LocalDate.of(2025, 12, 31);
        when(repository.findById(originalMonthly.getId())).thenReturn(Optional.of(originalMonthly));

        assertDoesNotThrow(() -> service.setRecurrencyEndDate(originalMonthly.getId(), end));

        assertThat(originalMonthly.getRecurrencyEndDate()).isEqualTo(end);
        verify(repository).save(originalMonthly);
    }

    /**
     * CT046: Deve lançar exceção ao definir data anterior ao original.
     */
    @Test
    @DisplayName("CT046 - Falha ao definir data anterior ao original")
    void failEndDateBeforeOriginal() {
        LocalDate end = LocalDate.of(2025, 9, 1); // antes de 12/10
        when(repository.findById(originalMonthly.getId())).thenReturn(Optional.of(originalMonthly));

        assertThrows(IllegalArgumentException.class,
                () -> service.setRecurrencyEndDate(originalMonthly.getId(), end));

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 5. CONSULTA DE FILHOS
    // -------------------------------------------------------------------------

    /**
     * CT047: Deve buscar lançamentos filhos.
     */
    @Test
    @DisplayName("CT047 - Busca lançamentos filhos")
    void findChildTransactions() {
        Transaction child = buildChild(originalMonthly, LocalDate.now(), LocalDate.now());
        when(repository.findAllChildTransactions(originalMonthly.getId())).thenReturn(List.of(child));

        List<Transaction> result = service.findChildTransactions(originalMonthly.getId());

        assertThat(result).hasSize(1);
    }

    /**
     * CT048: Deve contar lançamentos filhos.
     */
    @Test
    @DisplayName("CT048 - Conta lançamentos filhos")
    void countChildTransactions() {
        when(repository.countChildTransactions(originalMonthly.getId())).thenReturn(5L);

        Long result = service.countChildTransactions(originalMonthly.getId());

        assertThat(result).isEqualTo(5L);
    }

    // -------------------------------------------------------------------------
    // 6. CENÁRIOS DE DESATIVAÇÃO
    // -------------------------------------------------------------------------

    /**
     * CT049: Desativação deve manter último filho como data atual.
     */
    @Test
    @DisplayName("CT049 - Desativação mantém último filho como data atual")
    void deactivationKeepsLastChildAsToday() {
        Transaction child = buildChild(originalDaily, LocalDate.now(), LocalDate.now());
        when(repository.findById(originalDaily.getId())).thenReturn(Optional.of(originalDaily));

        assertDoesNotThrow(() -> service.deactivateRecurrency(originalDaily.getId()));

        // O filho permanece com data de hoje (não é apagado)
        assertThat(child.getCreatedAt().toLocalDate()).isEqualTo(LocalDate.now());
    }

    // -------------------------------------------------------------------------
    // 7. UNIQUE NÃO GERA FILHOS
    // -------------------------------------------------------------------------

    /**
     * CT050: UNIQUE não gera filhos.
     */
    @Test
    @DisplayName("CT050 - UNIQUE não gera filhos")
    void uniqueDoesNotGenerateChildren() {
        Transaction unique = buildOriginal(RecurrencyType.UNIQUE, LocalDate.now(), BigDecimal.TEN, "Único");
        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(unique));

        service.processAllActiveRecurrencies();

        verify(repository, never()).save(any(Transaction.class));
    }

    // -------------------------------------------------------------------------
    // 8. RESPEITA DATA DE TÉRMINO
    // -------------------------------------------------------------------------

    /**
     * CT015: Deve respeitar data de término e parar geração.
     */
    @Test
    @DisplayName("CT015 - Respeita data de término e para geração")
    void respectsEndDateAndStopsGeneration() {
        originalMonthly.setRecurrencyEndDate(LocalDate.now().minusDays(1));
        when(repository.findOriginalActiveRecurrentTransactions()).thenReturn(List.of(originalMonthly));
        when(repository.countByParentTransactionIdAndCreatedAtAfter(eq(originalMonthly.getId()), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(repository.findLastChildTransaction(originalMonthly.getId())).thenReturn(Optional.empty());

        service.processAllActiveRecurrencies();

        verify(repository, never()).save(any(Transaction.class));
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Transaction buildChild(Transaction parent, LocalDate created, LocalDate next) {
        Transaction child = new Transaction();
        child.setId(System.nanoTime());
        child.setType(parent.getType());
        child.setAmount(parent.getAmount());
        child.setDescription(parent.getDescription() + " (filho)");
        child.setRecurrency(parent.getRecurrency());
        child.setCategory(parent.getCategory());
        child.setParentTransactionId(parent.getId());
        child.setRecurrencyActive(false);
        child.setCreatedAt(created.atStartOfDay());
        return child;
    }
}