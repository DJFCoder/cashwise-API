package br.com.devjf.cashwise.domain.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotBlank(message = "O tipo de lançamento é obrigatório")
        @Size(max = 20, message = "O tipo deve conter no máximo 20 caracteres")
        @Pattern(regexp = "Receita|Despesa", message = "O tipo deve ser 'Receita' ou 'Despesa'")
        String type,
        
        @NotNull(message = "O ID da categoria é obrigatório")
        @Positive(message = "O ID da categoria deve ser um número positivo")
        Long categoryId,
        
        @NotNull(message = "O valor do lançamento é obrigatório")
        @Positive(message = "O valor deve ser positivo")
        @DecimalMax(value = "9999999999999.99", message = "O valor excede o limite permitido")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 13 dígitos inteiros e 2 decimais")
        BigDecimal amount,
        
        @Size(max = 255, message = "A descrição deve conter no máximo 255 caracteres")
        String description,
        
        @NotBlank(message = "A recorrência é obrigatória")
        @Size(max = 11, message = "A recorrência deve conter no máximo 11 caracteres")
        @Pattern(regexp = "UNIQUE|DAILY|WEEKLY|MONTHLY|QUARTERLY|ANNUAL",
                message = "A recorrência deve ser UNIQUE, DAILY, WEEKLY, MONTHLY, QUARTERLY ou ANUAL")
        String recurrency,

        @Future(message = "A data de término da recorrência deve ser uma data futura")
        LocalDate recurrencyEndDate
        ) {

}