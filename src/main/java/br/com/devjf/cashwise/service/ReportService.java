package br.com.devjf.cashwise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devjf.cashwise.repository.TransactionRepository;

/**
 * Serviço responsável pela geração de relatórios financeiros e análises
 * consolidadas.
 * Fornece dados agregados sobre receitas, despesas, saldos e distribuições por
 * categoria.
 * Todas as operações são somente leitura e utilizam queries otimizadas do
 * repositório.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Calcula o balancete de lançamentos em um período específico.
     * Retorna receitas totais, despesas totais e o saldo resultante.
     * 
     * @param startDate data inicial do período de análise
     * @param endDate   data final do período de análise
     * @return Map contendo três chaves:
     *         - "revenues": BigDecimal com total de receitas
     *         - "expenses": BigDecimal com total de despesas
     *         - "balance": BigDecimal com saldo (receitas - despesas)
     */
    public Map<String, BigDecimal> calculateBalanceByPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = toStartOfDay(startDate);
        LocalDateTime end = toEndOfDay(endDate);

        BigDecimal revenues = transactionRepository.sumRevenueByPeriod(start, end);
        BigDecimal expenses = transactionRepository.sumExpenseByPeriod(start, end);
        BigDecimal balance = revenues.subtract(expenses);

        return buildBalanceMap(revenues, expenses, balance);
    }

    /**
     * Retorna a distribuição de gastos agrupados por categoria em um período.
     * Útil para visualizar onde o dinheiro foi gasto e gerar gráficos de
     * pizza/barras.
     * 
     * @param startDate data inicial do período de análise
     * @param endDate   data final do período de análise
     * @return Map onde a chave é o nome da categoria (String) e o valor é o total
     *         gasto/recebido naquela categoria (BigDecimal)
     */
    public Map<String, BigDecimal> getDistributionByCategory(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = toStartOfDay(startDate);
        LocalDateTime end = toEndOfDay(endDate);

        List<Object[]> results = transactionRepository.findDistributionByCategory(start, end);

        return convertToDistributionMap(results);
    }

    /**
     * Retorna a evolução mensal de receitas e despesas ao longo de um ano.
     * Usado para gráficos de linha mostrando tendências mensais.
     * 
     * @param year ano a ser analisado (ex: 2025)
     * @return Lista de Maps, onde cada Map representa um mês contendo:
     *         - "month": Integer com número do mês (1-12)
     *         - "year": Integer com o ano
     *         - "revenues": BigDecimal com total de receitas do mês
     *         - "expenses": BigDecimal com total de despesas do mês
     */
    public List<Map<String, Object>> getMonthlyEvolution(int year) {
        List<Object[]> results = transactionRepository.findMonthlyEvolution(year);
        return convertToMonthlyEvolutionList(results);
    }

    /**
     * Converte LocalDate para início do dia (00:00:00).
     * 
     * @param date data a ser convertida
     * @return LocalDateTime no início do dia
     */
    private LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * Converte LocalDate para fim do dia (23:59:59.999999999).
     * 
     * @param date data a ser convertida
     * @return LocalDateTime no fim do dia
     */
    private LocalDateTime toEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * Constrói o mapa de balancete financeiro com receitas, despesas e saldo.
     * 
     * @param revenues total de receitas
     * @param expenses total de despesas
     * @param balance  saldo calculado
     * @return Map com os três valores
     */
    private Map<String, BigDecimal> buildBalanceMap(BigDecimal revenues, BigDecimal expenses, BigDecimal balance) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("revenues", revenues);
        result.put("expenses", expenses);
        result.put("balance", balance);
        return result;
    }

    /**
     * Converte resultado da query de distribuição em Map categoria-valor.
     * 
     * @param results lista de arrays contendo [nome_categoria, soma]
     * @return Map com categoria como chave e valor total como valor
     */
    private Map<String, BigDecimal> convertToDistributionMap(List<Object[]> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]));
    }

    /**
     * Converte resultado da query de evolução mensal em lista de Maps.
     * 
     * @param results lista de arrays contendo [mês, ano, receitas, despesas]
     * @return lista de Maps representando cada mês
     */
    private List<Map<String, Object>> convertToMonthlyEvolutionList(List<Object[]> results) {
        return results.stream()
                .map(this::buildMonthDataMap)
                .toList();
    }

    /**
     * Constrói um Map representando dados financeiros de um mês específico.
     * 
     * @param row array contendo [mês, ano, receitas, despesas]
     * @return Map com os dados estruturados
     */
    private Map<String, Object> buildMonthDataMap(Object[] row) {
        Map<String, Object> monthData = new HashMap<>();
        monthData.put("month", row[0]);
        monthData.put("year", row[1]);
        monthData.put("revenues", row[2]);
        monthData.put("expenses", row[3]);
        return monthData;
    }
}