package br.com.devjf.cashwise.domain.mapper;

import org.springframework.stereotype.Component;

import br.com.devjf.cashwise.domain.dto.category.CategoryResponse;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionRequest;
import br.com.devjf.cashwise.domain.dto.transaction.TransactionResponse;
import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;
import br.com.devjf.cashwise.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Mapper responsável pela conversão entre entidades Transaction e seus
 * respectivos DTOs.
 * <p>
 * Esta classe implementa o padrão Mapper para separar a camada de apresentação
 * (DTOs) da camada de domínio (Entidades), garantindo baixo acoplamento e alta
 * coesão.
 * </p>
 *
 * @author devjf
 */
@Component
public class TransactionMapper {

    private final CategoryRepository categoryRepository;

    public TransactionMapper(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Converte um DTO de requisição de transação em uma entidade Transaction.
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
     * @param request DTO contendo os dados da transação a ser criada
     * @return entidade Transaction populada com os dados do request
     * @throws EntityNotFoundException se a categoria especificada não existir
     * @throws IllegalArgumentException se o tipo ou recorrência forem inválidos
     */
    public Transaction toEntity(TransactionRequest request) {
        Transaction transaction = new Transaction();

        transaction.setType(convertToTransactionType(request.type()));
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setRecurrency(convertToRecurrencyType(request.recurrency()));
        transaction.setCategory(findCategoryById(request.categoryId()));

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
     * @return DTO TransactionResponse contendo os dados formatados da transação
     */
    public TransactionResponse toResponse(Transaction entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getType().name(),
                mapCategoryToResponse(entity.getCategory()),
                entity.getAmount(),
                entity.getCreatedAt().toLocalDate(),
                entity.getDescription(),
                entity.getRecurrency().name()
        );
    }

    /**
     * Converte uma string em um enum TransactionType.
     * <p>
     * Realiza a conversão case-insensitive, transformando a string em
     * maiúsculas antes da conversão para garantir compatibilidade.
     * </p>
     *
     * @param type string representando o tipo da transação (ex: "INCOME",
     * "EXPENSE")
     * @return enum TransactionType correspondente
     * @throws IllegalArgumentException se o tipo fornecido não corresponder a
     * nenhum valor do enum
     */
    private TransactionType convertToTransactionType(String type) {
        return TransactionType.valueOf(type.toUpperCase());
    }

    /**
     * Converte uma string em um enum RecurrencyType.
     * <p>
     * Realiza a conversão case-insensitive, transformando a string em
     * maiúsculas antes da conversão para garantir compatibilidade.
     * </p>
     *
     * @param recurrency string representando o tipo de recorrência (ex:
     * "MONTHLY", "ONCE")
     * @return enum RecurrencyType correspondente
     * @throws IllegalArgumentException se a recorrência fornecida não
     * corresponder a nenhum valor do enum
     */
    private RecurrencyType convertToRecurrencyType(String recurrency) {
        return RecurrencyType.valueOf(recurrency.toUpperCase());
    }

    /**
     * Busca uma categoria no banco de dados pelo seu identificador.
     * <p>
     * Implementa o padrão Fail Fast, lançando exceção imediatamente caso a
     * categoria não seja encontrada, evitando propagação de estados inválidos.
     * </p>
     *
     * @param categoryId identificador único da categoria
     * @return entidade Category encontrada
     * @throws EntityNotFoundException se nenhuma categoria com o ID fornecido
     * for encontrada
     */
    private Category findCategoryById(Long categoryId) { // Esse método é provisório enquanto não criei as classes service da aplicação, apenas para funcionar sem erro.
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                "Categoria com ID " + categoryId + " não encontrada"
        ));
    }

    /**
     * Converte uma entidade Category em um DTO CategoryResponse.
     * <p>
     * Extrai apenas os dados necessários da categoria para compor a resposta,
     * seguindo o princípio de mínima exposição de dados.
     * </p>
     *
     * @param category entidade Category a ser convertida
     * @return DTO CategoryResponse contendo id e nome da categoria
     */
    private CategoryResponse mapCategoryToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}
