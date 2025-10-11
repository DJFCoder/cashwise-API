package br.com.devjf.cashwise.service.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.devjf.cashwise.service.RecurrencyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Job responsável por processar automaticamente lançamentos recorrentes.
 * 
 * Executa diariamente às 01:00 da manhã, verificando lançamentos originais
 * com recorrência ativa e gerando novos lançamentos conforme necessário.
 * 
 * Regras de processamento:
 * - Apenas lançamentos originais (parentTransactionId = NULL) são processados
 * - Apenas lançamentos com recurrencyActive = true
 * - Apenas lançamentos com recurrency != UNIQUE
 * - Respeita a data de término (recurrencyEndDate) se definida
 * - Gera lançamentos apenas quando a data chega (não gera antecipadamente)
 * 
 * Exemplo de funcionamento:
 * - Usuário cadastra "Salário" em 10/10/2025 com recorrência MONTHLY
 * - Job roda diariamente, mas só gera novo lançamento quando chegar 10/11/2025
 * - Processo continua mensalmente até que:
 *   a) recurrencyActive seja definido como false, OU
 *   b) recurrencyEndDate seja atingida
 */
@Component
@Slf4j
public class RecurrencyJob {

    private final RecurrencyService recurrencyService;

    public RecurrencyJob(RecurrencyService recurrencyService) {
        this.recurrencyService = recurrencyService;
    }

    /**
     * Processa todos os lançamentos recorrentes ativos.
     * 
     * Agendamento: Executa diariamente às 01:00 da manhã
     * Cron: "0 0 1 * * ?" 
     * - Segundo: 0
     * - Minuto: 0
     * - Hora: 1
     * - Dia do mês: * (todos)
     * - Mês: * (todos)
     * - Dia da semana: ? (qualquer)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processRecurrentTransactions() {
        log.info("===== INICIANDO PROCESSAMENTO DE LANÇAMENTOS RECORRENTES =====");
        
        try {
            recurrencyService.processAllActiveRecurrencies();
            log.info("===== PROCESSAMENTO DE LANÇAMENTOS RECORRENTES FINALIZADO COM SUCESSO =====");
        } catch (Exception e) {
            log.error("===== ERRO DURANTE PROCESSAMENTO DE LANÇAMENTOS RECORRENTES =====", e);
            // Em produção, enviar alerta/notificação por aqui
        }
    }
}
