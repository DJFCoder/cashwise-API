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

    /**
     * Converte a descrição em português para o enum correspondente.
     * 
     * @param description "Receita" ou "Despesa"
     * @return TransactionType correspondente
     * @throws IllegalArgumentException se a descrição for inválida
     */
    public static TransactionType fromDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("Tipo não pode ser nulo");
        }

        for (TransactionType type : values()) {
            if (type.description.equalsIgnoreCase(description)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Tipo inválido: " + description + ". Use 'Receita' ou 'Despesa'");
    }
}