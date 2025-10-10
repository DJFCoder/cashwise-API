package br.com.devjf.cashwise.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.devjf.cashwise.domain.entity.Category;
import br.com.devjf.cashwise.domain.entity.RecurrencyType;
import br.com.devjf.cashwise.domain.entity.Transaction;
import br.com.devjf.cashwise.domain.entity.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        /**
         * Busca lançamentos criadas dentro de um período específico com paginação.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @param pageable  configuração de paginação e ordenação
         * @return página contendo lançamentos do período
         */
        Page<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

        /**
         * Busca lançamentos por tipo (RECEITA ou DESPESA) com paginação.
         * 
         * @param type     tipo do lançamento
         * @param pageable configuração de paginação e ordenação
         * @return página contendo lançamentos do tipo especificado
         */
        Page<Transaction> findByType(TransactionType type, Pageable pageable);

        /**
         * Busca lançamentos de uma categoria específica com paginação.
         * 
         * @param category categoria dos lançamentos
         * @param pageable configuração de paginação e ordenação
         * @return página contendo lançamentos da categoria
         */
        Page<Transaction> findByCategory(Category category, Pageable pageable);

        /**
         * Busca lançamentos filtrando por período e tipo simultaneamente.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @param type      tipo do lançamento
         * @param pageable  configuração de paginação e ordenação
         * @return página contendo lançamentos que atendem aos critérios
         */
        Page<Transaction> findByCreatedAtBetweenAndType(
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        TransactionType type,
                        Pageable pageable);

        /**
         * Busca lançamentos filtrando por período e categoria simultaneamente.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @param category  categoria dos lançamentos
         * @param pageable  configuração de paginação e ordenação
         * @return página contendo lançamentos que atendem aos critérios
         */
        Page<Transaction> findByCreatedAtBetweenAndCategory(
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Category category,
                        Pageable pageable);

        /**
         * Busca lançamentos com filtro completo: período, tipo e categoria.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @param type      tipo do lançamento
         * @param category  categoria dos lançamentos
         * @param pageable  configuração de paginação e ordenação
         * @return página contendo lançamentos que atendem a todos os critérios
         */
        Page<Transaction> findByCreatedAtBetweenAndTypeAndCategory(
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        TransactionType type,
                        Category category,
                        Pageable pageable);

        /**
         * Verifica se existem lançamentos vinculados a uma categoria.
         * Útil antes de excluir categorias para evitar inconsistências.
         * 
         * @param categoryId ID da categoria a ser verificada
         * @return true se existirem lançamentos vinculados, false caso contrário
         */
        boolean existsByCategoryId(Long categoryId);

        /**
         * Busca lançamentos que não são do tipo de recorrência especificado.
         * 
         * @param uniqueType tipo de recorrência a ser excluído da busca
         * @return lista de lançamentos com outros tipos de recorrência
         */
        @Query("SELECT t FROM Transaction t WHERE t.recurrency != :uniqueType")
        List<Transaction> findByRecurrencyNot(@Param("uniqueType") RecurrencyType uniqueType);

        /**
         * Busca todas os lançamentos recorrentes que precisam ser processadas.
         * Exclui lançamentos do tipo UNIQUE.
         * 
         * @return lista de lançamentos recorrentes
         */
        @Query("SELECT t FROM Transaction t WHERE t.recurrency != 'UNIQUE'")
        List<Transaction> findRecurrentTransactionsToProcess();

        /**
         * Calcula o saldo financeiro no período considerando receitas e despesas.
         * Receitas somam positivamente, despesas subtraem.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @return saldo calculado (receitas - despesas)
         */
        @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'REVENUE' THEN t.amount ELSE -t.amount END), 0) " +
                        "FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal calculateBalanceByPeriod(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Soma o total de receitas em um período específico.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @return total de receitas ou zero se não houver
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                        "WHERE t.type = 'REVENUE' AND t.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal sumRevenueByPeriod(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Soma o total de despesas em um período específico.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @return total de despesas ou zero se não houver
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                        "WHERE t.type = 'EXPENSE' AND t.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal sumExpenseByPeriod(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Retorna a distribuição de valores agrupados por categoria no período.
         * Usado para gerar relatórios de gastos por categoria.
         * 
         * @param startDate data inicial do período
         * @param endDate   data final do período
         * @return lista de arrays contendo [nome da categoria, soma dos valores]
         */
        @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
                        "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
                        "GROUP BY t.category.id, t.category.name")
        List<Object[]> findDistributionByCategory(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Retorna a evolução mensal de receitas e despesas para um ano específico.
         * Usado para gráficos de evolução financeira.
         * 
         * @param year ano para análise
         * @return lista de arrays contendo [mês, ano, total receitas, total despesas]
         */
        @Query("SELECT FUNCTION('MONTH', t.createdAt), FUNCTION('YEAR', t.createdAt), " +
                        "SUM(CASE WHEN t.type = 'REVENUE' THEN t.amount ELSE 0 END), " +
                        "SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END) " +
                        "FROM Transaction t " +
                        "WHERE FUNCTION('YEAR', t.createdAt) = :year " +
                        "GROUP BY FUNCTION('MONTH', t.createdAt), FUNCTION('YEAR', t.createdAt) " +
                        "ORDER BY FUNCTION('MONTH', t.createdAt)")
        List<Object[]> findMonthlyEvolution(@Param("year") int year);
}
