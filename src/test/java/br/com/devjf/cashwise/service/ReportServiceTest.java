package br.com.devjf.cashwise.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devjf.cashwise.repository.TransactionRepository;

/**
 * Testes unitários para ReportService.
 * Valida cálculos e agregações de relatórios financeiros.
 * 
 * Casos de teste cobertos:
 * - CT010: Cálculo de saldo no período
 * - CT011: Distribuição por categoria
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - ReportService")
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        // Arrange: Preparação de dados de teste
        startDate = LocalDate.of(2025, 10, 1);
        endDate = LocalDate.of(2025, 10, 31);
    }

    // ==================== CT010: Cálculo de Saldo no Período ====================

    @Test
    @DisplayName("CT010 - Deve calcular saldo corretamente quando houver receitas e despesas")
    void shouldCalculateBalanceCorrectlyWithRevenuesAndExpenses() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("5000.00");
        BigDecimal totalExpenses = new BigDecimal("3000.00");
        BigDecimal expectedBalance = new BigDecimal("2000.00");

        when(transactionRepository.sumRevenueByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        assertNotNull(result, "Resultado não deve ser nulo");
        assertEquals(3, result.size(), "Deve conter 3 chaves: revenues, expenses, balance");

        assertEquals(totalRevenues, result.get("revenues"), "Total de receitas deve ser 5000.00");
        assertEquals(totalExpenses, result.get("expenses"), "Total de despesas deve ser 3000.00");
        assertEquals(expectedBalance, result.get("balance"), "Saldo deve ser 2000.00 (5000 - 3000)");

        // Verify
        verify(transactionRepository, times(1)).sumRevenueByPeriod(any(), any());
        verify(transactionRepository, times(1)).sumExpenseByPeriod(any(), any());
    }

    @Test
    @DisplayName("CT010 - Deve calcular saldo positivo quando receitas forem maiores que despesas")
    void shouldCalculatePositiveBalanceWhenRevenuesExceedExpenses() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("10000.00");
        BigDecimal totalExpenses = new BigDecimal("4500.50");

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        BigDecimal balance = result.get("balance");
        assertTrue(balance.compareTo(BigDecimal.ZERO) > 0, "Saldo deve ser positivo");
        assertEquals(new BigDecimal("5499.50"), balance);
    }

    @Test
    @DisplayName("CT010 - Deve calcular saldo negativo quando despesas forem maiores que receitas")
    void shouldCalculateNegativeBalanceWhenExpensesExceedRevenues() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("2000.00");
        BigDecimal totalExpenses = new BigDecimal("3500.00");

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        BigDecimal balance = result.get("balance");
        assertTrue(balance.compareTo(BigDecimal.ZERO) < 0, "Saldo deve ser negativo");
        assertEquals(new BigDecimal("-1500.00"), balance);
    }

    @Test
    @DisplayName("CT010 - Deve calcular saldo zero quando receitas e despesas forem iguais")
    void shouldCalculateZeroBalanceWhenRevenuesEqualExpenses() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("5000.00");
        BigDecimal totalExpenses = new BigDecimal("5000.00");

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        BigDecimal balance = result.get("balance");
        assertEquals(0, balance.compareTo(BigDecimal.ZERO), "Saldo deve ser zero");
    }

    @Test
    @DisplayName("CT010 - Deve calcular saldo quando não houver receitas")
    void shouldCalculateBalanceWhenNoRevenues() {
        // Arrange
        BigDecimal totalRevenues = BigDecimal.ZERO;
        BigDecimal totalExpenses = new BigDecimal("1500.00");

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        assertEquals(BigDecimal.ZERO, result.get("revenues"));
        assertEquals(totalExpenses, result.get("expenses"));
        assertEquals(new BigDecimal("-1500.00"), result.get("balance"));
    }

    @Test
    @DisplayName("CT010 - Deve calcular saldo quando não houver despesas")
    void shouldCalculateBalanceWhenNoExpenses() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("3000.00");
        BigDecimal totalExpenses = BigDecimal.ZERO;

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        assertEquals(totalRevenues, result.get("revenues"));
        assertEquals(BigDecimal.ZERO, result.get("expenses"));
        assertEquals(totalRevenues, result.get("balance"));
    }

    @Test
    @DisplayName("CT010 - Deve manter precisão decimal com 2 casas em valores BRL")
    void shouldMaintainDecimalPrecisionForBRLValues() {
        // Arrange
        BigDecimal totalRevenues = new BigDecimal("1234.56");
        BigDecimal totalExpenses = new BigDecimal("789.12");

        when(transactionRepository.sumRevenueByPeriod(any(), any())).thenReturn(totalRevenues);
        when(transactionRepository.sumExpenseByPeriod(any(), any())).thenReturn(totalExpenses);

        // Act
        Map<String, BigDecimal> result = reportService.calculateBalanceByPeriod(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("445.44"), result.get("balance"));
        assertEquals(2, result.get("revenues").scale());
        assertEquals(2, result.get("expenses").scale());
    }

    // ==================== CT011: Distribuição por Categoria ====================

    @Test
    @DisplayName("CT011 - Deve retornar distribuição por categoria corretamente")
    void shouldReturnDistributionByCategoryCorrectly() {
        // Arrange
        Object[] alimentacao = { "Alimentação", new BigDecimal("1500.00") };
        Object[] transporte = { "Transporte", new BigDecimal("800.00") };
        Object[] lazer = { "Lazer", new BigDecimal("500.00") };

        List<Object[]> mockResults = Arrays.asList(alimentacao, transporte, lazer);

        when(transactionRepository.findDistributionByCategory(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockResults);

        // Act
        Map<String, BigDecimal> result = reportService.getDistributionByCategory(startDate, endDate);

        // Assert
        assertNotNull(result, "Resultado não deve ser nulo");
        assertEquals(3, result.size(), "Deve conter 3 categorias");

        assertEquals(new BigDecimal("1500.00"), result.get("Alimentação"));
        assertEquals(new BigDecimal("800.00"), result.get("Transporte"));
        assertEquals(new BigDecimal("500.00"), result.get("Lazer"));

        verify(transactionRepository, times(1)).findDistributionByCategory(any(), any());
    }

    @Test
    @DisplayName("CT011 - Deve retornar mapa vazio quando não houver lançamentos no período")
    void shouldReturnEmptyMapWhenNoTransactionsInPeriod() {
        // Arrange
        List<Object[]> emptyResults = Arrays.asList();

        when(transactionRepository.findDistributionByCategory(any(), any()))
                .thenReturn(emptyResults);

        // Act
        Map<String, BigDecimal> result = reportService.getDistributionByCategory(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Mapa deve estar vazio");
        verify(transactionRepository, times(1)).findDistributionByCategory(any(), any());
    }

    @Test
    @DisplayName("CT011 - Deve agrupar valores corretamente por categoria")
    void shouldGroupValuesByCategoryCorrectly() {
        // Arrange
        Object[] categoria1 = { "Alimentação", new BigDecimal("2500.75") };
        Object[] categoria2 = { "Saúde", new BigDecimal("1200.50") };

        List<Object[]> mockResults = Arrays.asList(categoria1, categoria2);

        when(transactionRepository.findDistributionByCategory(any(), any()))
                .thenReturn(mockResults);

        // Act

        Map<String, BigDecimal> result = reportService.getDistributionByCategory(startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("Alimentação"));
        assertTrue(result.containsKey("Saúde"));
        assertEquals(new BigDecimal("2500.75"), result.get("Alimentação"));
        assertEquals(new BigDecimal("1200.50"), result.get("Saúde"));
    }

    @Test
    @DisplayName("CT011 - Deve retornar distribuição para período de um ano completo")
    void shouldReturnDistributionForFullYear() {
        // Arrange
        LocalDate yearStart = LocalDate.of(2025, 1, 1);
        LocalDate yearEnd = LocalDate.of(2025, 12, 31);

        Object[] categoria1 = { "Moradia", new BigDecimal("12000.00") };
        Object[] categoria2 = { "Educação", new BigDecimal("8000.00") };

        List<Object[]> mockResults = Arrays.asList(categoria1, categoria2);

        when(transactionRepository.findDistributionByCategory(any(), any()))
                .thenReturn(mockResults);

        // Act
        Map<String, BigDecimal> result = reportService.getDistributionByCategory(yearStart, yearEnd);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository, times(1)).findDistributionByCategory(any(), any());
    }

    // ==================== Testes Adicionais - Evolução Mensal ====================

    @Test
    @DisplayName("Deve retornar evolução mensal corretamente para um ano")
    void shouldReturnMonthlyEvolutionCorrectly() {
        // Arrange
        int year = 2025;

        Object[] january = { 1, 2025, new BigDecimal("5000.00"), new BigDecimal("3000.00") };
        Object[] february = { 2, 2025, new BigDecimal("5500.00"), new BigDecimal("3200.00") };
        Object[] march = { 3, 2025, new BigDecimal("6000.00"), new BigDecimal("3500.00") };

        List<Object[]> mockResults = Arrays.asList(january, february, march);

        when(transactionRepository.findMonthlyEvolution(year)).thenReturn(mockResults);

        // Act
        List<Map<String, Object>> result = reportService.getMonthlyEvolution(year);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size(), "Deve retornar 3 meses");

        // Validar janeiro
        Map<String, Object> janData = result.get(0);
        assertEquals(1, janData.get("month"));
        assertEquals(2025, janData.get("year"));
        assertEquals(new BigDecimal("5000.00"), janData.get("revenues"));
        assertEquals(new BigDecimal("3000.00"), janData.get("expenses"));

        // Validar fevereiro
        Map<String, Object> febData = result.get(1);
        assertEquals(2, febData.get("month"));
        assertEquals(new BigDecimal("5500.00"), febData.get("revenues"));

        verify(transactionRepository, times(1)).findMonthlyEvolution(year);
    }

    @Test
    @DisplayName("Deve retornar listavazia quando não houver dados para o ano")
    void shouldReturnEmptyListWhenNoDataForYear() {
        // Arrange
        int year = 2024;
        List<Object[]> emptyResults = Arrays.asList();

        when(transactionRepository.findMonthlyEvolution(year)).thenReturn(emptyResults);

        // Act
        List<Map<String, Object>> result = reportService.getMonthlyEvolution(year);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionRepository, times(1)).findMonthlyEvolution(year);
    }
}