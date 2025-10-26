package br.com.devjf.cashwise.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devjf.cashwise.domain.dto.transaction.TransactionResponse;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.mapper.TransactionMapper;
import br.com.devjf.cashwise.service.RecurrencyService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para gerenciamento de recorrências de lançamentos.
 * <p>
 * Fornece endpoints para controlar o comportamento de lançamentos recorrentes,
 * permitindo ativar, desativar e definir data de término para recorrências.
 * </p>
 * <p>
 * Endpoints disponíveis:
 * - POST /api/recorrencia/{id}/desativar - Desativa recorrência de um lançamento
 * - POST /api/recorrencia/{id}/ativar - Ativa recorrência de um lançamento
 * - POST /api/recorrencia/{id}/data-final - Define data de término da recorrência
 * - GET /api/recorrencia/{id}/children - Lista lançamentos filhos gerados
 * - GET /api/recorrencia/{id}/count - Conta quantos filhos foram gerados
 * </p>
 * <p>
 * Regras de negócio:
 * - Apenas lançamentos originais (não filhos) podem ter recorrência gerenciada
 * - Lançamentos do tipo UNIQUE não possuem recorrência para ativar
 * - Data de término não pode ser anterior à data do lançamento original
 * - Desativar recorrência não exclui lançamentos filhos já gerados
 * </p>
 * 
 * @author devjf
 */
@RestController
@RequestMapping("/api/recorrencia")
@Slf4j
public class RecurrencyController {

    private final RecurrencyService recurrencyService;
    private final TransactionMapper transactionMapper;

    public RecurrencyController(RecurrencyService recurrencyService, TransactionMapper transactionMapper) {
        this.recurrencyService = recurrencyService;
        this.transactionMapper = transactionMapper;
    }

