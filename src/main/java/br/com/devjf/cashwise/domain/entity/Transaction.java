package br.com.devjf.cashwise.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Data;

@Entity
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 11)
    private RecurrencyType recurrency;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== NOVOS CAMPOS PARA CONTROLE DE RECORRÊNCIA ==========

    /**
     * Referência ao lançamento original que gerou este lançamento.
     * NULL = é um lançamento original (parent)
     * NOT NULL = é um lançamento gerado automaticamente (child)
     */
    @Column(name = "parent_transaction_id")
    private Long parentTransactionId;

    /**
     * Indica se este lançamento está gerando novos lançamentos recorrentes.
     * Apenas lançamentos originais (parentTransactionId = NULL) podem ter este campo como true.
     * Lançamentos filhos sempre terão false.
     */
    @Column(name = "recurrency_active", nullable = false)
    private Boolean recurrencyActive = true;

    /**
     * Data opcional de término da recorrência.
     * Se preenchida, o job não gerará lançamentos após esta data.
     * Aplicável apenas para lançamentos originais.
     */
    @Column(name = "recurrency_end_date")
    private LocalDate recurrencyEndDate;

    // ================================================================

    @PrePersist
    protected void onCreate() {
        // Só seta createdAt se ainda não foi definido (permite testes mockarem datas)
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        
        // Define recurrencyActive como true por padrão se for lançamento original
        if (this.recurrencyActive == null) {
            this.recurrencyActive = (this.parentTransactionId == null);
        }
    }

    public boolean isRevenue() {
        return TransactionType.REVENUE.equals(this.type);
    }

    public boolean isExpense() {
        return TransactionType.EXPENSE.equals(this.type);
    }

    /**
     * Verifica se este é um lançamento original (parent).
     * @return true se for lançamento original, false se for filho
     */
    public boolean isOriginalTransaction() {
        return this.parentTransactionId == null;
    }

    /**
     * Verifica se este é um lançamento filho (gerado automaticamente).
     * @return true se for lançamento filho, false se for original
     */
    public boolean isChildTransaction() {
        return this.parentTransactionId != null;
    }

    /**
     * Verifica se este lançamento está apto a gerar novos lançamentos recorrentes.
     * @return true se for original, recorrência ativa e não for UNIQUE
     */
    public boolean canGenerateRecurrency() {
        return isOriginalTransaction() 
            && Boolean.TRUE.equals(this.recurrencyActive)
            && !RecurrencyType.UNIQUE.equals(this.recurrency);
    }

    /**
     * Verifica se a recorrência deve continuar baseado na data de término.
     * @param referenceDate data de referência para comparação
     * @return true se deve continuar, false se atingiu a data de término
     */
    public boolean shouldContinueRecurrency(LocalDate referenceDate) {
        if (this.recurrencyEndDate == null) {
            return true;
        }
        return referenceDate.isBefore(this.recurrencyEndDate) 
            || referenceDate.isEqual(this.recurrencyEndDate);
    }
}