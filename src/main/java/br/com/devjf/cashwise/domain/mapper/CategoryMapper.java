package br.com.devjf.cashwise.domain.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import br.com.devjf.cashwise.domain.dto.category.CategoryRequest;
import br.com.devjf.cashwise.domain.dto.category.CategoryResponse;
import br.com.devjf.cashwise.domain.entity.Category;

/**
 * Mapper responsável pela conversão entre entidades Category e seus respectivos
 * DTOs.
 * <p>
 * Esta classe implementa o padrão Mapper para separar a camada de transição de
 * dados (DTOs) da camada de domínio (Entidades). <br>
 * Fornece métodos para conversão individual e em lote, otimizando
 * operações com múltiplas categorias.
 * </p>
 *
 * @author devjf
 */
@Component
public class CategoryMapper {

    /**
     * Converte um DTO de requisição de categoria em uma entidade Category.
     * <p>
     * Este método cria uma nova instância de Category e popula apenas o campo
     * 'name', pois os demais campos (id, createdAt, updatedAt, transactions)
     * são gerenciados automaticamente pela JPA através de anotações @PrePersist
     * e @PreUpdate.
     * </p>
     *
     * @param request DTO contendo os dados da categoria a ser criada
     * @return entidade Category populada com o nome fornecido
     * @throws IllegalArgumentException se o request for null
     */
    public Category toEntity(CategoryRequest request) {
        validateRequest(request);

        Category category = new Category();
        category.setName(sanitizeName(request.name()));

        return category;
    }

    /**
     * Converte uma entidade Category em um DTO de resposta.
     * <p>
     * Transforma a entidade de domínio em um objeto de transferência de dados
     * adequado para serialização e envio ao cliente. Expõe apenas os dados
     * essenciais (id e name), seguindo o princípio de mínima exposição de
     * dados.
     * </p>
     * <p>
     * <strong>Nota:</strong> Não inclui informações de auditoria (createdAt,
     * updatedAt) nem a lista de transações associadas.
     * </p>
     *
     * @param entity entidade Category a ser convertida
     * @return DTO CategoryResponse contendo id e nome da categoria
     * @throws IllegalArgumentException se a entidade for null
     */
    public CategoryResponse toResponse(Category entity) {
        validateEntity(entity);

        return new CategoryResponse(
                entity.getId(),
                entity.getName());
    }

    /**
     * Converte uma lista de entidades Category em uma lista de DTOs de
     * resposta.
     * <p>
     * Método utilitário para conversão em lote, útil em operações de listagem e
     * busca que retornam múltiplas categorias. Utiliza streams para
     * processamento funcional e imutável.
     * </p>
     * <p>
     * <strong>Performance:</strong> Para grandes volumes de dados, é necessário
     * implementar paginação na camada de serviço antes de chamar este método.
     * </p>
     *
     * @param entities lista de entidades Category a serem convertidas
     * @return lista de DTOs CategoryResponse correspondentes
     * @throws IllegalArgumentException se a lista for null
     */
    public List<CategoryResponse> toResponseList(List<Category> entities) {
        validateEntityList(entities);

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Atualiza uma entidade Category existente com dados de um DTO de
     * requisição.
     * <p>
     * Este método implementa o padrão de atualização parcial, modificando
     * apenas os campos fornecidos no request. Útil em operações de PUT/PATCH
     * onde a entidade já existe no contexto de persistência.
     * </p>
     * <p>
     * <strong>Importante:</strong> Este método não persiste a entidade, apenas
     * atualiza seus campos em memória. A persistência é gerenciada pela
     * camada de serviço.
     * </p>
     *
     * @param entity  entidade Category existente a ser atualizada
     * @param request DTO contendo os novos dados
     * @throws IllegalArgumentException se entity ou request forem null
     */
    public void updateEntityFromRequest(Category entity, CategoryRequest request) {
        validateEntity(entity);
        validateRequest(request);

        entity.setName(sanitizeName(request.name()));
    }

    /**
     * Valida se o DTO de requisição não é nulo.
     *
     * @param request DTO a ser validado
     * @throws IllegalArgumentException se o request for null
     */
    private void validateRequest(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CategoryRequest não pode ser nulo");
        }
    }

    /**
     * Valida se a entidade não é nula.
     *
     * @param entity entidade a ser validada
     * @throws IllegalArgumentException se a entidade for null
     */
    private void validateEntity(Category entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Category não pode ser nula");
        }
    }

    /**
     * Valida se a lista de entidades não é nula.
     * <p>
     * Permite listas vazias, pois representam um estado válido (nenhuma
     * categoria encontrada), mas rejeita referências nulas que indicariam erro
     * de programação.
     * </p>
     *
     * @param entities lista a ser validada
     * @throws IllegalArgumentException se a lista for null
     */
    private void validateEntityList(List<Category> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("Lista de categorias não pode ser nula");
        }
    }

    /**
     * Sanitiza o nome da categoria removendo espaços extras.
     * <p>
     * Remove espaços em branco no início e fim da string, garantindo
     * consistência nos dados armazenados. Previne problemas de duplicação por
     * diferenças de espaçamento.
     * </p>
     * <p>
     * <strong>Exemplo:</strong> " Alimentação " → "Alimentação"
     * </p>
     *
     * @param name nome a ser sanitizado
     * @return nome sem espaços extras no início e fim
     */
    private String sanitizeName(String name) {
        return name != null ? name.trim() : null;
    }
}
