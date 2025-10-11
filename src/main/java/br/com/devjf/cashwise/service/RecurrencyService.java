package br.com.devjf.cashwise.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class RecurrencyService {

    private static final int MONTHS_PER_QUARTER = 3;

    private final TransactionRepository transactionRepository;

    public RecurrencyService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Processa todos os lançamentos originais com recorrência ativa.
     * Gera novos lançamentos filhos baseado na próxima data de recorrência.
     * 
     * Chamado pelo RecurrencyJob diariamente.
     */
    public void processAllActiveRecurrencies() {
        List<Transaction> activeRecurrencies = transactionRepository.findOriginalActiveRecurrentTransactions();

        log.info("Processando {} lançamentos com recorrência ativa", activeRecurrencies.size());

        for (Transaction original : activeRecurrencies) {
            try {
                processRecurrencyForTransaction(original);
            } catch (Exception e) {
                log.error("Erro ao processar recorrência do lançamento ID {}: {}", 
                    original.getId(), e.getMessage(), e);
            }
        }

        log.info("Processamento de recorrências concluído");
    }

    /**
     * Processa a recorrência de um lançamento original específico.
     * Verifica se deve gerar novo lançamento baseado na última data gerada.
     * 
     * @param original lançamento original (parent)
     */
    private void processRecurrencyForTransaction(Transaction original) {
        if (!original.canGenerateRecurrency()) {
            log.debug("Lançamento ID {} não pode gerar recorrência", original.getId());
            return;
        }

        LocalDate nextDate = calculateNextRecurrencyDate(original);

        if (shouldGenerateNewTransaction(original, nextDate)) {
            Transaction child = createChildTransaction(original, nextDate);
            transactionRepository.save(child);
            log.info("Novo lançamento recorrente gerado: ID {} para data {}", child.getId(), nextDate);
        }
    }

    /**
     * Calcula a próxima data de recorrência baseado no último lançamento gerado.
     * Se não houver filho, usa a data do lançamento original.
     * 
     * @param original lançamento original
     * @return próxima data de recorrência
     */
    private LocalDate calculateNextRecurrencyDate(Transaction original) {
        Optional<Transaction> lastChild = transactionRepository.findLastChildTransaction(original.getId());

        LocalDateTime baseDate = lastChild
            .map(Transaction::getCreatedAt)
            .orElse(original.getCreatedAt());

        return calculateNextDate(baseDate, original.getRecurrency()).toLocalDate();
    }

    /**
     * Verifica se deve gerar um novo lançamento recorrente.
     * 
     * Critérios:
     * - A próxima data já passou ou é hoje
     * - Não ultrapassou a data de término (se definida)
     * 
     * @param original lançamento original
     * @param nextDate próxima data calculada
     * @return true se deve gerar, false caso contrário
     */
    private boolean shouldGenerateNewTransaction(Transaction original, LocalDate nextDate) {
        LocalDate today = LocalDate.now();

        boolean dateHasPassed = !nextDate.isAfter(today);
        boolean withinEndDate = original.shouldContinueRecurrency(nextDate);

        return dateHasPassed && withinEndDate;
    }

    /**
     * Cria um novo lançamento filho baseado no lançamento original.
     * 
     * @param original lançamento original (parent)
     * @param nextDate data do novo lançamento
     * @return novo lançamento filho configurado
     */
    private Transaction createChildTransaction(Transaction original, LocalDate nextDate) {
        Transaction child = new Transaction();
        child.setType(original.getType());
        child.setAmount(original.getAmount());
        child.setDescription(original.getDescription());
        child.setRecurrency(original.getRecurrency());
        child.setCategory(original.getCategory());
        child.setParentTransactionId(original.getId());
        child.setRecurrencyActive(false);
        child.setRecurrencyEndDate(null);
        child.setCreatedAt(nextDate.atStartOfDay());
        return child;
    }

    /**
     * Calcula a próxima data de ocorrência baseada no tipo de recorrência.
     * 
     * @param currentDate data atual/base
     * @param recurrency tipo de recorrência
     * @return próxima data calculada
     */
    private LocalDateTime calculateNextDate(LocalDateTime currentDate, RecurrencyType recurrency) {
        return switch (recurrency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(MONTHS_PER_QUARTER);
            case ANNUAL -> currentDate.plusYears(1);
            default -> currentDate;
        };
    }

    /**
     * Desativa a recorrência de um lançamento original.
     * Não afeta lançamentos filhos já gerados.
     * 
     * @param transactionId ID do lançamento original
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se não for lançamento original
     */
    public void deactivateRecurrency(Long transactionId) {
        Transaction transaction = findTransactionById(transactionId);
        validateIsOriginalTransaction(transaction);

        transaction.setRecurrencyActive(false);
        transactionRepository.save(transaction);

        log.info("Recorrência desativada para o lançamento ID {}", transactionId);
    }

    /**
     * Ativa a recorrência de um lançamento original.
     * 
     * @param transactionId ID do lançamento original
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se não for lançamento original
     */
    public void activateRecurrency(Long transactionId) {
        Transaction transaction = findTransactionById(transactionId);
        validateIsOriginalTransaction(transaction);
        validateHasRecurrency(transaction);

        transaction.setRecurrencyActive(true);
        transactionRepository.save(transaction);

        log.info("Recorrência ativada para o lançamento ID {}", transactionId);
    }

    /**
     * Define a data de término da recorrência para um lançamento original.
     * 
     * @param transactionId ID do lançamento original
     * @param endDate data de término (pode ser null para remover)
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se não for lançamento original ou data inválida
     */
    public void setRecurrencyEndDate(Long transactionId, LocalDate endDate) {
        Transaction transaction = findTransactionById(transactionId);
        validateIsOriginalTransaction(transaction);

        if (endDate != null) {
            validateEndDateIsValid(transaction, endDate);
        }

        transaction.setRecurrencyEndDate(endDate);
        transactionRepository.save(transaction);

        log.info("Data de término definida como {} para o lançamento ID {}", endDate, transactionId);
    }

    /**
     * Busca todos os lançamentos filhos de um lançamento original.
     * Útil para controllers exibirem o histórico completo.
     * 
     * @param parentId ID do lançamento original
     * @return lista de lançamentos filhos
     */
    @Transactional(readOnly = true)
    public List<Transaction> findChildTransactions(Long parentId) {
        return transactionRepository.findAllChildTransactions(parentId);
    }

    /**
     * Conta quantos lançamentos filhos foram gerados.
     * 
     * @param parentId ID do lançamento original
     * @return quantidade de filhos
     */
    @Transactional(readOnly = true)
    public Long countChildTransactions(Long parentId) {
        return transactionRepository.countChildTransactions(parentId);
    }

    // ========== MÉTODOS AUXILIARES DE VALIDAÇÃO ==========

    private Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "Lançamento com ID " + id + " não encontrado"));
    }

    private void validateIsOriginalTransaction(Transaction transaction) {
        if (!transaction.isOriginalTransaction()) {
            throw new IllegalArgumentException(
                "Operação permitida apenas para lançamentos originais. " +
                "Este é um lançamento filho gerado automaticamente.");
        }
    }

    private void validateHasRecurrency(Transaction transaction) {
        if (RecurrencyType.UNIQUE.equals(transaction.getRecurrency())) {
            throw new IllegalArgumentException(
                "Lançamento do tipo UNIQUE não possui recorrência para ativar");
        }
    }

    private void validateEndDateIsValid(Transaction transaction, LocalDate endDate) {
        LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();

        if (endDate.isBefore(transactionDate)) {
            throw new IllegalArgumentException(
                "Data de término não pode ser anterior à data do lançamento original");
        }
    }
}
