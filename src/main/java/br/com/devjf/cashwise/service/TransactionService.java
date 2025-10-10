package br.com.devjf.cashwise.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class TransactionService {

    private static final int DEFAULT_RECURRENCY_OCCURRENCES = 12;
    private static final int MONTHS_PER_QUARTER = 3;

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    /**
     * Registra um novo lançamento no sistema.
     * <p>
     * Aplica regras de negócio específicas:
     * - Valida se a categoria existe (já validado pelo mapper)
     * - Define data atual se não fornecida
     * - Processa recorrência se diferente de UNICA
     * </p>
     *
     * @param transaction entidade Transaction a ser persistida
     * @return lançamento salvo com o ID gerado
     * @throws IllegalArgumentException se o lançamento for inválido
     */
    public Transaction registerTransaction(Transaction transaction) {
        validateTransaction(transaction);
        setCreatedAtIfNull(transaction);

        Transaction saved = transactionRepository.save(transaction);

        if (isRecurrent(transaction)) {
            processRecurrency(saved);
        }

        return saved;
    }

    /**
     * Exclui um lançamento pelo ID.
     *
     * @param id identificador do lançamento
     * @throws EntityNotFoundException se o lançamento não existir
     */
    public void deleteTransaction(Long id) {
        validateTransactionExists(id);
        transactionRepository.deleteById(id);
    }

    /**
     * Lista lançamentos com filtros opcionais.
     *
     * @param startDate data inicial (opcional)
     * @param endDate   data final (opcional)
     * @param pageable  configuração de paginação
     * @return página de lançamentos filtrados
     */
    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (hasPeriodFilter(startDate, endDate)) {
            return findByPeriod(startDate, endDate, pageable);
        }
        return transactionRepository.findAll(pageable);
    }

    /**
     * Busca lançamentos recorrentes que precisam gerar novos lançamentos.
     * Usado pelo RecurrencyJob.
     *
     * @return lista de lançamentos recorrentes para processamento
     */
    @Transactional(readOnly = true)
    public List<Transaction> findRecurrentTransactionsToProcess() {
        return transactionRepository.findRecurrentTransactionsToProcess();
    }

    /**
     * Lista lançamentos aplicando todos os filtros disponíveis.
     * Combina filtros de período, tipo e categoria conforme fornecidos.
     *
     * @param startDate  data inicial (opcional)
     * @param endDate    data final (opcional)
     * @param type       tipo de lançamento (opcional)
     * @param categoryId identificador da categoria (opcional)
     * @param pageable   configuração de paginação
     * @return página de lançamentos filtrados
     */
    @Transactional(readOnly = true)
    public Page<Transaction> listTransactionsWithFilters(
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            Pageable pageable) {

        Category category = findCategoryIfProvided(categoryId);

        if (hasAllFilters(startDate, endDate, type, category)) {
            return findByPeriodAndTypeAndCategory(startDate, endDate, type, category, pageable);
        }

        if (hasPeriodAndTypeFilter(startDate, endDate, type)) {
            return findByPeriodAndType(startDate, endDate, type, pageable);
        }

        if (hasPeriodAndCategoryFilter(startDate, endDate, category)) {
            return findByPeriodAndCategory(startDate, endDate, category, pageable);
        }

        if (hasPeriodFilter(startDate, endDate)) {
            return findByPeriod(startDate, endDate, pageable);
        }

        if (type != null) {
            return transactionRepository.findByType(type, pageable);
        }

        if (category != null) {
            return transactionRepository.findByCategory(category, pageable);
        }

        return transactionRepository.findAll(pageable);
    }

    /**
     * Valida um lançamento antes de persistir.
     *
     * @param transaction lançamento a ser validado
     * @throws IllegalArgumentException se o lançamento for inválida
     */
    private void validateTransaction(Transaction transaction) {
        validateTransactionNotNull(transaction);
        validateAmount(transaction);
        validateCategory(transaction);
        validateDescription(transaction);
    }

    private void validateTransactionNotNull(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Lançamento não pode ser nulo");
        }
    }

    private void validateAmount(Transaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Valor do lançamento deve ser positivo");
        }
    }

    private void validateCategory(Transaction transaction) {
        if (transaction.getCategory() == null) {
            throw new IllegalArgumentException("Categoria é obrigatória");
        }
    }

    private void validateDescription(Transaction transaction) {
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }
    }

    private void validateTransactionExists(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Lançamento com ID " + id + " não encontrada");
        }
    }

    private void setCreatedAtIfNull(Transaction transaction) {
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDate.now().atStartOfDay());
        }
    }

    private boolean isRecurrent(Transaction transaction) {
        return !RecurrencyType.UNIQUE.equals(transaction.getRecurrency());
    }

    private Category findCategoryIfProvided(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryService.findCategoryById(categoryId);
    }

    private boolean hasAllFilters(LocalDate startDate, LocalDate endDate, TransactionType type, Category category) {
        return hasPeriodFilter(startDate, endDate) && type != null && category != null;
    }

    private boolean hasPeriodAndTypeFilter(LocalDate startDate, LocalDate endDate, TransactionType type) {
        return hasPeriodFilter(startDate, endDate) && type != null;
    }

    private boolean hasPeriodAndCategoryFilter(LocalDate startDate, LocalDate endDate, Category category) {
        return hasPeriodFilter(startDate, endDate) && category != null;
    }

    private boolean hasPeriodFilter(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null;
    }

    private Page<Transaction> findByPeriod(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findByCreatedAtBetween(
                toStartOfDay(startDate),
                toEndOfDay(endDate),
                pageable);
    }

    private Page<Transaction> findByPeriodAndType(LocalDate startDate, LocalDate endDate, TransactionType type,
            Pageable pageable) {
        return transactionRepository.findByCreatedAtBetweenAndType(
                toStartOfDay(startDate),
                toEndOfDay(endDate),
                type,
                pageable);
    }

    private Page<Transaction> findByPeriodAndCategory(LocalDate startDate, LocalDate endDate, Category category,
            Pageable pageable) {
        return transactionRepository.findByCreatedAtBetweenAndCategory(
                toStartOfDay(startDate),
                toEndOfDay(endDate),
                category,
                pageable);
    }

    private Page<Transaction> findByPeriodAndTypeAndCategory(
            LocalDate startDate, LocalDate endDate, TransactionType type, Category category, Pageable pageable) {
        return transactionRepository.findByCreatedAtBetweenAndTypeAndCategory(
                toStartOfDay(startDate),
                toEndOfDay(endDate),
                type,
                category,
                pageable);
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * Processa a criação de lançamentos recorrentes.
     * Gera múltiplas ocorrências futuras baseadas no tipo de recorrência.
     *
     * @param originalTransaction lançamento original com recorrência
     */
    private void processRecurrency(Transaction originalTransaction) {
        for (int i = 1; i <= DEFAULT_RECURRENCY_OCCURRENCES; i++) {
            Transaction recurrentTransaction = createRecurrentTransaction(originalTransaction, i);
            transactionRepository.save(recurrentTransaction);
        }
    }

    /**
     * Cria um lançamento recorrente baseado no original.
     *
     * @param original         lançamento original
     * @param occurrenceNumber número da ocorrência
     * @return novo lançamento recorrente
     */
    private Transaction createRecurrentTransaction(Transaction original, int occurrenceNumber) {
        Transaction recurrent = new Transaction();
        recurrent.setType(original.getType());
        recurrent.setAmount(original.getAmount());
        recurrent.setDescription(buildRecurrentDescription(original, occurrenceNumber));
        recurrent.setRecurrency(original.getRecurrency());
        recurrent.setCategory(original.getCategory());
        recurrent.setCreatedAt(calculateNextDate(original.getCreatedAt(), original.getRecurrency(), occurrenceNumber));
        return recurrent;
    }

    private String buildRecurrentDescription(Transaction original, int occurrenceNumber) {
        return original.getDescription() + " (Recorrência " + occurrenceNumber + ")";
    }

    /**
     * Calcula a próxima data de ocorrência baseada no tipo de recorrência.
     *
     * @param originalDate data original
     * @param recurrency   tipo de recorrência
     * @param occurrence   número da ocorrência
     * @return data calculada da próxima ocorrência
     */
    private LocalDateTime calculateNextDate(LocalDateTime originalDate, RecurrencyType recurrency, int occurrence) {
        return switch (recurrency) {
            case DAILY -> originalDate.plusDays(occurrence);
            case WEEKLY -> originalDate.plusWeeks(occurrence);
            case MONTHLY -> originalDate.plusMonths(occurrence);
            case QUARTERLY -> originalDate.plusMonths((long) occurrence * MONTHS_PER_QUARTER);
            case ANNUAL -> originalDate.plusYears(occurrence);
            default -> originalDate;
        };
    }
}