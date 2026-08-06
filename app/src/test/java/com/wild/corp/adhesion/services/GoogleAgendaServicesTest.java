package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleAgendaServicesTest {

    private final GoogleAgendaServices service = new GoogleAgendaServices(HttpClient.newHttpClient());

    @Test
    void parsesTimedRecurringAndAllDayEvents() throws Exception {
        String ics = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Adhesion Test//FR
                X-WR-CALNAME:Agenda public
                BEGIN:VTIMEZONE
                TZID:Europe/Paris
                BEGIN:DAYLIGHT
                TZOFFSETFROM:+0100
                TZOFFSETTO:+0200
                DTSTART:19700329T020000
                RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU
                END:DAYLIGHT
                BEGIN:STANDARD
                TZOFFSETFROM:+0200
                TZOFFSETTO:+0100
                DTSTART:19701025T030000
                RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU
                END:STANDARD
                END:VTIMEZONE
                BEGIN:VEVENT
                UID:cours-google
                DTSTART;TZID=Europe/Paris:20260901T190000
                DTEND;TZID=Europe/Paris:20260901T200000
                RRULE:FREQ=WEEKLY;COUNT=3
                SUMMARY:Cours externe
                LOCATION:Salle Google
                DESCRIPTION:Commentaire public de l'événement
                END:VEVENT
                BEGIN:VEVENT
                UID:journee-google
                DTSTART;VALUE=DATE:20260905
                DTEND;VALUE=DATE:20260906
                SUMMARY:Journée associative
                TRANSP:TRANSPARENT
                END:VEVENT
                END:VCALENDAR
                """;

        var evenements = service.lireCalendrier(ics.getBytes(StandardCharsets.UTF_8),
                "demo@group.calendar.google.com", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(evenements).hasSize(4);
        assertThat(evenements).filteredOn(evenement -> evenement.titre().equals("Cours externe"))
                .extracting(evenement -> evenement.debut().toLocalDate())
                .containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 15));
        assertThat(evenements).filteredOn(evenement -> evenement.titre().equals("Cours externe"))
                .extracting(evenement -> evenement.commentaire())
                .containsOnly("Commentaire public de l'événement");
        assertThat(evenements).filteredOn(evenement -> evenement.journeeEntiere())
                .singleElement().satisfies(evenement -> {
                    assertThat(evenement.debut()).isEqualTo(LocalDateTime.of(2026, 9, 5, 0, 0));
                    assertThat(evenement.agenda()).isEqualTo("Agenda public");
                    assertThat(evenement.agendaSource()).isEqualTo("demo@group.calendar.google.com");
                });
    }

    @Test
    void acceptsPublicGoogleShareEmbedAndIcalUrls() {
        String id = "demo@group.calendar.google.com";
        String cid = Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(StandardCharsets.UTF_8));

        assertThat(service.extraireIdentifiant(id)).isEqualTo(id);
        assertThat(service.extraireIdentifiant(
                "https://calendar.google.com/calendar/embed?src=demo%40group.calendar.google.com")).isEqualTo(id);
        assertThat(service.extraireIdentifiant(
                "https://calendar.google.com/calendar/u/0?cid=" + cid)).isEqualTo(id);
        assertThat(service.extraireIdentifiant(
                "https://calendar.google.com/calendar/ical/demo%40group.calendar.google.com/public/basic.ics"))
                .isEqualTo(id);
    }
}
