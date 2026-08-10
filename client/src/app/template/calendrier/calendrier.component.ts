import { Component, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { AgendaGoogleConfiguration } from 'src/app/models';
import { EvenementGoogleAgenda, SeanceCalendrier } from 'src/app/models/seance';
import { faCheck, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NgClass, SlicePipe, DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

interface EvenementCalendrier {
  id: string;
  titre: string;
  lieu: string | null;
  adresseSalle?: string | null;
  commentaire?: string | null;
  debut: string;
  fin: string;
  source: 'SEANCE' | 'GOOGLE';
  journeeEntiere: boolean;
  agenda?: string;
  agendaSource?: string;
  couleurSalle?: string | null;
  lien?: string | null;
  etatSeance?: SeanceCalendrier['etatSeance'];
}

interface JourCalendrier {
  date: Date;
  iso: string;
  numero: number;
  dansLeMois: boolean;
  aujourdhui: boolean;
  evenements: EvenementCalendrier[];
}

@Component({
    selector: 'app-calendrier',
    templateUrl: './calendrier.component.html',
    styleUrls: ['../../page/login/login.component.css'],
    imports: [NgClass, FaIconComponent, SlicePipe, DatePipe]
})
export class CalendrierComponent implements OnInit, OnChanges {
  private paramService = inject(ParamService);
  private activiteService = inject(ActiviteService);

  @Input() tribuUuid?: string;

  calendrier: JourCalendrier[] = [];
  jourSelectionne: JourCalendrier | null = null;
  popupJourOuverte = false;
  moisAffiche = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  chargementCalendrier = false;
  erreurCalendrier = '';
  agendasGoogle: AgendaGoogleConfiguration[] = [];
  googleAgendaIds: string[] = [];
  googleAgendaErreur = '';
  readonly joursSemaine = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  faXmark = faXmark;
  faCheck = faCheck;
  faTriangleExclamation = faTriangleExclamation;

  ngOnInit(): void { this.chargerConfigurationAgendas(); }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tribuUuid'] && !changes['tribuUuid'].firstChange && this.agendasGoogle) this.chargerCalendrier();
  }

  chargerConfigurationAgendas(): void {
    this.paramService.getAgendasGoogle().subscribe({
      next: agendas => {
        this.agendasGoogle = agendas;
        this.googleAgendaIds = agendas.map(agenda => agenda.source);
        this.chargerCalendrier();
      },
      error: () => {
        this.agendasGoogle = [];
        this.googleAgendaIds = [];
        this.googleAgendaErreur = "La configuration des agendas Google n'est pas disponible pour le moment.";
        this.chargerCalendrier();
      }
    });
  }

  get libelleMois(): string { return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.moisAffiche); }
  get libelleJourSelectionne(): string {
    return this.jourSelectionne ? new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(this.jourSelectionne.date) : '';
  }
  changerMois(decalage: number): void {
    this.moisAffiche = new Date(this.moisAffiche.getFullYear(), this.moisAffiche.getMonth() + decalage, 1);
    this.chargerCalendrier();
  }
  revenirAujourdhui(): void {
    const maintenant = new Date();
    this.moisAffiche = new Date(maintenant.getFullYear(), maintenant.getMonth(), 1);
    this.chargerCalendrier();
  }
  selectionnerJour(jour: JourCalendrier): void { this.jourSelectionne = jour; this.popupJourOuverte = true; }
  fermerPopupJour(): void { this.popupJourOuverte = false; }

  chargerCalendrier(): void {
    const debut = this.premierJourVisible();
    const fin = new Date(debut); fin.setDate(fin.getDate() + 41);
    this.chargementCalendrier = true; this.erreurCalendrier = ''; this.googleAgendaErreur = '';
    const dateDebut = this.dateIso(debut); const dateFin = this.dateIso(fin);
    const seances$ = this.activiteService.getCalendrier(dateDebut, dateFin, this.tribuUuid).pipe(catchError(() => {
      this.erreurCalendrier = "Le calendrier des séances n'est pas disponible pour le moment.";
      return of([] as SeanceCalendrier[]);
    }));
    const google$ = this.googleAgendaIds.length ? this.activiteService.getCalendrierGoogle(dateDebut, dateFin, this.googleAgendaIds).pipe(catchError(() => {
      this.googleAgendaErreur = "Les agendas Google publics ne sont pas disponibles pour le moment.";
      return of({ evenements: [] as EvenementGoogleAgenda[], erreurs: [] });
    })) : of({ evenements: [] as EvenementGoogleAgenda[], erreurs: [] });
    forkJoin({ seances: seances$, google: google$ }).subscribe(({ seances, google }) => {
      this.googleAgendaErreur ||= google.erreurs.join(' ');
      this.construireCalendrier(debut, seances, google.evenements);
      this.chargementCalendrier = false;
    });
  }

  heure(dateHeure: string): string { return dateHeure?.substring(11, 16) || ''; }
  etatLibelle(etat?: SeanceCalendrier['etatSeance']): string { return etat === 'ANNULEE' ? 'Annulée' : etat === 'REALISEE' ? 'Réalisée' : etat === 'MODIFIEE' ? 'Modifiée' : 'Programmée'; }
  iconeEtat(etat?: SeanceCalendrier['etatSeance']) { return etat === 'ANNULEE' ? this.faXmark : etat === 'REALISEE' ? this.faCheck : this.faTriangleExclamation; }
  classeEtat(etat?: SeanceCalendrier['etatSeance']): string { return `etat-${(etat || 'PROGRAMMEE').toLowerCase()}`; }
  heureEvenement(evenement: EvenementCalendrier): string { return evenement.journeeEntiere ? 'Journée' : this.heure(evenement.debut); }
  classeEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'GOOGLE' ? 'source-google' : this.classeEtat(evenement.etatSeance); }
  couleurEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'SEANCE' ? evenement.couleurSalle || '#5CBBaf' : this.agendasGoogle.find(a => a.source === evenement.agendaSource)?.couleur || '#D29438'; }
  nomAgenda(evenement: EvenementCalendrier): string { return this.agendasGoogle.find(a => a.source === evenement.agendaSource)?.nom || evenement.agenda || 'Agenda Google'; }
  sourceEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'GOOGLE' ? `Google · ${this.nomAgenda(evenement)}` : this.etatLibelle(evenement.etatSeance); }
  lienActivite(evenement: EvenementCalendrier): string | null { if (evenement.source !== 'SEANCE' || !evenement.lien?.trim()) return null; const lien = evenement.lien.trim(); return /^https?:\/\//i.test(lien) ? lien : `https://${lien}`; }
  lienAdresseSalle(evenement: EvenementCalendrier): string | null { return evenement.adresseSalle?.trim() ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(evenement.adresseSalle.trim())}` : null; }

  private premierJourVisible(): Date {
    const premier = new Date(this.moisAffiche.getFullYear(), this.moisAffiche.getMonth(), 1);
    premier.setDate(premier.getDate() - (premier.getDay() + 6) % 7);
    return premier;
  }
  private construireCalendrier(debut: Date, seances: SeanceCalendrier[], google: EvenementGoogleAgenda[]): void {
    const parJour = new Map<string, EvenementCalendrier[]>();
    const evenements: EvenementCalendrier[] = [...seances.map(s => ({ id: `seance-${s.id}`, titre: s.activiteNom, lieu: s.salle, adresseSalle: s.adresseSalle, commentaire: s.commentaire, couleurSalle: s.couleurSalle, lien: s.lien, debut: s.debut, fin: s.fin, source: 'SEANCE' as const, journeeEntiere: false, etatSeance: s.etatSeance })), ...google.map(e => ({ ...e, source: 'GOOGLE' as const }))];
    evenements.forEach(e => {
      const date = new Date(`${e.debut.substring(0, 10)}T12:00:00`); const dernier = new Date(`${e.fin.substring(0, 10)}T12:00:00`);
      if (e.journeeEntiere) dernier.setDate(dernier.getDate() - 1);
      do { const iso = this.dateIso(date); parJour.set(iso, [...(parJour.get(iso) || []), e]); date.setDate(date.getDate() + 1); } while (date <= dernier);
    });
    const aujourdHui = this.dateIso(new Date());
    this.calendrier = Array.from({ length: 42 }, (_, index) => {
      const date = new Date(debut); date.setDate(debut.getDate() + index); const iso = this.dateIso(date);
      return { date, iso, numero: date.getDate(), dansLeMois: date.getMonth() === this.moisAffiche.getMonth(), aujourdhui: iso === aujourdHui, evenements: (parJour.get(iso) || []).sort((a, b) => a.debut.localeCompare(b.debut)) };
    });
    const ancienne = this.jourSelectionne?.iso;
    this.jourSelectionne = this.calendrier.find(j => j.iso === ancienne) || this.calendrier.find(j => j.iso === aujourdHui) || this.calendrier.find(j => j.dansLeMois && j.evenements.length > 0) || this.calendrier.find(j => j.dansLeMois) || null;
  }
  private dateIso(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
}
