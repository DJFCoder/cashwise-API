package br.com.devjf.cashwise.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável pelo processamento e gerenciamento de lançamentos recorrentes.
 * <p>
 * Implementa as regras de negócio para geração automática de lançamentos filhos
 * a partir de lançamentos originais com recorrência ativa.
 * </p>
 * <p>
 * Principais funcionalidades:
 * <ul>
 *   <li>Processamento diário de recorrências ativas</li>
 *   <li>Geração de no máximo 1 lançamento filho por execução (limite de 2 por dia)</li>
 *   <li>Cálculo da próxima data sempre +1 período a partir da fonte mais recente</li>
 *   <li>Ativação/desativação de recorrências</li>
 *   <li>Definição de data de término para recorrências</li>
 *   <li>Consulta de lançamentos filhos gerados</li>
 * </ul>
 * </p>
 * <p>
 * Regras de recorrência:
 * <ul>
 *   <li><b>UNIQUE</b>: Não gera filhos</li>
 *   <li><b>DAILY</b>: Gera filhos com data +1 dia</li>
 *   <li><b>WEEKLY</b>: Gera filhos com data +1 semana</li>
 *   <li><b>MONTHLY</b>: Gera filhos com data +1 mês</li>
 *   <li><b>QUARTERLY</b>: Gera filhos com data +3 meses</li>
 *   <li><b>ANNUAL</b>: Gera filhos com data +1 ano</li>
 * </ul>
 * </p>
 * <p>
 * Comportamento de geração:
 * <ul>
 *   <li>No dia do cadastro do original: sistema cria 1 filho automaticamente</li>
 *   <li>Nos dias seguintes: job cria 1 filho por execução (máximo 2 por dia)</li>
 *   <li>Fonte de cálculo: original (primeira vez) ou último filho gerado</li>
 *   <li>Desativação: último filho gerado é sempre o da data atual</li>
 * </ul>
 * </p>
 *
 * @author devjf
 * @see Transaction
 * @see RecurrencyType
 * @see RecurrencyJob
 */
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
     * <p>
     * Executado diariamente pelo {@link RecurrencyJob}, este método:
     * </p>
     * <ul>
     *   <li>Seleciona apenas lançamentos originais (parentTransactionId = null)</li>
     *   <li>Filtra por recurrencyActive = true e recurrency ≠ UNIQUE</li>
     *   <li>Processa cada lançamento individualmente</li>
     *   <li>Captura e loga exceções sem interromper o processamento dos demais</li>
     * </ul>
     * <p>
     * Para cada lançamento, verifica se deve gerar novos filhos respeitando:
     * <ul>
     *   <li>Limite máximo de 2 filhos por dia</li>
     *   <li>Data de término da recorrência (se definida)</li>
     *   <li>Próxima data calculada (+1 período)</li>
     * </ul>
     * </p>
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
     * <p>
     * Aplica as regras de negócio para decidir se deve gerar um novo lançamento filho:
     * </p>
     * <ul>
     *   <li>Verifica se o lançamento pode gerar recorrência (original, ativo, ≠ UNIQUE)</li>
     *   <li>Conta quantos filhos já foram criados hoje (máximo 2)</li>
     *   <li>Identifica a fonte de cálculo (original ou último filho)</li>
     *   <li>Calcula a próxima data (+1 período)</li>
     *   <li>Verifica se ainda está dentro do período permitido</li>
     *   <li>Cria exatamente 1 filho por execução</li>
     * </ul>
     * <p>
     * O método utiliza o último lançamento gerado como fonte para calcular a próxima data,
     * garantindo que a sequência de datas seja sempre crescente e sem gaps.
     * </p>
     *
     * @param original lançamento original (parent) que será processado
     */
    private void processRecurrencyForTransaction(Transaction original) {
        if (!original.canGenerateRecurrency()) {
            log.debug("Lançamento ID {} não pode gerar recorrência", original.getId());
            return;
        }

        // Bloqueio: no máximo 2 filhos por dia
        long countToday = countChildrenCreatedToday(original.getId());
        if (countToday >= 2) {
            log.debug("Original {} já possui 2 filhos hoje", original.getId());
            return;
        }

        // Identifica a fonte (original ou último filho)
        Transaction source = getSourceForNextDate(original);
        LocalDate nextDate = calculateNextRecurrencyDate(source);

        if (!original.shouldContinueRecurrency(nextDate)) {
            log.debug("Próxima data {} ultrapassa o fim da recorrência", nextDate);
            return;
        }

        // Cria exatamente 1 filho por execução
        Transaction child = createChildTransaction(original, nextDate);
        transactionRepository.save(child);
        log.info("Novo lançamento recorrente gerado: ID {} para data {}", child.getId(), nextDate);
    }

    /**
     * Conta quantos lançamentos filhos foram criados hoje (a partir de 00:00).
     * <p>
     * Utilizado para garantir o limite máximo de 2 lançamentos filhos por dia,
     * evitando loops infinitos e excesso de geração.
     * </p>
     *
     * @param parentId ID do lançamento original
     * @return quantidade de filhos criados hoje
     */
    private long countChildrenCreatedToday(Long parentId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return transactionRepository.countByParentTransactionIdAndCreatedAtAfter(parentId, startOfDay);
    }

    /**
     * Define a fonte de cálculo da próxima data:
     * <ul>
     *   <li>Se NÃO há filhos → usa o original</li>
     *   <li>Se HÁ filhos → usa o último filho</li>
     * </ul>
     * <p>
     * Isso garante que o cálculo sempre seja feito a partir do ponto mais recente
     * da sequência de recorrência.
     * </p>
     *
     * @param original lançamento original
     * @return lançamento que será usado como base para o cálculo
     */
    private Transaction getSourceForNextDate(Transaction original) {
        return transactionRepository.findLastChildTransaction(original.getId())
                .orElse(original);
    }

    /**
     * Calcula a próxima data de recorrência baseada no lançamento de origem.
     * <p>
     * Adiciona 1 período completo (dia, semana, mês, trimestre ou ano) à data
     * do lançamento fonte (original ou último filho).
     * </p>
     *
     * @param source lançamento de referência (original ou último filho)
     * @return próxima data de recorrência
     */
    private LocalDate calculateNextRecurrencyDate(Transaction source) {
        LocalDateTime sourceDateTime = source.getCreatedAt();
        return switch (source.getRecurrency()) {
            case DAILY     -> sourceDateTime.plusDays(1).toLocalDate();
            case WEEKLY    -> sourceDateTime.plusWeeks(1).toLocalDate();
            case MONTHLY   -> sourceDateTime.plusMonths(1).toLocalDate();
            case QUARTERLY -> sourceDateTime.plusMonths(MONTHS_PER_QUARTER).toLocalDate();
            case ANNUAL    -> sourceDateTime.plusYears(1).toLocalDate();
            default        -> sourceDateTime.toLocalDate(); // UNIQUE nunca entra aqui
        };
    }

    /**
     * Cria um novo lançamento filho com os mesmos dados do lançamento de origem.
     * <p>
     * O filho herda todos os campos do original/último filho, exceto:
     * </p>
     * <ul>
     *   <li>parentTransactionId: aponta para o original (se já for filho, mantém o mesmo parent)</li>
     *   <li>recurrencyActive: false (filhos não geram netos)</li>
     *   <li>recurrencyEndDate: null (apenas o original controla o fim)</li>
     *   <li>createdAt: data calculada (sempre 00:00)</li>
     * </ul>
     *
     * @param original  lançamento original (para manter o parent correto)
     * @param nextDate  data que será atribuída ao novo lançamento
     * @return lançamento filho configurado
     */
    private Transaction createChildTransaction(Transaction original, LocalDate nextDate) {
        Transaction source = getSourceForNextDate(original);
        Transaction child = new Transaction();
        child.setType(source.getType());
        child.setAmount(source.getAmount());
        child.setDescription(source.getDescription());
        child.setRecurrency(source.getRecurrency());
        child.setCategory(source.getCategory());
        child.setParentTransactionId(original.getId()); // sempre aponta para o original
        child.setRecurrencyActive(false);
        child.setRecurrencyEndDate(null);
        child.setCreatedAt(nextDate.atStartOfDay());
        return child;
    }

    // ==================== MÉTODOS PÚBLICOS (CONTROLLER) ====================

    /**
     * Desativa a recorrência de um lançamento original.
     * <p>
     * Após desativar, o job automático não gerará mais lançamentos filhos
     * para este lançamento. Os filhos já gerados permanecem no sistema.
     * </p>
     *
     * @param transactionId ID do lançamento original
     * @throws EntityNotFoundException  se o lançamento não existir
     * @throws IllegalArgumentException se o lançamento não for original
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
     * <p>
     * Após ativar, o job automático voltará a gerar lançamentos filhos
     * conforme a periodicidade definida.
     * </p>
     *
     * @param transactionId ID do lançamento original
     * @throws EntityNotFoundException  se o lançamento não existir
     * @throws IllegalArgumentException se o lançamento não for original ou for UNIQUE
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
     * Define a data de término da recorrência de um lançamento original.
     * <p>
     * Após definir a data, o job automático não gerará lançamentos após essa data.
     * Para remover a data de término (recorrência infinita), envie null.
     * </p>
     *
     * @param transactionId ID do lançamento original
     * @param endDate       data de término (pode ser null)
     * @throws EntityNotFoundException  se o lançamento não existir
     * @throws IllegalArgumentException se o lançamento não for original ou se a data for anterior à data do lançamento
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
     * <p>
     * Útil para exibir o histórico completo de lançamentos gerados
     * automaticamente pela recorrência.
     * </p>
     *
     * @param parentId ID do lançamento original
     * @return lista de lançamentos filhos (ordenados por data de criação ascendente)
     */
    @Transactional(readOnly = true)
    public List<Transaction> findChildTransactions(Long parentId) {
        return transactionRepository.findAllChildTransactions(parentId);
    }

    /**
     * Conta quantos lançamentos filhos foram gerados a partir de um lançamento original.
     * <p>
     * Útil para estatísticas e monitoramento de recorrências.
     * </p>
     *
     * @param parentId ID do lançamento original
     * @return quantidade de lançamentos filhos gerados
     */
    @Transactional(readOnly = true)
    public Long countChildTransactions(Long parentId) {
        return transactionRepository.countChildTransactions(parentId);
    }

    // ==================== MÉTODOS AUXILIARES DE VALIDAÇÃO ====================

    /**
     * Busca um lançamento pelo ID ou lança exceção se não existir.
     *
     * @param id identificador do lançamento
     * @return lançamento encontrado
     * @throws EntityNotFoundException se o lançamento não existir
     */
    private Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lançamento com ID " + id + " não encontrado"));
    }

    /**
     * Valida se o lançamento é original (não é filho).
     *
     * @param transaction lançamento a ser validado
     * @throws IllegalArgumentException se o lançamento for filho
     */
    private void validateIsOriginalTransaction(Transaction transaction) {
        if (!transaction.isOriginalTransaction()) {
            throw new IllegalArgumentException(
                    "Operação permitida apenas para lançamentos originais. "
                    + "Este é um lançamento filho gerado automaticamente.");
        }
    }

    /**
     * Valida se o lançamento possui recorrência para ser ativada.
     *
     * @param transaction lançamento a ser validado
     * @throws IllegalArgumentException se o lançamento for do tipo UNIQUE
     */
    private void validateHasRecurrency(Transaction transaction) {
        if (RecurrencyType.UNIQUE.equals(transaction.getRecurrency())) {
            throw new IllegalArgumentException("Lançamento do tipo UNIQUE não possui recorrência para ativar");
        }
    }

    /**
     * Valida se a data de término é válida (não pode ser anterior à data do lançamento).
     *
     * @param transaction lançamento original
     * @param endDate     data de término a ser validada
     * @throws IllegalArgumentException se a data for anterior à data do lançamento
     */
    private void validateEndDateIsValid(Transaction transaction, LocalDate endDate) {
        LocalDate transactionDate = transaction.getCreatedAt().toLocalDate();
        if (endDate.isBefore(transactionDate)) {
            throw new IllegalArgumentException("Data de término não pode ser anterior à data do lançamento original");
        }
    }
}