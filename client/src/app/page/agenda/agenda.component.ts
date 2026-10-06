import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTriangleExclamation, faUser, faXmark } from '@fortawesome/free-solid-svg-icons';
import { EvenementModalService } from '../../_services/evenement-modal.service';
import { Evenement, evenementSeance, evenementGoogle } from '../../models/evenement';
import { catchError, forkJoin, of, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TribuService } from '../../_services/tribu.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { AgendaPreferences, AgendaPreferencesService } from '../../_services/agenda-preferences.service';
import { AgendaGoogleConfiguration, SectionConfiguration } from '../../models';
import { CalendrierGoogle, SeanceCalendrier } from '../../models/seance';
import { ResponsabiliteSeance } from '../../models/responsabiliteSeance';
import { ResponsabilitesSeanceComponent } from '../../template/responsabilites-seance/responsabilites-seance.component';

type Categorie = 'adherent' | 'autre' | 'asso';

interface EvenementAgenda extends Evenement {
  categorie: Categorie;
  agendaGoogleId?: number;
  etat?: SeanceCalendrier['etatSeance'];
  couleur: string;
}

interface JourAgenda {
  iso: string;
  numero: number;
  dansLeMois: boolean;
  aujourdhui: boolean;
  evenements: EvenementAgenda[];
}

