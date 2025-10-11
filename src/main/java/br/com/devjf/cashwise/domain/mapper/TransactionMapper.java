package br.com.devjf.cashwise.domain.mapper;

import org.springframework.stereotype.Component;

import br.com.devjf.cashwise.domain.dto.category.CategoryResponse;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionRequest;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionResponse;
import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Mapper responsável pela conversão entre entidades Transaction e seus
 * respectivos DTOs.
 * <p>
 * Esta classe implementa o padrão Mapper para separar a camada de recebimento
 * de dados (DTOs) da camada de domínio (Entidades).
 * </p>
 *
 * @author devjf
 */
@Component
public class TransactionMapper {

    private final CategoryService categoryService;

    public TransactionMapper(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Converte um DTO de requisição de lançamento em uma entidade Transaction.
     * <p>
     * Este método realiza as seguintes operações:
     * <ul>
     * <li>Converte strings de tipo e recorrência para seus respectivos
     * enums</li>
     * <li>Busca e valida a categoria associada no banco de dados</li>
     * <li>Popula todos os campos da entidade Transaction</li>
     * </ul>
     * </p>
     *
     * @param request DTO contendo os dados do lançamento a ser criado
     * @return entidade Transaction populada com os dados do request
     * @throws EntityNotFoundException  se a categoria especificada não existir
     * @throws IllegalArgumentException se o tipo ou recorrência forem inválidos
     */
    public Transaction toEntity(TransactionRequest request) {
        Transaction transaction = new Transaction();

        transaction.setType(convertToTransactionType(request.type()));
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setRecurrency(convertToRecurrencyType(request.recurrency()));
        transaction.setCategory(categoryService.findCategoryById(request.categoryId()));

        return transaction;
    }

    /**
     * Converte uma entidade Transaction em um DTO de resposta.
     * <p>
     * Transforma a entidade de domínio em um objeto de transferência de dados
     * adequado para serialização e envio ao cliente. Inclui a conversão da
     * categoria associada e formatação de datas.
     * </p>
     *
     * @param entity entidade Transaction a ser convertida
     * @return DTO TransactionResponse contendo os dados formatados do lançamento
     */
    public TransactionResponse toResponse(Transaction entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getType().name(),
                mapCategoryToResponse(entity.getCategory()),
                entity.getAmount(),
                entity.getCreatedAt().toLocalDate(),
                entity.getDescription(),
                entity.getRecurrency().name());
    }

    /**
     * Converte uma string em português em um enum TransactionType.
     * <p>
     * Utiliza o método fromDescription() do enum para converter
     * "Receita" ou "Despesa" para seus respectivos valores enum.
     * </p>
     *
     * @param type string representando o tipo do lançamento em português
     *             (exemplo: "Receita", "Despesa")
     * @return enum TransactionType correspondente
     * @throws IllegalArgumentException se o tipo fornecido não corresponder a
     *                                  nenhum valor válido
     */
    private TransactionType convertToTransactionType(String type) {
        return TransactionType.fromDescription(type);
    }

    /**
     * Converte uma string em um enum RecurrencyType.
     * <p>
     * Realiza a conversão case-insensitive, transformando a string em
     * maiúsculas antes da conversão para garantir compatibilidade.
     * </p>
     *
     * @param recurrency string representando o tipo de recorrência (ex:
     *                   "MONTHLY", "UNIQUE")
     * @return enum RecurrencyType correspondente
     * @throws IllegalArgumentException se a recorrência fornecida não
     *                                  corresponder a nenhum valor do enum
     */
    private RecurrencyType convertToRecurrencyType(String recurrency) {
        return RecurrencyType.valueOf(recurrency.toUpperCase());
    }

    /**
     * Converte uma entidade Category em um DTO CategoryResponse.
     *
     * @param category entidade Category a ser convertida
     * @return DTO CategoryResponse contendo id e nome da categoria
     */
    private CategoryResponse mapCategoryToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName());
    }
}