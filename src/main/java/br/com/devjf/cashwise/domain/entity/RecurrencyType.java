package br.com.devjf.cashwise.domain.entity;

public enum RecurrencyType {
    UNIQUE("Única"),
    DAILY("Diária"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    QUARTERLY("Trimestral"),
    ANNUAL("Anual");

    private final String description;

    RecurrencyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