    /**
     * Desativa a recorrência de um lançamento.
     * <p>
     * Endpoint: POST /api/recorrencia/{id}/desativar
     * </p>
     * <p>
     * Após desativar, o job automático não gerará mais lançamentos filhos
     * para este lançamento original. Os lançamentos filhos já gerados
     * permanecerão no sistema.
     * </p>
     * <p>
     * Validações:
     * - Lançamento deve existir
     * - Lançamento deve ser original (não pode ser filho)
     * </p>
     * 
     * @param id identificador único do lançamento original
     * @return ResponseEntity com status 200 (OK) e mensagem de sucesso
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se o lançamento for filho
     */
    @PostMapping("/{id}/desativar")
    public ResponseEntity<RecurrencyOperationResponse> deactivateRecurrency(@PathVariable Long id) {
        log.info("Recebida requisição para desativar recorrência do lançamento ID: {}", id);
        
        recurrencyService.deactivateRecurrency(id);
        
        RecurrencyOperationResponse response = new RecurrencyOperationResponse(
                id,
                "DEACTIVATED",
                "Recorrência desativada com sucesso. Novos lançamentos não serão mais gerados automaticamente."
        );
        
        log.info("Recorrência desativada com sucesso - ID: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Ativa a recorrência de um lançamento.
     * <p>
     * Endpoint: POST /api/recorrencia/{id}/ativar
     * </p>
     * <p>
     * Após ativar, o job automático voltará a gerar lançamentos filhos
     * conforme a periodicidade definida (DAILY, WEEKLY, MONTHLY, etc).
     * </p>
     * <p>
     * Validações:
     * - Lançamento deve existir
     * - Lançamento deve ser original (não pode ser filho)
     * - Lançamento não pode ser do tipo UNIQUE
     * </p>
     * 
     * @param id identificador único do lançamento original
     * @return ResponseEntity com status 200 (OK) e mensagem de sucesso
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se o lançamento for filho ou UNIQUE
     */
    @PostMapping("/{id}/ativar")
    public ResponseEntity<RecurrencyOperationResponse> activateRecurrency(@PathVariable Long id) {
        log.info("Recebida requisição para ativar recorrência do lançamento ID: {}", id);
        
        recurrencyService.activateRecurrency(id);
        
        RecurrencyOperationResponse response = new RecurrencyOperationResponse(
                id,
                "ACTIVATED",
                "Recorrência ativada com sucesso. Novos lançamentos serão gerados automaticamente."
        );
        
        log.info("Recorrência ativada com sucesso - ID: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Define a data de término da recorrência de um lançamento.
     * <p>
     * Endpoint: POST /api/recorrencia/{id}/data-final
     * </p>
     * <p>
     * Após definir a data de término, o job automático não gerará mais
     * lançamentos após essa data. Útil para recorrências temporárias.
     * </p>
     * <p>
     * Para remover a data de término (recorrência infinita), envie null no corpo.
     * </p>
     * <p>
     * Validações:
     * - Lançamento deve existir
     * - Lançamento deve ser original (não pode ser filho)
     * - Data de término não pode ser anterior à data do lançamento original
     * </p>
     * 
     * @param id identificador único do lançamento original
     * @param request DTO contendo a data de término (pode ser null)
     * @return ResponseEntity com status 200 (OK) e mensagem de sucesso
     * @throws EntityNotFoundException se o lançamento não existir
     * @throws IllegalArgumentException se a data for inválida ou lançamento for filho
     */
    @PostMapping("/{id}/data-final")
    public ResponseEntity<RecurrencyOperationResponse> setRecurrencyEndDate(
            @PathVariable Long id,
            @Valid @RequestBody EndDateRequest request) {
        
        log.info("Recebida requisição para definir data de término da recorrência - ID: {}, endDate: {}", 
                id, request.endDate());
        
        recurrencyService.setRecurrencyEndDate(id, request.endDate());
        
        String message = request.endDate() != null
                ? "Data de término definida com sucesso: " + request.endDate()
                : "Data de término removida. Recorrência agora é infinita.";
        
        RecurrencyOperationResponse response = new RecurrencyOperationResponse(
                id,
                "END_DATE_SET",
                message
        );
        
        log.info("Data de término definida com sucesso - ID: {}, endDate: {}", id, request.endDate());
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os lançamentos filhos gerados a partir de um lançamento original.
     * <p>
     * Endpoint: GET /api/recorrencia/{id}/children
     * </p>
     * <p>
     * Útil para visualizar o histórico completo de lançamentos gerados
     * automaticamente pela recorrência.
     * </p>
     * 
     * @param id identificador único do lançamento original
     * @return ResponseEntity com status 200 (OK) e lista de lançamentos filhos
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<TransactionResponse>> findChildTransactions(@PathVariable Long id) {
        log.info("Recebida requisição para listar lançamentos filhos do ID: {}", id);
        
        List<Transaction> children = recurrencyService.findChildTransactions(id);
        List<TransactionResponse> response = children.stream()
                .map(transactionMapper::toResponse)
                .toList();
        
        log.info("Retornando {} lançamento(s) filho(s)", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Conta quantos lançamentos filhos foram gerados a partir de um lançamento original.
     * <p>
     * Endpoint: GET /api/recorrencia/{id}/count
     * </p>
     * <p>
     * Útil para estatísticas e monitoramento de recorrências.
     * </p>
     * 
     * @param id identificador único do lançamento original
     * @return ResponseEntity com status 200 (OK) e a contagem de filhos
     */
    @GetMapping("/{id}/count")
    public ResponseEntity<ChildCountResponse> countChildTransactions(@PathVariable Long id) {
        log.info("Recebida requisição para contar lançamentos filhos do ID: {}", id);
        
        Long count = recurrencyService.countChildTransactions(id);
        
        ChildCountResponse response = new ChildCountResponse(
                id,
                count,
                count > 0 ? "Lançamentos filhos encontrados" : "Nenhum lançamento filho gerado ainda"
        );
        
        log.info("Total de lançamentos filhos: {}", count);
        return ResponseEntity.ok(response);
    }

    /**
     * DTO de requisição para definir data de término.
     */
    public record EndDateRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {}

    /**
     * DTO de resposta para operações de recorrência.
     */
    public record RecurrencyOperationResponse(
            Long transactionId,
            String operation,
            String message
    ) {}

    /**
     * DTO de resposta para contagem de filhos.
     */
    public record ChildCountResponse(
            Long parentTransactionId,
            Long childCount,
            String message
    ) {}
}
