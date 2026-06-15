package com.maniba.eventledger.account.controller;

import com.maniba.eventledger.account.dto.AccountResponse;
import com.maniba.eventledger.account.dto.BalanceResponse;
import com.maniba.eventledger.account.dto.TransactionRequest;
import com.maniba.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(path = "/{accountId}/transactions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BalanceResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {
        log.info("POST transaction request received: accountId={}, eventId={}, type={}, amount={}",
                accountId, request.getEventId(), request.getType(), request.getAmount());
        BalanceResponse response = accountService.applyTransaction(accountId, request);
        log.info("POST transaction response produced: accountId={}, eventId={}, balance={}",
                response.getAccountId(), request.getEventId(), response.getBalance());
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        log.info("GET balance request received: accountId={}", accountId);
        BalanceResponse response = accountService.getBalance(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        log.info("GET account request received: accountId={}", accountId);
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
