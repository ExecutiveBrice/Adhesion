import { ChangeDetectorRef, Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { EvenementModalService } from '../../_services/evenement-modal.service';
import { Evenement, evenementSeance, evenementGoogle } from '../../models/evenement';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of } from 'rxjs';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { AgendaGoogleConfiguration } from 'src/app/models';
import { EvenementGoogleAgenda, PresencePrevue, SeanceCalendrier } from 'src/app/models/seance';
import { faCalendarPlus, faCheck, faChevronLeft, faChevronRight, faFileArrowDown, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NgClass, DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { PresencePrevueService } from '../../_services/presence-prevue.service';
import { ResponsabiliteSeance } from '../../models/responsabiliteSeance';
import { ResponsabilitesSeanceComponent } from '../responsabilites-seance/responsabilites-seance.component';

type EvenementCalendrier = Evenement;

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
    styleUrls: ['../../page/login/login.component.css', './calendrier.component.css'],
    imports: [NgClass, FaIconComponent, DatePipe, ResponsabilitesSeanceComponent]
})
export class CalendrierComponent implements OnInit, OnChanges {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private paramService = inject(ParamService);
  private activiteService = inject(ActiviteService);
  private readonly presencePrevueService = inject(PresencePrevueService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modales = inject(EvenementModalService);

  @Input() tribuUuid?: string;
  @Input() adherentId?: number;
  @Input() joursVisibles?: number;
  @Input() affichage: 'calendrier' | 'liste' = 'calendrier';
  @Input() presencesPrevues = false;

  evenementsAVenir: EvenementCalendrier[] = [];
  evenementSelectionne: EvenementCalendrier | null = null;
  nombreEvenementsAffiches = 12;

  calendrier: JourCalendrier[] = [];
  jourSelectionne: JourCalendrier | null = null;
  popupJourOuverte = false;
  chargementCalendrier = false;
  erreurCalendrier = '';
  agendasGoogle: AgendaGoogleConfiguration[] = [];
  googleAgendaIds: string[] = [];
  googleAgendaErreur = '';
  exportCalendrierEnCours = false;
  erreurExportCalendrier = '';
  dateAffichee = this.aujourdhui();
  readonly joursSemaine = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  faXmark = faXmark;
  faCheck = faCheck;
  faTriangleExclamation = faTriangleExclamation;
  faChevronLeft = faChevronLeft;
  faChevronRight = faChevronRight;
  faCalendarPlus = faCalendarPlus;
  faFileArrowDown = faFileArrowDown;

  ngOnInit(): void { this.chargerConfigurationAgendas(); }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['tribuUuid'] && !changes['tribuUuid'].firstChange)
      || (changes['adherentId'] && !changes['adherentId'].firstChange)
      || (changes['joursVisibles'] && !changes['joursVisibles'].firstChange)
      || (changes['affichage'] && !changes['affichage'].firstChange)
      || (changes['presencesPrevues'] && !changes['presencesPrevues'].firstChange)) this.chargerCalendrier();
  }

  chargerConfigurationAgendas(): void {
    this.paramService.getAgendasGoogle().subscribe({
      next: agendas => {
        this.agendasGoogle = agendas.filter(agenda => agenda.isVisisbleApp);
        this.googleAgendaIds = this.agendasGoogle.map(agenda => agenda.source);
        this.chargerCalendrier();
      },
      error: () => {
        this.agendasGoogle = [];
        this.googleAgendaIds = [];
        this.chargerCalendrier();
        this.googleAgendaErreur = "La configuration des agendas Google n'est pas disponible pour le moment.";
      }
    });
  }

  get libelleSemaine(): string {
    const debut = this.premierJourSemaineCourante();
    const fin = new Date(debut);
    fin.setDate(fin.getDate() + 6);
    const format = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
    return `Semaine du ${format.format(debut)} au ${format.format(fin)}`;
  }
  get libelleAujourdhui(): string {
    return new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(this.dateAffichee);
  }
  get libelleJourSelectionne(): string {
    return this.jourSelectionne ? new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(this.jourSelectionne.date) : '';
  }
  selectionnerJour(jour: JourCalendrier): void { this.jourSelectionne = jour; this.popupJourOuverte = true; }
  fermerPopupJour(): void { this.popupJourOuverte = false; }

  ouvrirEvenement(evenement: EvenementCalendrier): void {
    this.evenementSelectionne = evenement;
    const modale = this.modales.ouvrir(evenement, this.adherentId);
    modale.componentInstance.presenceChange.subscribe(() => this.changeDetectorRef.markForCheck());
    const fermer = () => {
      if (this.destroyRef.destroyed) return;
      if (!evenement.presences && !evenement.chargementPresences && !evenement.erreurPresence) this.chargerPresencesPrevues(evenement);
      this.changeDetectorRef.markForCheck();
    };
    modale.result.then(fermer, fermer);
  }

  mesResponsabilites(evenement: EvenementCalendrier): ResponsabiliteSeance[] {
    return this.adherentId == null ? [] : (evenement.responsabilites ?? [])
      .filter(responsabilite => responsabilite.adherent.id === this.adherentId);
  }

  peutRenseignerPresence(evenement: EvenementCalendrier): boolean {
    return this.presencesPrevues && this.adherentId != null && evenement.source === 'SEANCE'
      && evenement.sectionType === 'COMPETITION'
      && (evenement.etatSeance === 'PROGRAMMEE' || evenement.etatSeance === 'MODIFIEE')
      && new Date(evenement.debut).getTime() > Date.now();
  }

  chargerPresencesPrevues(evenement: EvenementCalendrier): void {
    if (!this.peutRenseignerPresence(evenement) || evenement.seanceId == null || evenement.chargementPresences) return;
    evenement.chargementPresences = true;
    evenement.erreurPresence = '';
    this.modales.actualiser(evenement);
    this.presencePrevueService.getPresences(evenement.seanceId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: presences => {
        evenement.presences = presences;
        evenement.chargementPresences = false;
        this.modales.actualiser(evenement);
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        evenement.erreurPresence = error.error?.detail || 'Impossible de charger les présences prévues.';
        evenement.chargementPresences = false;
        this.modales.actualiser(evenement);
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  maPresence(evenement: EvenementCalendrier): PresencePrevue | undefined {
    return evenement.presences?.find(presence => presence.adherentId === this.adherentId);
  }

  nombrePresences(evenement: EvenementCalendrier, valeur: boolean): number {
    // Compte les réponses de tous les inscrits à cette séance, quel que soit l'adhérent connecté.
    return evenement.presences?.filter(presence => presence.presencePrevue === valeur).length || 0;
  }

  definirPresencePrevue(evenement: EvenementCalendrier, valeur: boolean): void {
    const presence = this.maPresence(evenement);
    if (!this.peutRenseignerPresence(evenement) || evenement.seanceId == null || evenement.chargementPresences
      || evenement.enregistrementPresence || !presence || presence.presencePrevue === valeur) return;
    evenement.enregistrementPresence = true;
    evenement.erreurPresence = '';
    this.modales.actualiser(evenement);
    this.presencePrevueService.updatePresence(evenement.seanceId, valeur).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: miseAJour => {
        evenement.presences = evenement.presences?.map(element => element.id === miseAJour.id ? miseAJour : element);
        evenement.enregistrementPresence = false;
        this.chargerPresencesPrevues(evenement);
        this.changeDetectorRef.detectChanges();
      },
      error: error => {
        evenement.erreurPresence = error.error?.detail || 'Impossible d’enregistrer votre présence prévue.';
        evenement.enregistrementPresence = false;
        this.modales.actualiser(evenement);
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  changerPeriode(nombreDeJours: number): void {
    const nouvelleDate = new Date(this.dateAffichee);
    nouvelleDate.setDate(nouvelleDate.getDate() + nombreDeJours);
    if (this.joursVisibles != null
      && (nouvelleDate < this.aujourdhui() || nouvelleDate > this.finVisibilite())) return;
    this.dateAffichee = nouvelleDate;
    this.popupJourOuverte = false;
    this.chargerCalendrier();
  }

  chargerCalendrier(): void {
    const debut = this.affichage === 'liste' ? this.aujourdhui() : this.premierJourSemaineCourante();
    const fin = new Date(debut);
    if (this.affichage === 'liste') fin.setTime(this.finVisibilite().getTime());
    else fin.setDate(fin.getDate() + 6);
    this.chargementCalendrier = true; this.erreurCalendrier = ''; this.googleAgendaErreur = '';
    const dateDebut = this.dateIso(debut); const dateFin = this.dateIso(fin);
    const seances$ = this.chargerSeances(dateDebut, dateFin).pipe(catchError(() => {
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
      if (this.affichage === 'liste' && this.presencesPrevues) {
        this.evenementsAVenir.filter(evenement => this.peutRenseignerPresence(evenement))
          .forEach(evenement => this.chargerPresencesPrevues(evenement));
      }
    });
  }

  heure(dateHeure: string): string { return dateHeure?.substring(11, 16) || ''; }
  etatLibelle(etat?: SeanceCalendrier['etatSeance']): string { return etat === 'ANNULEE' ? 'Annulée' : etat === 'REALISEE' ? 'Réalisée' : etat === 'MODIFIEE' ? 'Modifiée' : 'Programmée'; }
  iconeEtat(etat?: SeanceCalendrier['etatSeance']) { return etat === 'ANNULEE' ? this.faXmark : etat === 'REALISEE' ? this.faCheck : this.faTriangleExclamation; }
  classeEtat(etat?: SeanceCalendrier['etatSeance']): string { return `etat-${(etat || 'PROGRAMMEE').toLowerCase()}`; }
  heureEvenement(evenement: EvenementCalendrier): string { return evenement.journeeEntiere ? 'Journée' : this.heure(evenement.debut); }
  classeEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'GOOGLE' ? 'source-google' : this.classeEtat(evenement.etatSeance); }
  couleurEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'SEANCE' ? evenement.couleurSection || '#5CBBAF' : this.agendasGoogle.find(a => a.source === evenement.agendaSource)?.couleur || '#D29438'; }
  nomAgenda(evenement: EvenementCalendrier): string { return this.agendasGoogle.find(a => a.source === evenement.agendaSource)?.nom || evenement.agenda || 'Agenda Google'; }
  sourceEvenement(evenement: EvenementCalendrier): string { return evenement.source === 'GOOGLE' ? `Google · ${this.nomAgenda(evenement)}` : this.etatLibelle(evenement.etatSeance); }
  lienActivite(evenement: EvenementCalendrier): string | null { return evenement.source === 'SEANCE' ? this.lienUrl(evenement.lien) : null; }
  lienAdresseSalle(evenement: EvenementCalendrier): string | null { return evenement.adresseSalle?.trim() ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(evenement.adresseSalle.trim())}` : null; }

  telechargerSeancesGoogle(): void {
    const debut = this.aujourdhui();
    const fin = new Date(debut);
    fin.setTime(this.finVisibilite().getTime());
    this.exportCalendrierEnCours = true;
    this.erreurExportCalendrier = '';

    this.chargerSeances(this.dateIso(debut), this.dateIso(fin)).subscribe({
      next: seances => {
        const contenu = this.creerFichierIcs(seances.filter(seance => seance.etatSeance !== 'ANNULEE'));
        const fichier = new Blob([contenu], { type: 'text/calendar;charset=utf-8' });
        const lien = URL.createObjectURL(fichier);
        const telechargement = document.createElement('a');
        telechargement.href = lien;
        telechargement.download = 'seances.ics';
        telechargement.click();
        URL.revokeObjectURL(lien);
        this.exportCalendrierEnCours = false;
      },
      error: () => {
        this.erreurExportCalendrier = "Impossible de préparer le fichier d'import Google Agenda pour le moment.";
        this.exportCalendrierEnCours = false;
      }
    });
  }

  private chargerSeances(dateDebut: string, dateFin: string) {
    if (this.joursVisibles != null) {
      const aujourdHui = this.dateIso(this.aujourdhui());
      const limite = this.dateIso(this.finVisibilite());
      if (dateDebut < aujourdHui) dateDebut = aujourdHui;
      if (dateFin > limite) dateFin = limite;
      if (dateDebut > dateFin) return of([] as SeanceCalendrier[]);
    }
    return this.adherentId != null
      ? this.activiteService.getCalendrierAdherent(dateDebut, dateFin, this.adherentId)
      : this.activiteService.getCalendrier(dateDebut, dateFin, this.tribuUuid);
  }

  private finVisibilite(): Date {
    const fin = this.aujourdhui();
    if (this.joursVisibles != null) fin.setDate(fin.getDate() + this.joursVisibles - 1);
    else fin.setFullYear(fin.getFullYear() + 1);
    return fin;
  }

  lienGoogleAgenda(evenement: EvenementCalendrier): string | null {
    if (evenement.source !== 'SEANCE' || evenement.etatSeance === 'ANNULEE') {
      return null;
    }

    const description = [
      this.texteCalendrier(evenement.commentaire),
      this.lienActivite(evenement)
    ].filter(Boolean).join('\n\n');
    const lieu = evenement.adresseSalle?.trim() || evenement.lieu?.trim() || '';
    const params = new URLSearchParams({
      action: 'TEMPLATE',
      text: evenement.titre,
      dates: `${this.dateGoogleAgenda(evenement.debut)}/${this.dateGoogleAgenda(evenement.fin)}`,
      ctz: 'Europe/Paris'
    });
    if (description) params.set('details', description);
    if (lieu) params.set('location', lieu);

    return `https://calendar.google.com/calendar/render?${params.toString()}`;
  }

  private dateGoogleAgenda(dateHeure: string): string {
    return dateHeure.replace(/[-:]/g, '').replace(/\.\d+/, '');
  }

  private texteCalendrier(texte?: string | null): string {
    return (texte || '')
      .replace(/<[^>]*>/g, ' ')
      .replace(/&nbsp;/gi, ' ')
      .replace(/&amp;/gi, '&')
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>')
      .replace(/&quot;/gi, '"')
      .replace(/&#39;/gi, "'")
      .replace(/\s+/g, ' ')
      .trim();
  }

  private lienUrl(lien?: string | null): string | null {
    if (!lien?.trim()) return null;
    const lienNettoye = lien.trim();
    return /^https?:\/\//i.test(lienNettoye) ? lienNettoye : `https://${lienNettoye}`;
  }

  private creerFichierIcs(seances: SeanceCalendrier[]): string {
    const maintenant = new Date().toISOString()
      .replace(/\.\d{3}Z$/, 'Z')
      .replace(/[-:]/g, '');
    const evenements = seances.map(seance => {
      const description = [
        this.texteCalendrier(seance.commentaire),
        this.lienUrl(seance.lien)
      ].filter(Boolean).join('\\n\\n');
      return [
        'BEGIN:VEVENT',
        `UID:seance-${seance.id}@adhesion`,
        `DTSTAMP:${maintenant}`,
        `DTSTART;TZID=Europe/Paris:${this.dateGoogleAgenda(seance.debut)}`,
        `DTEND;TZID=Europe/Paris:${this.dateGoogleAgenda(seance.fin)}`,
        `SUMMARY:${this.echapperIcs(seance.activiteNom)}`,
        description ? `DESCRIPTION:${this.echapperIcs(description)}` : '',
        seance.adresseSalle || seance.salle ? `LOCATION:${this.echapperIcs(seance.adresseSalle || seance.salle)}` : '',
        'END:VEVENT'
      ].filter(Boolean).join('\r\n');
    });
    return [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//Séances//FR',
      'CALSCALE:GREGORIAN',
      'X-WR-CALNAME:Séances',
      'X-WR-TIMEZONE:Europe/Paris',
      ...evenements,
      'END:VCALENDAR',
      ''
    ].join('\r\n');
  }

  private echapperIcs(valeur: string): string {
    return valeur
      .replace(/\\/g, '\\\\')
      .replace(/\r?\n/g, '\\n')
      .replace(/;/g, '\\;')
      .replace(/,/g, '\\,');
  }

  private premierJourSemaineCourante(): Date {
    const debut = new Date(this.dateAffichee);
    debut.setHours(0, 0, 0, 0);
    debut.setDate(debut.getDate() - (debut.getDay() + 6) % 7);
    return debut;
  }
  private construireCalendrier(debut: Date, seances: SeanceCalendrier[], google: EvenementGoogleAgenda[]): void {
    const parJour = new Map<string, EvenementCalendrier[]>();
    const evenements: EvenementCalendrier[] = [...seances.map(s => evenementSeance(s, this.adherentId != null)), ...google.map(e => evenementGoogle(e, this.agendasGoogle))];
    const maintenant = new Date();
    this.evenementsAVenir = evenements
      .filter(e => e.etatSeance !== 'REALISEE' && (e.journeeEntiere
        ? e.debut.substring(0, 10) >= this.dateIso(maintenant)
        : new Date(e.debut).getTime() >= maintenant.getTime()))
      .sort((a, b) => a.debut.localeCompare(b.debut) || a.titre.localeCompare(b.titre, 'fr'));
    this.nombreEvenementsAffiches = 12;
    if (this.affichage === 'liste') return;
    evenements.forEach(e => {
      const date = new Date(`${e.debut.substring(0, 10)}T12:00:00`); const dernier = new Date(`${e.fin.substring(0, 10)}T12:00:00`);
      if (e.journeeEntiere) dernier.setDate(dernier.getDate() - 1);
      do { const iso = this.dateIso(date); parJour.set(iso, [...(parJour.get(iso) || []), e]); date.setDate(date.getDate() + 1); } while (date <= dernier);
    });
    const aujourdHui = this.dateIso(new Date());
    this.calendrier = Array.from({ length: 7 }, (_, index) => {
      const date = new Date(debut); date.setDate(debut.getDate() + index); const iso = this.dateIso(date);
      return { date, iso, numero: date.getDate(), dansLeMois: true, aujourdhui: iso === aujourdHui, evenements: (parJour.get(iso) || []).sort((a, b) => a.debut.localeCompare(b.debut)) };
    });
    const ancienne = this.jourSelectionne?.iso;
    const dateAffichee = this.dateIso(this.dateAffichee);
    this.jourSelectionne = this.calendrier.find(j => j.iso === dateAffichee) || this.calendrier.find(j => j.iso === ancienne) || this.calendrier.find(j => j.iso === aujourdHui) || this.calendrier.find(j => j.evenements.length > 0) || this.calendrier[0] || null;
  }
  private aujourdhui(): Date { const date = new Date(); return new Date(date.getFullYear(), date.getMonth(), date.getDate()); }
  private dateIso(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
}
