package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.AdherentRepository;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewsletterSyncServiceTest {
    private final AdherentRepository adherents = mock(AdherentRepository.class);
    private final NewsletterContactService contacts = mock(NewsletterContactService.class);
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    private final NewsletterSyncService service = new NewsletterSyncService(adherents, contacts, tasks::add);

    @Test
    void normalizesDeduplicatesAndCountsInvalidAddressesWithoutMutatingThem() {
        when(adherents.findAllContactEmails()).thenReturn(Arrays.asList(" A@Example.org ", "a@example.org",
                null, "", "invalid", "b@example.org", "c@example.org", "d@example.org"));
        when(contacts.subscribeFromAdministration("a@example.org")).thenReturn(NewsletterContactService.AdminSubscription.SUBSCRIBED);
        when(contacts.subscribeFromAdministration("b@example.org")).thenReturn(NewsletterContactService.AdminSubscription.RESUBSCRIBED);
        when(contacts.subscribeFromAdministration("c@example.org")).thenReturn(NewsletterContactService.AdminSubscription.ALREADY_SUBSCRIBED);
        when(contacts.subscribeFromAdministration("d@example.org")).thenReturn(NewsletterContactService.AdminSubscription.STILL_UNSUBSCRIBED);
        assertThat(service.start().running()).isTrue();
        tasks.remove().run();
        assertThat(service.status()).isEqualTo(new NewsletterSyncService.Progress(false, 4, 4, 1, 1, 1, 1, 4, ""));
        verify(contacts, times(4)).subscribeFromAdministration(anyString());
    }

    @Test
    void onlyOneBatchRunsAndQuotaErrorsPreservePartialProgressWithoutReplaying() {
        when(adherents.findAllContactEmails()).thenReturn(List.of("a@example.org", "b@example.org", "c@example.org"));
        when(contacts.subscribeFromAdministration("a@example.org")).thenReturn(NewsletterContactService.AdminSubscription.SUBSCRIBED);
        when(contacts.subscribeFromAdministration("b@example.org")).thenThrow(new NewsletterApiException(429, Duration.ofSeconds(60)));
        service.start(); service.start();
        assertThat(tasks).hasSize(1);
        tasks.remove().run();
        assertThat(service.status().running()).isFalse();
        assertThat(service.status().processed()).isEqualTo(1);
        assertThat(service.status().error()).contains("429", "60 secondes");
        verify(contacts, never()).subscribeFromAdministration("c@example.org");
        service.start();
        assertThat(tasks).hasSize(1);
    }
}
