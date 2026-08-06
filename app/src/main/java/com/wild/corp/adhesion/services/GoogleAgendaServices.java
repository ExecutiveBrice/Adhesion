package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.resources.CalendrierGoogleResponse;
import com.wild.corp.adhesion.models.resources.EvenementGoogleAgendaResponse;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Status;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class GoogleAgendaServices {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final int MAX_AGENDAS = 10;
    private static final int MAX_ICAL_BYTES = 5 * 1024 * 1024;
    private final HttpClient httpClient;

    public GoogleAgendaServices() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    GoogleAgendaServices(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CalendrierGoogleResponse getCalendrier(LocalDate dateDebut, LocalDate dateFin, List<String> sources) {
        validerPeriode(dateDebut, dateFin);
        Set<String> agendas = new LinkedHashSet<>();
        if (sources != null) {
            sources.stream().map(this::extraireIdentifiant).filter(id -> !id.isBlank()).forEach(agendas::add);
        }
        if (agendas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun agenda Google public n'est indiqué");
        }
        if (agendas.size() > MAX_AGENDAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre d'agendas Google est limité à " + MAX_AGENDAS);
        }

        List<EvenementGoogleAgendaResponse> evenements = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();
        for (String agenda : agendas) {
            try {
                evenements.addAll(chargerAgenda(agenda, dateDebut, dateFin));
            } catch (Exception exception) {
                log.warn("Impossible de charger l'agenda Google {} : {}", agenda, exception.getMessage());
                erreurs.add("Agenda " + nomCourt(agenda) + " : impossible de charger le flux public");
            }
        }
        evenements.sort(Comparator.comparing(EvenementGoogleAgendaResponse::debut));
        return new CalendrierGoogleResponse(evenements, erreurs);
    }

    List<EvenementGoogleAgendaResponse> lireCalendrier(byte[] contenu, String identifiant,
                                                        LocalDate dateDebut, LocalDate dateFin) throws Exception {
        var calendrier = new CalendarBuilder().build(new ByteArrayInputStream(contenu));
        String nomAgenda = calendrier.<Property>getProperty("X-WR-CALNAME")
                .map(Property::getValue).filter(nom -> !nom.isBlank()).orElse(nomCourt(identifiant));
        List<EvenementGoogleAgendaResponse> evenements = new ArrayList<>();

        for (var composant : calendrier.getComponents()) {
            if (!(composant instanceof VEvent evenement)) {
                continue;
            }
            try {
                if (evenement.getStatus() != null
                        && Status.VALUE_CANCELLED.equalsIgnoreCase(evenement.getStatus().getValue())) {
                    continue;
                }
                Temporal debutInitial = evenement.getStartDate().map(date -> date.getDate()).orElse(null);
                if (debutInitial == null) {
                    continue;
                }
                boolean journeeEntiere = debutInitial instanceof LocalDate;
                for (Period<?> occurrence : occurrences(evenement, debutInitial, dateDebut, dateFin)) {
                    LocalDateTime debut = heureLocale(occurrence.getStart());
                    LocalDateTime fin = heureLocale(occurrence.getEnd());
                    if (fin == null || !fin.isAfter(debut)) {
                        fin = journeeEntiere ? debut.plusDays(1) : debut;
                    }
                    String uid = evenement.getUid().map(Property::getValue).orElse("google");
                    String titre = evenement.getSummary() == null || evenement.getSummary().getValue().isBlank()
                            ? "Événement Google Agenda" : evenement.getSummary().getValue();
                    String lieu = evenement.getLocation() == null ? null : evenement.getLocation().getValue();
                    String commentaire = evenement.<Property>getProperty("DESCRIPTION")
                            .map(Property::getValue).filter(description -> !description.isBlank()).orElse(null);
                    evenements.add(new EvenementGoogleAgendaResponse(
                            uid + "@" + debut, titre, lieu, commentaire, debut, fin,
                            journeeEntiere, nomAgenda, identifiant));
                }
            } catch (Exception exception) {
                String uid = evenement.getUid().map(Property::getValue).orElse("sans identifiant");
                log.warn("Événement {} ignoré dans l'agenda Google {} : {}", uid, identifiant,
                        exception.getMessage());
            }
        }
        return evenements;
    }

    String extraireIdentifiant(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        String valeur = source.trim();
        if (valeur.startsWith("http://") || valeur.startsWith("https://")) {
            URI uri;
            try {
                uri = URI.create(valeur);
            } catch (IllegalArgumentException exception) {
                throw sourceInvalide();
            }
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"calendar.google.com".equalsIgnoreCase(uri.getHost())) {
                throw sourceInvalide();
            }
            String chemin = uri.getRawPath();
            int ical = chemin.indexOf("/calendar/ical/");
            if (ical >= 0) {
                String suite = chemin.substring(ical + "/calendar/ical/".length());
                int slash = suite.indexOf('/');
                valeur = URLDecoder.decode(slash < 0 ? suite : suite.substring(0, slash), StandardCharsets.UTF_8);
            } else {
                valeur = parametre(uri.getRawQuery(), "src");
                if (valeur.isBlank()) {
                    valeur = decoderCid(parametre(uri.getRawQuery(), "cid"));
                }
            }
        }
        if (valeur.length() > 500 || !valeur.matches("[A-Za-z0-9._%+@#=\\-]+")) {
            throw sourceInvalide();
        }
        return valeur;
    }

    private List<EvenementGoogleAgendaResponse> chargerAgenda(String identifiant,
                                                               LocalDate dateDebut, LocalDate dateFin) throws Exception {
        String idEncode = URLEncoder.encode(identifiant, StandardCharsets.UTF_8).replace("+", "%20");
        URI uri = URI.create("https://calendar.google.com/calendar/ical/" + idEncode + "/public/basic.ics");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "text/calendar")
                .GET().build();
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }
        byte[] contenu;
        try (var flux = response.body()) {
            contenu = flux.readNBytes(MAX_ICAL_BYTES + 1);
        }
        if (contenu.length > MAX_ICAL_BYTES) {
            throw new IOException("Flux iCalendar trop volumineux");
        }
        return lireCalendrier(contenu, identifiant, dateDebut, dateFin);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Period<?>> occurrences(VEvent evenement, Temporal exemple,
                                        LocalDate dateDebut, LocalDate dateFin) {
        Period periode = periode(exemple, dateDebut, dateFin.plusDays(1));
        return new ArrayList<>((Set) evenement.calculateRecurrenceSet(periode));
    }

    private Period<?> periode(Temporal exemple, LocalDate debut, LocalDate finExclusive) {
        if (exemple instanceof LocalDate) {
            return new Period<>(debut, finExclusive);
        }
        if (exemple instanceof ZonedDateTime date) {
            return new Period<>(debut.atStartOfDay(date.getZone()), finExclusive.atStartOfDay(date.getZone()));
        }
        if (exemple instanceof OffsetDateTime date) {
            return new Period<>(debut.atStartOfDay().atOffset(date.getOffset()),
                    finExclusive.atStartOfDay().atOffset(date.getOffset()));
        }
        if (exemple instanceof Instant) {
            return new Period<>(debut.atStartOfDay(PARIS).toInstant(), finExclusive.atStartOfDay(PARIS).toInstant());
        }
        return new Period<>(debut.atStartOfDay(), finExclusive.atStartOfDay());
    }

    private LocalDateTime heureLocale(Temporal valeur) {
        if (valeur instanceof LocalDate date) return date.atStartOfDay();
        if (valeur instanceof LocalDateTime dateHeure) return dateHeure;
        if (valeur instanceof ZonedDateTime dateHeure) return dateHeure.withZoneSameInstant(PARIS).toLocalDateTime();
        if (valeur instanceof OffsetDateTime dateHeure) return dateHeure.atZoneSameInstant(PARIS).toLocalDateTime();
        if (valeur instanceof Instant instant) return instant.atZone(PARIS).toLocalDateTime();
        throw new IllegalArgumentException("Type de date iCalendar non pris en charge");
    }

    private String parametre(String query, String nom) {
        if (query == null) return "";
        for (String parametre : query.split("&")) {
            int egal = parametre.indexOf('=');
            if (egal > 0 && URLDecoder.decode(parametre.substring(0, egal), StandardCharsets.UTF_8).equals(nom)) {
                return URLDecoder.decode(parametre.substring(egal + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private String decoderCid(String cid) {
        if (cid.isBlank()) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(cid), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return cid;
        }
    }

    private String nomCourt(String identifiant) {
        int arobase = identifiant.indexOf('@');
        return arobase > 0 ? identifiant.substring(0, arobase) : identifiant;
    }

    private void validerPeriode(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)
                || ChronoUnit.DAYS.between(dateDebut, dateFin) > 370) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période du calendrier est invalide");
        }
    }

    private ResponseStatusException sourceInvalide() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les URLs publiques de calendar.google.com sont acceptées");
    }
}
