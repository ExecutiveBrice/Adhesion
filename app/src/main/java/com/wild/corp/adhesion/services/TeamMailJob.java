package com.wild.corp.adhesion.services;

import com.google.gson.Gson;
import com.wild.corp.adhesion.models.*;
import com.wild.corp.adhesion.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

/** Durable claim before HTTP; an uncertain send is never retried automatically. */
@Component
public class TeamMailJob {
    private final TeamMailRepository mails;
    private final ConvocationEquipeRepository invitations;
    private final EmailService email;
    private final TransactionTemplate tx;
    private final String key;
    private final String frontend;
    private final HttpClient http;
    @org.springframework.beans.factory.annotation.Autowired
    public TeamMailJob(TeamMailRepository mails, ConvocationEquipeRepository invitations, EmailService email,
            org.springframework.transaction.PlatformTransactionManager manager,
            @Value("${brevo.newsletter.api-key:}") String key,
            @Value("${adhesion.frontend.base-url:http://localhost:4200}") String frontend) {
        this(mails, invitations, email, manager, key, frontend,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }
    TeamMailJob(TeamMailRepository mails, ConvocationEquipeRepository invitations, EmailService email,
            org.springframework.transaction.PlatformTransactionManager manager, String key, String frontend, HttpClient http) {
        this.mails = mails; this.invitations = invitations; this.email = email; this.tx = new TransactionTemplate(manager); this.key = key;
        this.frontend = frontend; this.http = http;
    }
    private record Delivery(Long id, String address, String subject, String content) { }

    @Scheduled(fixedDelayString = "${adhesion.teams.mail-delay-ms:15000}", initialDelayString = "${adhesion.teams.mail-initial-delay-ms:30000}")
    public void sendPending() {
        if (key.isBlank() || !email.isMailSendingEnabled()) return;
        // One request per tick also bounds the shared provider traffic.
        Delivery d = tx.execute(status -> {
            TeamMail m = mails.findFirstByEtatOrderByIdAsc("PENDING").orElse(null);
            if (m == null) return null;
            Seance s = m.getSeance();
            List<ConvocationEquipe> validRecipients = invitations.findAllById(m.getConvocations()).stream()
                    .filter(c -> TeamRoster.contactAddress(c.getAdherent()).equals(m.getDestinataire()))
                    .filter(c -> switch (m.getType()) {
                        case "ANNULATION" -> s.getEtatSeance() == ESeance.ANNULEE;
                        case "RETRAIT" -> !c.isSelectionnee();
                        default -> c.isSelectionnee() && c.getPublication() != null
                                && TeamRoster.eligible(c, s)
                                && (!"RELANCE".equals(m.getType()) || !c.hasReplied());
                    }).toList();
            boolean valid = m.getRevision() == s.getTeamRevision() && !validRecipients.isEmpty()
                    && (m.isAnnulation() || (s.getEtatSeance() != ESeance.ANNULEE
                    && s.getDebut().isAfter(LocalDateTime.now())));
            if (!valid) { m.setEtat("CANCELLED"); return null; }
            // Rebuild the names so a sibling withdrawn since publication is not included.
            m.setContenu("<p>" + html(m.getType()) + " : " + html(s.getActivite().getNom()) + "</p><p>"
                    + validRecipients.stream().map(c -> html(c.recipientLabel())).collect(java.util.stream.Collectors.joining(", "))
                    + "</p><p>" + html(s.getDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    + "</p><p>" + html(s.getLieuExterieur() != null ? s.getLieuExterieur() : (s.getSalle() == null ? "" : s.getSalle().getNom()))
                    + "</p><p><a href=\"" + html(frontend.replaceAll("/$", "") + "/evenements/" + s.getId()) + "\">Consulter l’événement</a></p>");
            m.setEtat("SENDING");
            return new Delivery(m.getId(), m.getDestinataire(), m.getSujet(), m.getContenu());
        });
        if (d == null) return;
        String outcome;
        try {
            String body = new Gson().toJson(Map.of("sender", Map.of("email", "adhesion@alod.fr", "name", "ALOD"),
                    "to", List.of(Map.of("email", d.address())), "subject", d.subject(), "htmlContent", d.content()));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(20)).header("api-key", key).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int code = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            outcome = code >= 200 && code < 300 ? "SENT" : (code >= 400 && code < 500 ? "REJECTED" : "UNKNOWN");
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); outcome = "UNKNOWN"; }
        catch (Exception ex) { outcome = "UNKNOWN"; }
        String state = outcome;
        tx.executeWithoutResult(status -> mails.findById(d.id()).ifPresent(m -> m.setEtat(state)));
    }
    private String html(String text) { return org.springframework.web.util.HtmlUtils.htmlEscape(text == null ? "" : text); }
}
