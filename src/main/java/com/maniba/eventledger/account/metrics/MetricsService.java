package com.maniba.eventledger.account.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    private final Counter transactionsApplied;
    private final Counter transactionsDuplicate;
    private final Counter transactionsFailed;

    public MetricsService(MeterRegistry registry) {
        this.transactionsApplied = Counter.builder("transactions.applied.count")
                .description("Number of successfully applied transactions")
                .register(registry);
        this.transactionsDuplicate = Counter.builder("transactions.duplicate.count")
                .description("Number of duplicate transaction submissions")
                .register(registry);
        this.transactionsFailed = Counter.builder("transactions.failed.count")
                .description("Number of failed transaction attempts")
                .register(registry);
    }

    public void incrementApplied() {
        this.transactionsApplied.increment();
    }

    public void incrementDuplicate() {
        this.transactionsDuplicate.increment();
    }

    public void incrementFailed() {
        this.transactionsFailed.increment();
    }
}
