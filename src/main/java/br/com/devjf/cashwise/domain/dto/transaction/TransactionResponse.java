package br.com.devjf.cashwise.domain.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.devjf.cashwise.domain.dto.category.CategoryResponse;

public record TransactionResponse(
        Long id,
        String type,
        CategoryResponse category,
        BigDecimal amount,
        LocalDate date,
        String description,
        String recurrency
        ) {

}
