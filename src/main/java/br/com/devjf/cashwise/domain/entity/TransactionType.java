package br.com.devjf.cashwise.domain.entity;

public enum TransactionType {
    REVENUE("Receita"),
    EXPENSE("Despesa");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
