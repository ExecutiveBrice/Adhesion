package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import com.wild.corp.adhesion.utils.Status;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeamMailJobTest {
    TeamMailRepository mails;
    ConvocationEquipeRepository invitations;
    HttpClient http;
    TeamMailJob job;
    TeamMail mail;
    ConvocationEquipe c;
    @BeforeEach void setup() {
        mails = mock(TeamMailRepository.class); invitations = mock(ConvocationEquipeRepository.class); http = mock(HttpClient.class);
        EmailService email = mock(EmailService.class); when(email.isMailSendingEnabled()).thenReturn(true);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        job = new TeamMailJob(mails, invitations, email, tx, "test-key-not-used-on-network", "https://example.org", http);
        Activite activity = new Activite(); activity.setId(1L); activity.setNom("Danse");
        Seance session = new Seance(); session.setActivite(activity); session.setDebut(LocalDateTime.now().plusDays(1)); session.setEtatSeance(ESeance.PROGRAMMEE);
        Adherent person = new Adherent(); person.setPrenom("Alice"); person.setNom("Martin"); person.setUser(new User("alice@example.org", "password"));
        Adhesion membership = new Adhesion(); membership.setActivite(activity); membership.setAdherent(person); membership.setStatutActuel(Status.VALIDEE.label);
        c = new ConvocationEquipe(); c.setId(3L); c.setSeance(session); c.setAdherent(person); c.setAdhesion(membership); c.setPublication(LocalDateTime.now()); c.setSelectionnee(true);
        mail = new TeamMail(); mail.setId(4L); mail.setSeance(session); mail.setDestinataire("alice@example.org"); mail.setType("CONVOCATION"); mail.setSujet("Convocation"); mail.getConvocations().add(3L);
        when(mails.findFirstByEtatOrderByIdAsc("PENDING")).thenAnswer(call -> "PENDING".equals(mail.getEtat()) ? Optional.of(mail) : Optional.empty());
        when(invitations.findAllById(any())).thenReturn(List.of(c));
        lenient().when(mails.findById(4L)).thenReturn(Optional.of(mail));
    }
    @Test void aReplyBeforeDispatchCancelsTheReminderWithoutHttp() throws Exception {
        mail.setType("RELANCE"); c.setReponse(true); job.sendPending();
        assertThat(mail.getEtat()).isEqualTo("CANCELLED"); verify(http, never()).send(any(), any());
    }
    @Test void aCanceledOrRescheduledSessionInvalidatesPendingInvitations() throws Exception {
        mail.getSeance().setTeamRevision(1); job.sendPending();
        assertThat(mail.getEtat()).isEqualTo("CANCELLED"); verify(http, never()).send(any(), any());
    }
    @Test void changedRepresentativeAddressIsNeverSentToTheOldContact() throws Exception {
        c.getAdherent().getUser().setUsername("new@example.org"); job.sendPending();
        assertThat(mail.getEtat()).isEqualTo("CANCELLED"); verify(http, never()).send(any(), any());
    }
    @Test void anUncertainHttpOutcomeIsNotAutomaticallySentAgain() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new java.io.IOException("simulated"));
        job.sendPending(); job.sendPending();
        assertThat(mail.getEtat()).isEqualTo("UNKNOWN"); verify(http, times(1)).send(any(), any());
    }
    @Test void theDurableClaimExistsBeforeSendingAndTheContentIsEscaped() throws Exception {
        c.getAdherent().setPrenom("<Alice>");
        HttpResponse<Void> response = mock(HttpResponse.class); when(response.statusCode()).thenReturn(201);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(call -> {
            assertThat(mail.getEtat()).isEqualTo("SENDING"); return response;
        });
        job.sendPending();
        assertThat(mail.getEtat()).isEqualTo("SENT"); assertThat(mail.getContenu()).contains("&lt;Alice&gt;");
    }
}
