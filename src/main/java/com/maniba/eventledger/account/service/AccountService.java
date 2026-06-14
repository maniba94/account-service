package com.maniba.eventledger.account.service;

import com.maniba.eventledger.account.dto.*;
import com.maniba.eventledger.account.entity.Account;
import com.maniba.eventledger.account.entity.AccountTransaction;
import com.maniba.eventledger.account.entity.TransactionType;
import com.maniba.eventledger.account.exception.AccountNotFoundException;
import com.maniba.eventledger.account.exception.BadRequestException;
import com.maniba.eventledger.account.metrics.MetricsService;
import com.maniba.eventledger.account.repository.AccountRepository;
import com.maniba.eventledger.account.repository.AccountTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final MetricsService metricsService;

    public AccountService(AccountRepository accountRepository, AccountTransactionRepository transactionRepository, MetricsService metricsService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public BalanceResponse applyTransaction(String accountId, TransactionRequest request) {
        log.info("transaction received: accountId={}, eventId={}, type={}, amount={}", accountId, request.getEventId(), request.getType(), request.getAmount());

        // validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            metricsService.incrementFailed();
            throw new BadRequestException("Amount must be greater than zero");
        }

        // validate type
        TransactionType type;
        try {
            type = TransactionType.valueOf(request.getType().toUpperCase());
        } catch (Exception ex) {
            metricsService.incrementFailed();
            throw new BadRequestException("Invalid transaction type: " + request.getType());
        }

        // idempotency check
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            metricsService.incrementFailed();
            throw new BadRequestException("eventId is required");
        }

        if (transactionRepository.findByEventId(request.getEventId()).isPresent()) {
            log.warn("duplicate transaction detected: eventId={}", request.getEventId());
            metricsService.incrementDuplicate();
            return accountRepository.findByAccountId(accountId)
                    .map(a -> new BalanceResponse(a.getAccountId(), a.getCurrentBalance(), a.getCurrency()))
                    .orElse(new BalanceResponse(accountId, BigDecimal.ZERO, request.getCurrency()));
        }

        // find or create account
        Account account = accountRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    log.info("account created: accountId={}", accountId);
                    return new Account(accountId, BigDecimal.ZERO, request.getCurrency());
                });

        // apply amount
        BigDecimal current = account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance();
        BigDecimal updated;
        if (type == TransactionType.CREDIT) {
            updated = current.add(request.getAmount());
        } else { // DEBIT
            updated = current.subtract(request.getAmount());
        }
        account.setCurrentBalance(updated);
        account.setCurrency(request.getCurrency());

        try {
            accountRepository.save(account);

            AccountTransaction tx = new AccountTransaction(request.getEventId(), accountId, type.name(), request.getAmount(), request.getCurrency(), request.getEventTimestamp());
            transactionRepository.save(tx);
            log.info("transaction stored: eventId={}, accountId={}", request.getEventId(), accountId);
            log.info("balance updated: accountId={}, balance={}", accountId, updated);
            metricsService.incrementApplied();
        } catch (DataIntegrityViolationException dive) {
            log.warn("duplicate transaction detected during save: eventId={}", request.getEventId());
            metricsService.incrementDuplicate();
            return accountRepository.findByAccountId(accountId)
                    .map(a -> new BalanceResponse(a.getAccountId(), a.getCurrentBalance(), a.getCurrency()))
                    .orElse(new BalanceResponse(accountId, BigDecimal.ZERO, request.getCurrency()));
        }

        return new BalanceResponse(account.getAccountId(), account.getCurrentBalance(), account.getCurrency());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.warn("account lookup failed for balance: accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });
        return new BalanceResponse(account.getAccountId(), account.getCurrentBalance(), account.getCurrency());
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.warn("account lookup failed for account details: accountId={}", accountId);
                    return new AccountNotFoundException(accountId);
                });

        List<AccountTransaction> txs = transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId);
        List<TransactionResponse> txResponses = txs.stream()
                .map(t -> new TransactionResponse(t.getEventId(), t.getType(), t.getAmount(), t.getCurrency(), t.getEventTimestamp()))
                .collect(Collectors.toList());

        return new AccountResponse(account.getAccountId(), account.getCurrentBalance(), account.getCurrency(), txResponses);
    }
}
