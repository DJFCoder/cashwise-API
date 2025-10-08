package br.com.devjf.cashwise.domain.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO para filtrar transações. Todos os campos são opcionais e podem ser
 * combinados.
 */
public record TransactionRequestFilter(
        // Filtro por período de criação
        LocalDate initialDate,
        LocalDate endDate,
        // Filtro por tipo de transação
        @Pattern(
                regexp = "REVENUE|EXPENSE",
                message = "O tipo deve ser REVENUE ou EXPENSE"
        )
        @Size(max = 20, message = "O tipo deve conter no máximo 20 caracteres")
        String type,
        // Filtro por categoria
        @Positive(message = "O ID da categoria deve ser um número positivo")
        Long categoryId,
        // Filtro por recorrência
        @Pattern(
                regexp = "UNIQUE|DAILY|WEEKLY|MONTHLY|QUARTERLY|ANNUAL",
                message = "A recorrência deve ser UNIQUE, DAILY, WEEKLY, MONTHLY, QUARTERLY ou ANNUAL"
        )
        @Size(max = 11, message = "A recorrência deve conter no máximo 11 caracteres")
        String recurrency,
        // Filtro por faixa de valor
        @PositiveOrZero(message = "O valor mínimo deve ser zero ou positivo")
        @DecimalMax(value = "9999999999999.99", message = "O valor mínimo excede o limite permitido")
        @Digits(integer = 13, fraction = 2, message = "O valor mínimo deve ter no máximo 13 dígitos inteiros e 2 decimais")
        BigDecimal minAmount,
        @PositiveOrZero(message = "O valor máximo deve ser zero ou positivo")
        @DecimalMax(value = "9999999999999.99", message = "O valor máximo excede o limite permitido")
        @Digits(integer = 13, fraction = 2, message = "O valor máximo deve ter no máximo 13 dígitos inteiros e 2 decimais")
        BigDecimal maxAmount,
        // Filtro por descrição (busca parcial)
        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String description
        ) {

    /**
     * Valida se a data inicial não é posterior à data final. Lança exceção se
     * houver inconsistência.
     */
    public TransactionRequestFilter        {
        if (initialDate != null && endDate != null && initialDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final"
            );
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException(
                    "O valor mínimo não pode ser maior que o valor máximo"
            );
        }
    }

    /**
     * Verifica se há algum filtro aplicado, para otimizar queries.
     */
    public boolean hasAnyFilter() {
        return initialDate != null
                || endDate != null
                || type != null
                || categoryId != null
                || recurrency != null
                || minAmount != null
                || maxAmount != null
                || description != null;
    }

    /**
     * Verifica se há filtro de período.
     */
    public boolean hasPeriodFilter() {
        return initialDate != null || endDate != null;
    }

    /**
     * Verifica se há filtro de valor.
     */
    public boolean hasAmountFilter() {
        return minAmount != null || maxAmount != null;
    }
}
