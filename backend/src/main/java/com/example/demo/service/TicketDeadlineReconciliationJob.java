package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketDeadlineReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(TicketDeadlineReconciliationJob.class);

    private final TicketDeadlineService deadlineService;

    public TicketDeadlineReconciliationJob(TicketDeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @Scheduled(fixedDelayString = "${tickets.deadline-reconciliation-delay-ms:900000}", initialDelayString = "${tickets.deadline-reconciliation-delay-ms:900000}")
    public void reconcile() {
        try {
            deadlineService.reconcile();
        } catch (RuntimeException exception) {
            log.error("Ticket deadline reconciliation failed; it will retry on the next scheduled run", exception);
        }
    }
}
