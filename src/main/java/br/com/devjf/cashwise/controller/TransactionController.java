package br.com.devjf.cashwise.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.devjf.cashwise.domain.dto.transaction.PageResponse;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionRequest;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionResponse;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.domain.mapper.TransactionMapper;
import br.com.devjf.cashwise.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para gerenciamento de lançamentos financeiros.
 * <p>
 * Fornece endpoints para operações CRUD e consultas com filtros de lançamentos.
 * Suporta paginação e ordenação para otimizar a performance em grandes volumes
 * de dados.
 * </p>
 * <p>
 * Endpoints disponíveis: - POST /api/lancamento - Cadastrar novo lançamento -
 * GET /api/lancamento/listar - Listar lançamentos com filtros e paginação - GET
 * /api/lancamento/{id} - Buscar lançamento por ID - DELETE /api/lancamento/{id}
 * - Excluir lançamento
 * </p>
 * <p>
 * Filtros suportados: - startDate: Data inicial do período - endDate: Data
 * final do período - type: Tipo do lançamento (REVENUE ou EXPENSE) -
 * categoryId: ID da categoria
 * </p>
 *
 * @author devjf
 */
@RestController
@RequestMapping("/api/lancamento")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    /**
     * Cadastra um novo lançamento financeiro.
     * <p>
     * Endpoint: POST /api/lancamento
     * </p>
     * <p>
     * Validações aplicadas: - Tipo deve ser "Receita" ou "Despesa" - Categoria
     * deve existir - Valor deve ser positivo - Recorrência deve ser válida
     * (UNIQUE, DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUAL) - Descrição é
     * opcional, mas se fornecida deve ter no máximo 255 caracteres
     * </p>
     *
     * @param request DTO com os dados do lançamento a ser criado
     * @return ResponseEntity com status 201 (Created) e o lançamento criado
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> registerTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("Recebida requisição para cadastrar lançamento: tipo={}, valor={}, categoria={}",
                request.type(), request.amount(), request.categoryId());

        Transaction transaction = transactionMapper.toEntity(request);
        Transaction savedTransaction = transactionService.registerTransaction(transaction);
        TransactionResponse response = transactionMapper.toResponse(savedTransaction);

        log.info("Lançamento cadastrado com sucesso - ID: {}", savedTransaction.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista lançamentos com filtros opcionais e paginação.
     * <p>
     * Endpoint: GET /api/lancamento/listar
     * </p>
     * <p>
     * Parâmetros de consulta: - startDate: Data inicial (formato: yyyy-MM-dd) -
     * endDate: Data final (formato: yyyy-MM-dd) - type: Tipo do lançamento
     * (REVENUE ou EXPENSE) - categoryId: ID da categoria - page: Número da
     * página (padrão: 0) - size: Tamanho da página (padrão: 20) - sort: Campo
     * de ordenação (padrão: createdAt,desc)
     * </p>
     * <p>
     * Exemplos de uso: 
     * - GET /api/lancamento/listar?startDate=2025-01-01&endDate=2025-01-31
     * - GET /api/lancamento/listar?type=EXPENSE&categoryId=1 
     * - GET /api/lancamento/listar?page=0&size=10&sort=amount,desc
     * </p>
     *
     * @param startDate data inicial do período (opcional)
     * @param endDate data final do período (opcional)
     * @param type tipo do lançamento (opcional)
     * @param categoryId ID da categoria (opcional)
     * @param page número da página (padrão: 0)
     * @param size tamanho da página (padrão: 20)
     * @param sortBy campo de ordenação (padrão: createdAt)
     * @param sortDirection direção da ordenação (padrão: desc)
     * @return ResponseEntity com status 200 (OK) e página de lançamentos
     */
    @GetMapping("/listar")
    public ResponseEntity<PageResponse<TransactionResponse>> listTransactions(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info(
                "Recebida requisição para listar lançamentos - startDate={}, endDate={}, type={}, categoryId={}, page={}, size={}",
                startDate, endDate, type, categoryId, page, size);

        // Cria objeto de paginação e ordenação
        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Aplica filtros conforme parâmetros fornecidos
        Page<Transaction> transactions = findTransactionsByFilters(startDate, endDate, type, categoryId, pageable);

        // Converte entidades para DTOs
        Page<TransactionResponse> response = convertToResponsePage(transactions);

        log.info("Retornando {} lançamento(s) de um total de {}",
                response.getNumberOfElements(), response.getTotalElements());
        return ResponseEntity.ok(PageResponse.from(response));
    }

    /**
     * Busca um lançamento por ID.
     * <p>
     * Endpoint: GET /api/lancamento/{id}
     * </p>
     * <p>
     * Nota: Este endpoint não está explicitamente no plano de testes porém
     * funciona normalmente.
     * </p>
     *
     * @param id identificador único do lançamento
     * @return ResponseEntity com status 200 (OK) e o lançamento encontrado
     * @throws EntityNotFoundException se o lançamento não existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findTransactionById(@PathVariable Long id) {
        log.info("Recebida requisição para buscar lançamento com ID: {}", id);

        // Busca através do serviço (implementação simplificada)
        // Em uma implementação completa, seria necessário adicionar este método no
        // TransactionService
        Transaction transaction = searchTransactionById(id);
        TransactionResponse response = transactionMapper.toResponse(transaction);

        log.info("Lançamento encontrado: tipo={}, valor={}", transaction.getType(), transaction.getAmount());
        return ResponseEntity.ok(response);
    }

    /**
     * Exclui um lançamento.
     * <p>
     * Endpoint: DELETE /api/lancamento/{id}
     * </p>
     * <p>
     * Validações aplicadas: - Lançamento deve existir
     * </p>
     * <p>
     * Nota: Se o lançamento for um lançamento original com recorrência ativa,
     * apenas o lançamento original será excluído. Os lançamentos filhos gerados
     * automaticamente permanecerão no sistema (conforme regra de negócio).
     * </p>
     *
     * @param id identificador único do lançamento a ser excluído
     * @return ResponseEntity com status 204 (No Content)
     * @throws EntityNotFoundException se o lançamento não existir
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        log.info("Recebida requisição para excluir lançamento ID: {}", id);

        transactionService.deleteTransaction(id);

        log.info("Lançamento excluído com sucesso - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort sortCriteria = Sort.by(sortBy);

        if (sortDirection.equalsIgnoreCase("asc")) {
            return sortCriteria.ascending();
        }

        return sortCriteria.descending();
    }

    private Page<Transaction> findTransactionsByFilters(LocalDate startDate, LocalDate endDate,
            String type, Long categoryId, Pageable pageable) {

        // Filtros completos: período + (tipo E/OU categoria)
        if (startDate != null && endDate != null && (type != null || categoryId != null)) {
            return findTransactionsWithCompleteFilters(startDate, endDate, type, categoryId, pageable);
        }

        // Apenas período
        if (startDate != null && endDate != null) {
            return findTransactionsByPeriod(startDate, endDate, pageable);
        }

        // Apenas tipo
        if (type != null && categoryId == null) {
            return findTransactionsByType(type, pageable);
        }

        // Apenas categoria
        if (categoryId != null && type == null) {
            return findTransactionsByCategory(categoryId, pageable);
        }

        // Tipo E categoria (sem período)
        if (type != null && categoryId != null) {
            return findTransactionsWithCompleteFilters(null, null, type, categoryId, pageable);
        }

        // Sem filtros
        return findAllTransactions(pageable);
    }

    private Page<Transaction> findTransactionsWithCompleteFilters(LocalDate startDate, LocalDate endDate,
            String type, Long categoryId, Pageable pageable) {

        TransactionType transactionType = convertToTransactionType(type);
        return transactionService.listTransactionsWithFilters(startDate, endDate, transactionType, categoryId, pageable);
    }

    private TransactionType convertToTransactionType(String type) {
        if (type == null) {
            return null;
        }

        return TransactionType.valueOf(type);
    }

    private Page<Transaction> findTransactionsByPeriod(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionService.listTransactions(startDate, endDate, pageable);
    }

    private Page<Transaction> findAllTransactions(Pageable pageable) {
        LocalDate defaultStartDate = LocalDate.of(2000, 1, 1);
        LocalDate defaultEndDate = LocalDate.now();
        LocalDate endDateWithBuffer = defaultEndDate.plusYears(10);

        return transactionService.listTransactions(defaultStartDate, endDateWithBuffer, pageable);
    }

    private Page<Transaction> findTransactionsByType(String type, Pageable pageable) {
        TransactionType transactionType = convertToTransactionType(type);
        return transactionService.listTransactionsWithFilters(null, null, transactionType, null, pageable);
    }

    private Page<Transaction> findTransactionsByCategory(Long categoryId, Pageable pageable) {
        return transactionService.listTransactionsWithFilters(null, null, null, categoryId, pageable);
    }

    private Transaction searchTransactionById(Long id) {
        return transactionService.findById(id);
    }

    private Page<TransactionResponse> convertToResponsePage(Page<Transaction> transactions) {
        return transactions.map(transactionMapper::toResponse);
    }
}