@Component({
  selector: 'app-agenda',
  imports: [DatePipe, FaIconComponent, ResponsabilitesSeanceComponent],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css'
})
export class AgendaComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly activites = inject(ActiviteService);
  private readonly params = inject(ParamService);
  private readonly tribus = inject(TribuService);
  private readonly storage = inject(TokenStorageService);
  private readonly modales = inject(EvenementModalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly preferencesService = inject(AgendaPreferencesService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);
  private requete?: Subscription;
  private adherentId?: number;
  agendas: AgendaGoogleConfiguration[] = [];
  sections: SectionConfiguration[] = [];
  private evenements: EvenementAgenda[] = [];
  preferences: AgendaPreferences = { sectionsMasquees: [], agendasGoogleMasques: [] };
  preferencesChargees = false;
  chargementPreferences = false;
  enregistrementPreferences = false;
  erreurPreferences = '';
  messagePreferences = '';
  private erreursConfiguration: string[] = [];

  mois = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  jours: JourAgenda[] = [];
  chargement = true;
  erreurs: string[] = [];
  nombreEvenements = 0;
  evenementSelectionne: EvenementAgenda | null = null;
  readonly faTriangleExclamation = faTriangleExclamation;
  readonly faXmark = faXmark;
  readonly faUser = faUser;
  readonly joursSemaine = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];
  readonly categories: { id: Categorie; nom: string }[] = [
    { id: 'adherent', nom: 'Mes activités' },
    { id: 'autre', nom: 'Autres activités' },
    { id: 'asso', nom: 'ALOD' }
  ];

  ngOnInit(): void {
    this.chargementPreferences = true;
    forkJoin({
      tribu: this.tribus.getConnected().pipe(catchError(() => {
        this.erreursConfiguration.push('Impossible d’identifier vos activités.');
        return of(null);
      })),
      agendas: this.params.getAgendasGoogle().pipe(catchError(() => {
        this.erreursConfiguration.push('Impossible de charger les agendas de l’association.');
        return of([] as AgendaGoogleConfiguration[]);
      })),
      sections: this.params.getSections().pipe(catchError(() => {
        this.erreursConfiguration.push('Impossible de charger la liste des sections.');
        return of([] as SectionConfiguration[]);
      })),
      preferences: this.preferencesService.get().pipe(catchError(() => {
        this.erreurPreferences = 'Impossible de charger votre sélection d’agendas.';
        return of(null);
      }))
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ tribu, agendas, sections, preferences }) => {
      this.adherentId = tribu?.adherents.find(a => a.user?.id === this.storage.getUser().id)?.id;
      if (tribu && this.adherentId == null) this.erreursConfiguration.push('Impossible d’identifier vos activités.');
      this.agendas = agendas.filter(a => a.isVisisbleApp);
      this.sections = sections;
      this.chargementPreferences = false;
      this.preferencesChargees = preferences != null && this.adherentId != null;
      if (preferences) this.preferences = preferences;
      this.chargerMois();
    });
  }

  get filtresDesactives(): boolean {
    return !this.preferencesChargees || this.enregistrementPreferences;
  }

  sectionVisible(id: number): boolean { return !this.preferences.sectionsMasquees.includes(id); }
  agendaVisible(id: number): boolean { return !this.preferences.agendasGoogleMasques.includes(id); }

  selectionnerSection(id: number, visible: boolean): void {
    this.modifierSelection('sectionsMasquees', id, visible);
  }

  selectionnerAgenda(id: number, visible: boolean): void {
    this.modifierSelection('agendasGoogleMasques', id, visible);
  }

  private modifierSelection(cle: keyof AgendaPreferences, id: number, visible: boolean): void {
    if (this.filtresDesactives) return;
    const ids = this.preferences[cle].filter(valeur => valeur !== id);
    this.preferences = { ...this.preferences, [cle]: visible ? ids : [...ids, id] };
    this.appliquerSelection();
    this.enregistrerSelection();
  }

  enregistrerSelection(): void {
    if (this.filtresDesactives) return;
    this.enregistrementPreferences = true;
    this.changeDetectorRef.markForCheck();
    this.erreurPreferences = '';
    this.messagePreferences = '';
    this.preferencesService.update(this.preferences).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: preferences => {
        this.preferences = preferences;
        this.enregistrementPreferences = false;
        this.messagePreferences = 'Sélection enregistrée dans votre profil.';
        this.appliquerSelection();
      },
      error: () => {
        this.enregistrementPreferences = false;
        this.erreurPreferences = 'Votre sélection n’a pas pu être enregistrée. Réessayez pour la conserver dans votre profil.';
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  rechargerSelection(): void {
    if (this.chargementPreferences || this.adherentId == null) return;
    this.chargementPreferences = true;
    this.changeDetectorRef.markForCheck();
    this.preferencesService.get().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: preferences => {
        this.preferences = preferences;
        this.preferencesChargees = true;
        this.chargementPreferences = false;
        this.erreurPreferences = '';
        this.appliquerSelection();
      },
      error: () => {
        this.chargementPreferences = false;
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  private appliquerSelection(): void {
    this.construireMois(this.evenements.filter(e => e.categorie === 'asso'
      ? e.agendaGoogleId == null || this.agendaVisible(e.agendaGoogleId)
      : e.sectionId == null || this.sectionVisible(e.sectionId)));
    this.changeDetectorRef.markForCheck();
  }

  get titreMois(): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.mois);
  }

  changerMois(delta: number): void {
    this.mois = new Date(this.mois.getFullYear(), this.mois.getMonth() + delta, 1);
    this.chargerMois();
  }

  revenirAuMoisCourant(): void {
    const maintenant = new Date();
    this.mois = new Date(maintenant.getFullYear(), maintenant.getMonth(), 1);
    this.chargerMois();
  }

  chargerMois(): void {
    this.requete?.unsubscribe();
    this.chargement = true;
    this.erreurs = [...this.erreursConfiguration];
    this.jours = [];
    this.nombreEvenements = 0;
    const debut = this.dateIso(this.mois);
    const fin = this.dateIso(new Date(this.mois.getFullYear(), this.mois.getMonth() + 1, 0));
    const seances$ = this.activites.getCalendrier(debut, fin).pipe(catchError(() => {
      this.erreurs.push('Impossible de charger les séances des activités.');
      return of([] as SeanceCalendrier[]);
    }));
    const personnelles$ = this.adherentId == null ? of(null) : this.activites
      .getCalendrierAdherent(debut, fin, this.adherentId).pipe(catchError(() => {
        this.erreurs.push('Impossible de distinguer vos activités des autres activités.');
        return of(null);
      }));
    const google$ = this.agendas.length ? this.activites
      .getCalendrierGoogle(debut, fin, this.agendas.map(a => a.source)).pipe(catchError(() => {
        this.erreurs.push('Impossible de charger les évènements de l’association.');
        return of({ evenements: [], erreurs: [] } as CalendrierGoogle);
      })) : of({ evenements: [], erreurs: [] } as CalendrierGoogle);
    this.requete = forkJoin({ seances: seances$, personnelles: personnelles$, google: google$ })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ seances, personnelles, google }) => {
        this.erreurs.push(...google.erreurs);
        const ids = new Set(personnelles?.map(s => s.id));
        // Keep personal sessions even if the general calendar failed to load.
        const toutes = new Map([...seances, ...(personnelles ?? [])].map(s => [s.id, s]));
        this.evenements = [
          ...Array.from(toutes.values(), s => ({
            ...evenementSeance(s, ids.has(s.id)),
            categorie: ids.has(s.id) ? 'adherent' as const : 'autre' as const,
            etat: s.etatSeance,
            couleur: s.couleurSection || this.sections.find(section => section.id === s.sectionId)?.couleur || '#5CBBAF',
          })),
          ...google.evenements.map(e => ({
            ...evenementGoogle(e, this.agendas), categorie: 'asso' as const,
            couleur: this.agendas.find(a => a.source === e.agendaSource)?.couleur || '#D29438',
            agendaGoogleId: this.agendas.find(a => a.source === e.agendaSource)?.id
          }))
        ];
        // Also offer sections returned by the calendar if the configuration request failed.
        toutes.forEach(s => {
          if (s.sectionId != null && !this.sections.some(section => section.id === s.sectionId)) {
            this.sections = [...this.sections, { id: s.sectionId, nom: s.sectionNom || 'Section',
              type: s.sectionType === 'COMPETITION' ? 'COMPETITION' : 'NON_COMPETITIVE', couleur: s.couleurSection || undefined }];
          }
        });
        this.sections.sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.appliquerSelection();
        this.chargement = false;
      });
  }

  ouvrirEvenement(evenement: EvenementAgenda): void {
    this.evenementSelectionne = evenement;
    this.modales.ouvrir(evenement, this.adherentId);
  }

  categorieLibelle(categorie: Categorie): string {
    return this.categories.find(c => c.id === categorie)!.nom;
  }

  etatLibelle(etat: SeanceCalendrier['etatSeance']): string {
    return { PROGRAMMEE: 'Programmée', ANNULEE: 'Annulée', REALISEE: 'Réalisée', MODIFIEE: 'Modifiée' }[etat];
  }

  horaire(e: EvenementAgenda): string {
    return e.journeeEntiere ? 'Toute la journée' : `${e.debut.substring(11, 16)} – ${e.fin.substring(11, 16)}`;
  }

  mesResponsabilites(e: EvenementAgenda): ResponsabiliteSeance[] {
    return this.adherentId == null ? [] : (e.responsabilites ?? [])
      .filter(responsabilite => responsabilite.adherent.id === this.adherentId);
  }

  libelleAccessible(e: EvenementAgenda): string {
    const responsabilites = this.mesResponsabilites(e).map(r =>
      `${r.tache.nom} : ${r.adherent.prenom} ${r.adherent.nom}`).join(', ');
    return `${e.titre}, ${this.categorieLibelle(e.categorie)}, ${this.horaire(e)}${e.etat ? ', ' + this.etatLibelle(e.etat) : ''}${responsabilites ? ', ' + responsabilites : ''}`;
  }

  private construireMois(evenements: EvenementAgenda[]): void {
    const debut = new Date(this.mois);
    debut.setDate(debut.getDate() - (debut.getDay() + 6) % 7);
    const dernier = new Date(this.mois.getFullYear(), this.mois.getMonth() + 1, 0);
    const nombreJours = Math.ceil(((this.mois.getDay() + 6) % 7 + dernier.getDate()) / 7) * 7;
    const aujourdHui = this.dateIso(new Date());
    const tries = [...evenements].sort((a, b) => a.debut.localeCompare(b.debut) || a.titre.localeCompare(b.titre, 'fr'));
    this.nombreEvenements = evenements.length;
    this.jours = Array.from({ length: nombreJours }, (_, index) => {
      const date = new Date(debut);
      date.setDate(debut.getDate() + index);
      const iso = this.dateIso(date);
      const dansLeMois = date.getMonth() === this.mois.getMonth();
      return {
        iso, numero: date.getDate(), dansLeMois, aujourdhui: iso === aujourdHui,
        evenements: dansLeMois ? tries.filter(e => {
          // End dates are exclusive, including timed events ending at midnight.
          const fin = e.fin.length === 10 ? `${e.fin}T00:00:00` : e.fin;
          return e.debut.substring(0, 10) <= iso && fin > `${iso}T00:00:00`;
        }) : []
      };
    });
  }

  private dateIso(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
