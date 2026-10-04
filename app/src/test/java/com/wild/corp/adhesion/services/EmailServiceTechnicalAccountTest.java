package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.EmailContent;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailServiceTechnicalAccountTest {
    @Test
    void excludesTechnicalAccountsFromMailingRecipients() {
        assertThat(EmailService.patternMatches("adherent-123@sans-email.invalid", ".*@.*")).isFalse();
        assertThat(EmailService.patternMatches("ADHERENT-123@SANS-EMAIL.INVALID", ".*@.*")).isFalse();
        assertThat(EmailService.patternMatches("alice@example.org", ".*@.*")).isTrue();
    }

    @Test
    void skipsSendingMessagesAndTemplatesToTechnicalAccounts() {
        EmailService service = spy(new EmailService());
        service.singleMessage(List.of("adherent-123@sans-email.invalid"), new EmailContent(), null, false);
        service.sendTemplate("adherent-123@sans-email.invalid", 1L);

        verify(service, never()).isMailSendingEnabled();
    }
}
