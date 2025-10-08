package br.com.devjf.cashwise.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devjf.cashwise.domain.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
