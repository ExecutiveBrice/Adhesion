import { Component, OnInit } from '@angular/core';
import { CalendarOptions, EventInput, EventSourceInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import googleCalendarPlugin from '@fullcalendar/google-calendar';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import frLocale from '@fullcalendar/core/locales/fr';
import { UserService } from '../../_services/user.service';
import { Seance } from '../../models/seance';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-accueil',
  templateUrl: './accueil.component.html',
  styleUrls: ['./accueil.component.css']
})
export class AccueilComponent implements OnInit {
  calendrierCharge = false;

  calendarOptions: CalendarOptions = {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, googleCalendarPlugin],
    initialView: 'timeGridWeek',
    locale: frLocale,
    firstDay: 1,
    weekends: true,
    allDaySlot: false,
    nowIndicator: true,
    height: 'auto',
    slotMinTime: '07:00:00',
    slotMaxTime: '23:00:00',
    expandRows: true,
    eventTimeFormat: {hour: '2-digit', minute: '2-digit', hour12: false},
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridWeek,dayGridMonth'
    },
    buttonText: {
      today: "Aujourd'hui",
      week: 'Semaine',
      month: 'Mois'
    },
    googleCalendarApiKey: environment.googleCalendarApiKey || undefined,
    eventSources: []
  };

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getSeancesDeLaSemaine().subscribe({
      next: seances => {
        this.calendarOptions = {
          ...this.calendarOptions,
          eventSources: this.sourcesDuCalendrier(seances)
        };
        this.calendrierCharge = true;
      },
      error: () => {
        this.calendrierCharge = true;
      }
    });
  }

  private sourcesDuCalendrier(seances: Seance[]): EventSourceInput[] {
    const sources: EventSourceInput[] = [seances.map(seance => this.toEvent(seance))];
    if (environment.googleCalendarApiKey && environment.googleCalendarId) {
      sources.push({googleCalendarId: environment.googleCalendarId});
    }
    return sources;
  }

  private toEvent(seance: Seance): EventInput {
    const annulee = seance.etatSeance === 'ANNULEE';
    return {
      id: String(seance.id),
      title: seance.activite.nom,
      start: seance.debut,
      end: seance.fin,
      backgroundColor: annulee ? '#a12828' : '#527c65',
      borderColor: annulee ? '#7d1f1f' : '#3e604d',
      classNames: annulee ? ['seance-annulee'] : [],
      extendedProps: {
        salle: seance.activite.salle,
        etat: seance.etatSeance,
        commentaire: seance.commentaire
      }
    };
  }
}
