package com.wild.corp.adhesion.shop.payment.service;

import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.Mockito.*;

class PaymentReconciliationJobTest {
    @Test
    void continuesAfterFailureAndCyclesThroughBatchesWithoutStarvingLaterPayments() {
        var attempts = mock(PaymentAttemptRepository.class);
        var payments = mock(PaymentService.class);
        var page = PageRequest.of(0, 2);
        when(attempts.findPendingHelloAssoAttemptIds(0L, page)).thenReturn(List.of(1L, 2L));
        when(attempts.findPendingHelloAssoAttemptIds(2L, page)).thenReturn(List.of(3L));
        when(payments.refreshPaymentStatus(1L)).thenThrow(new IllegalStateException("HelloAsso indisponible"));
        var job = new PaymentReconciliationJob(attempts, payments, true, 2);

        job.reconcile();
        job.reconcile();
        job.reconcile();

        verify(payments, times(2)).refreshPaymentStatus(1L);
        verify(payments, times(2)).refreshPaymentStatus(2L);
        verify(payments).refreshPaymentStatus(3L);
    }

    @Test
    void restartsScanWhenLastBatchHasDisappeared() {
        var attempts = mock(PaymentAttemptRepository.class);
        var payments = mock(PaymentService.class);
        var page = PageRequest.of(0, 1);
        when(attempts.findPendingHelloAssoAttemptIds(0L, page)).thenReturn(List.of(1L));
        when(attempts.findPendingHelloAssoAttemptIds(1L, page)).thenReturn(List.of());
        var job = new PaymentReconciliationJob(attempts, payments, true, 1);
        job.reconcile();
        job.reconcile();
        verify(payments, times(2)).refreshPaymentStatus(1L);
    }

    @Test
    void canBeDisabled() {
        var attempts = mock(PaymentAttemptRepository.class);
        var payments = mock(PaymentService.class);
        new PaymentReconciliationJob(attempts, payments, false, 50).reconcile();
        verifyNoInteractions(attempts, payments);
    }
}
