import { Component, DestroyRef, OnInit, TemplateRef, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { catchError, forkJoin, of, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TribuService } from '../../_services/tribu.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { AgendaGoogleConfiguration } from '../../models';
import { CalendrierGoogle, SeanceCalendrier } from '../../models/seance';

type Categorie = 'adherent' | 'autre' | 'asso';

interface EvenementAgenda {
  id: string;
  titre: string;
  categorie: Categorie;
  debut: string;
  fin: string;
  journeeEntiere: boolean;
  lieu?: string | null;
  adresse?: string | null;
  commentaire?: string | null;
  lien?: string | null;
  agenda?: string;
  etat?: SeanceCalendrier['etatSeance'];
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
  imports: [DatePipe],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css'
})
export class AgendaComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly activites = inject(ActiviteService);
  private readonly params = inject(ParamService);
  private readonly tribus = inject(TribuService);
  private readonly storage = inject(TokenStorageService);
  private readonly modales = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);
  private requete?: Subscription;
  private adherentId?: number;
  private agendas: AgendaGoogleConfiguration[] = [];
  private erreursConfiguration: string[] = [];

  mois = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  jours: JourAgenda[] = [];
  chargement = true;
  erreurs: string[] = [];
  nombreEvenements = 0;
  evenementSelectionne: EvenementAgenda | null = null;
  readonly joursSemaine = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];
  readonly categories: { id: Categorie; nom: string }[] = [
    { id: 'adherent', nom: 'Mes activités' },
    { id: 'autre', nom: 'Autres activités' },
    { id: 'asso', nom: 'Association' }
  ];

  ngOnInit(): void {
    forkJoin({
      tribu: this.tribus.getConnected().pipe(catchError(() => {
        this.erreursConfiguration.push('Impossible d’identifier vos activités.');
        return of(null);
      })),
      agendas: this.params.getAgendasGoogle().pipe(catchError(() => {
        this.erreursConfiguration.push('Impossible de charger les agendas de l’association.');
        return of([] as AgendaGoogleConfiguration[]);
      }))
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(({ tribu, agendas }) => {
      this.adherentId = tribu?.adherents.find(a => a.user?.id === this.storage.getUser().id)?.id;
      if (tribu && this.adherentId == null) this.erreursConfiguration.push('Impossible d’identifier vos activités.');
      this.agendas = agendas.filter(a => a.isVisisbleApp);
      this.chargerMois();
    });
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
        const evenements: EvenementAgenda[] = [
          ...Array.from(toutes.values(), s => ({
            id: `seance-${s.id}`, titre: s.activiteNom,
            categorie: ids.has(s.id) ? 'adherent' as const : 'autre' as const,
            debut: s.debut, fin: s.fin, journeeEntiere: false, lieu: s.salle,
            adresse: s.adresseSalle, commentaire: s.commentaire, lien: s.lien, etat: s.etatSeance
          })),
          ...google.evenements.map(e => ({
            ...e, id: `google-${e.agendaSource}-${e.id}-${e.debut}`, categorie: 'asso' as const,
            agenda: this.agendas.find(a => a.source === e.agendaSource)?.nom || e.agenda
          }))
        ];
        this.construireMois(evenements);
        this.chargement = false;
      });
  }

  ouvrirEvenement(evenement: EvenementAgenda, contenu: TemplateRef<unknown>): void {
    this.evenementSelectionne = evenement;
    this.modales.open(contenu, { centered: true, scrollable: true, ariaLabelledBy: 'agenda-detail-titre' });
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

  libelleAccessible(e: EvenementAgenda): string {
    return `${e.titre}, ${this.categorieLibelle(e.categorie)}, ${this.horaire(e)}${e.etat ? ', ' + this.etatLibelle(e.etat) : ''}`;
  }

  dernierJour(e: EvenementAgenda): string {
    if (!e.journeeEntiere) return e.fin;
    const date = new Date(`${e.fin.substring(0, 10)}T12:00:00`);
    date.setDate(date.getDate() - 1);
    return this.dateIso(date);
  }

  lienActivite(e: EvenementAgenda): string | null {
    const lien = e.lien?.trim();
    if (!lien) return null;
    if (/^https?:\/\//i.test(lien)) return lien;
    return /^[\w.-]+\.[a-z]{2,}(?:[/:?#]|$)/i.test(lien) ? `https://${lien}` : null;
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
