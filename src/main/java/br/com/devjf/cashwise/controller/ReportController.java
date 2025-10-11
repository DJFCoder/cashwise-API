package br.com.devjf.cashwise.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.devjf.cashwise.service.ReportService;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para geração de relatórios financeiros.
 * <p>
 * Fornece endpoints para consulta de dados analíticos e estatísticos
 * sobre os lançamentos financeiros, facilitando a visualização e
 * tomada de decisões.
 * </p>
 * <p>
 * Endpoints disponíveis:
 * - GET /api/relatorio/balancete - Calcula o balancete financeiro em um período
 * - GET /api/relatorio/distribuicao - Retorna distribuição de gastos por categoria
 * - GET /api/relatorio/evolucao-mensal - Retorna evolução mensal de receitas e despesas
 * </p>
 *
 * @author devjf
 */
@RestController
@RequestMapping("/api/relatorio")
@Slf4j
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Calcula o balancete financeiro em um período específico.
     * <p>
     * Endpoint: GET /api/relatorio/balancete
     * </p>
     * <p>
     * O balancete é calculado como: Total de Receitas - Total de Despesas
     * </p>
     * <p>
     * Parâmetros de consulta:
     * - startDate: Data inicial do período (obrigatório, formato: yyyy-MM-dd)
     * - endDate: Data final do período (obrigatório, formato: yyyy-MM-dd)
     * </p>
     * <p>
     * Exemplo de uso:
     * - GET /api/relatorio/balancete?startDate=2025-01-01&endDate=2025-01-31
     * </p>
     * <p>
     * Resposta:
     * - Valor positivo: Superávit (receitas > despesas)
     * - Valor negativo: Déficit (despesas > receitas)
     * - Zero: Equilíbrio (receitas = despesas)
     * </p>
     *
     * @param startDate data inicial do período
     * @param endDate data final do período
     * @return ResponseEntity com status 200 (OK) e o balancete calculado
     */
    @GetMapping("/balancete")
    public ResponseEntity<BalanceResponse> calculateBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Aplicado: KISS, SRP (validação delegada), DRY (uso do serviço),
        // Fail Fast parcialmente (validação de período); tratamento seguro dos dados retornados.
        log.info("Recebida requisição para calcular balancete - período: {} a {}", startDate, endDate);

        validatePeriod(startDate, endDate);

        // Serviço retorna um mapa com chaves: "revenues", "expenses", "balance"
        Map<String, BigDecimal> balanceData = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Usa 0 como padrão caso alguma chave esteja ausente para evitar NullPointerException
        BigDecimal revenues = balanceData.getOrDefault("revenues", BigDecimal.ZERO);
        BigDecimal expenses = balanceData.getOrDefault("expenses", BigDecimal.ZERO);
        BigDecimal balance = balanceData.getOrDefault("balance", revenues.subtract(expenses));

        BalanceResponse response = new BalanceResponse(
                startDate,
                endDate,
                revenues,
                expenses,
                balance,
                determineBalanceStatus(balance)
        );

        log.info("balancete calculado: {} (status: {})", balance, response.status());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna a distribuição de gastos por categoria em um período.
     * <p>
     * Endpoint: GET /api/relatorio/distribuicao
     * </p>
     * <p>
     * Agrupa todas as despesas por categoria e retorna o total gasto em cada uma.
     * Útil para visualizar onde o dinheiro está sendo mais gasto.
     * </p>
     * <p>
     * Parâmetros de consulta:
     * - startDate: Data inicial do período (obrigatório, formato: yyyy-MM-dd)
     * - endDate: Data final do período (obrigatório, formato: yyyy-MM-dd)
     * </p>
     * <p>
     * Exemplo de uso:
     * - GET /api/relatorio/distribuicao?startDate=2025-01-01&endDate=2025-01-31
     * </p>
     * <p>
     * Resposta: Mapa com nome da categoria como chave e total gasto como valor
     * Exemplo:
     * {
     *   "Alimentação": 1500.00,
     *   "Transporte": 800.00,
     *   "Lazer": 500.00
     * }
     * </p>
     *
     * @param startDate data inicial do período
     * @param endDate data final do período
     * @return ResponseEntity com status 200 (OK) e a distribuição por categoria
     */
    @GetMapping("/distribuicao")
    public ResponseEntity<DistributionResponse> getDistributionByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Recebida requisição para obter distribuição por categoria - período: {} a {}",
                startDate, endDate);

        validatePeriod(startDate, endDate);

        Map<String, BigDecimal> distribution = reportService.getDistributionByCategory(startDate, endDate);

        DistributionResponse response = new DistributionResponse(
                startDate,
                endDate,
                distribution,
                distribution.size()
        );

        log.info("Distribuição calculada: {} categoria(s)", distribution.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna a evolução mensal de receitas e despesas para um ano específico.
     * <p>
     * Endpoint: GET /api/relatorio/evolucao-mensal
     * </p>
     * <p>
     * Agrupa lançamentos por mês e retorna o total de receitas e despesas de cada mês.
     * Útil para visualizar a evolução financeira ao longo do ano.
     * </p>
     * <p>
     * Parâmetros de consulta:
     * - year: Ano para análise (obrigatório)
     * </p>
     * <p>
     * Exemplo de uso:
     * - GET /api/relatorio/evolucao-mensal?year=2025
     * </p>
     * <p>
     * Resposta: Lista de objetos contendo mês, ano, receitas e despesas
     * Exemplo:
     * [
     *   {
     *     "month": 1,
     *     "year": 2025,
     *     "revenues": 5000.00,
     *     "expenses": 3000.00
     *   },
     *   {
     *     "month": 2,
     *     "year": 2025,
     *     "revenues": 5500.00,
     *     "expenses": 3200.00
     *   }
     * ]
     * </p>
     *
     * @param year ano para análise
     * @return ResponseEntity com status 200 (OK) e a evolução mensal
     */
    @GetMapping("/evolucao-mensal")
    public ResponseEntity<MonthlyEvolutionResponse> getMonthlyEvolution(
            @RequestParam int year) {

        log.info("Recebida requisição para obter evolução mensal - ano: {}", year);

        validateYear(year);

        List<Map<String, Object>> evolution = reportService.getMonthlyEvolution(year);

        MonthlyEvolutionResponse response = new MonthlyEvolutionResponse(
                year,
                evolution,
                evolution.size()
        );

        log.info("Evolução mensal calculada: {} mês(es)", evolution.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Valida se o período fornecido é válido.
     *
     * @param startDate data inicial
     * @param endDate data final
     * @throws IllegalArgumentException se o período for inválido
     */
    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final");
        }
    }

    /**
     * Valida se o ano fornecido é válido.
     *
     * @param year ano a ser validado
     * @throws IllegalArgumentException se o ano for inválido
     */
    private void validateYear(int year) {
        int currentYear = LocalDate.now().getYear();

        if (year < 2000 || year > currentYear + 10) {
            throw new IllegalArgumentException(
                    "Ano inválido. Deve estar entre 2000 e " + (currentYear + 10));
        }
    }

    /**
     * Determina o status do balancete baseado no valor.
     *
     * @param balance valor do balancete
     * @return status do balancete (SUPERAVIT, DEFICIT ou EQUILIBRIO)
     */
    private String determineBalanceStatus(BigDecimal balance) {
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            return "SUPERAVIT";
        } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return "DEFICIT";
        } else {
            return "EQUILIBRIO";
        }
    }

    /**
     * DTO de resposta para o endpoint de balancete.
     */
    public record BalanceResponse(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal revenues,
            BigDecimal expenses,
            BigDecimal balance,
            String status
    ) {}

    /**
     * DTO de resposta para o endpoint de distribuição por categoria.
     */
    public record DistributionResponse(
            LocalDate startDate,
            LocalDate endDate,
            Map<String, BigDecimal> distribution,
            int totalCategories
    ) {}

    /**
     * DTO de resposta para o endpoint de evolução mensal.
     */
    public record MonthlyEvolutionResponse(
            int year,
            List<Map<String, Object>> monthlyData,
            int totalMonths
    ) {}
}
