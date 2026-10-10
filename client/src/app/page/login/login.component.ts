import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ParamTransmissionService } from 'src/app/_services/transmission.service';
import { faBook, faCheck, faClock, faCloudDownloadAlt, faPencilSquare, faScaleBalanced, faSquareMinus, faSquarePlus, faTriangleExclamation, faUserPlus, faXmark } from '@fortawesome/free-solid-svg-icons';
import { catchError, finalize, forkJoin, of, switchMap, tap } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { EvenementGoogleAgenda, SeanceCalendrier } from 'src/app/models/seance';
import { AgendaGoogleConfiguration } from 'src/app/models';
import { FormsModule } from '@angular/forms';
import { CalendrierComponent } from '../../template/calendrier/calendrier.component';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { LoginPageService } from '../../_services/login-page.service';
import { SessionNavigationService } from '../../_services/session-navigation.service';

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
  couleurSection?: string | null;
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
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    imports: [FormsModule, CalendrierComponent]
})
export class LoginComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private toastr = inject(ToastService);
  transmissionService = inject(ParamTransmissionService);
  private authService = inject(AuthService);
  private tokenStorage = inject(TokenStorageService);
  router = inject(Router);
  private route = inject(ActivatedRoute);
  paramService = inject(ParamService);
  private activiteService = inject(ActiviteService);
  private readonly loginPage = inject(LoginPageService);
  private readonly sessionNavigation = inject(SessionNavigationService);


  form: any = {
    username: null,
    password: null
  };

  faClock = faClock;
  faXmark = faXmark;
  faCheck = faCheck;
  faTriangleExclamation = faTriangleExclamation;
  isLoggedIn = false;
  isLoginFailed = false;
  isresetFailed = false;
  isSuccessful = false;
  inscriptionSubmitting = false;
  inscriptionError = '';
  messageInscription = '';
  textRGPD = '';

  get newInscription(): boolean {
    return this.loginPage.view() === 'inscription' && this.loginPage.inscriptionOpen() && !this.isLoggedIn;
  }
  get oublieMDP(): boolean {
    return this.loginPage.view() === 'recuperation';
  }

  set oublieMDP(value: boolean) {
    this.loginPage.view.set(value ? 'recuperation' : 'connexion');
  }

  errorMessage = '';
  reinitMDPDone = false;
  roles: string[] = [];
  messageConnexion: string = ""
  textMaintenance: string = ""
  calendrier: JourCalendrier[] = [];
  jourSelectionne: JourCalendrier | null = null;
  popupJourOuverte = false;
  moisAffiche = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  chargementCalendrier = false;
  erreurCalendrier = '';
  agendasGoogle: AgendaGoogleConfiguration[] = [];
  googleAgendaIds: string[] = [];
  googleAgendaErreur = '';
  sessionExpiree = false;
  readonly joursSemaine = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  maintenance: Boolean = false;
  // L'interface ne sonde plus l'existence d'un compte : cette valeur évite
  // d'exposer des panneaux fondés sur une information sensible.
  userExist = true;

  ngOnInit(): void {
    this.loginPage.view.set('connexion');

    this.sessionExpiree = this.route.snapshot.queryParamMap.get('sessionExpiree') === '1';

    this.chargerConfigurationAgendas();

    this.paramService.getAllText().subscribe({
      next: (data) => {
        this.messageConnexion = data.find(param => param.paramName == "Text_Accueil")?.paramValue || '';
        this.messageInscription = data.find(param => param.paramName === 'Text_Inscription')?.paramValue || '';
        this.textRGPD = data.find(param => param.paramName === 'RGPD')?.paramValue || '';
        this.textMaintenance = data.find(param => param.paramName == "Text_Maintenance")?.paramValue || '';
      },
      error: (error) => {
      }
    });
    this.paramService.isClose()
      .subscribe({
        next: (data) => {
          this.maintenance = data;

          if (this.tokenStorage.getToken() && !this.tokenStorage.isTokenExpired()) {
            this.isLoggedIn = true;
            this.roles = this.tokenStorage.getUser().roles;
            if (!this.maintenance) {
              this.router.navigate(['accueil']);
            }
          }

        },
        error: (error) => {
          this.maintenance = true;
        }
      });


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

  get libelleMois(): string {
    return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })
      .format(this.moisAffiche);
  }

  get libelleJourSelectionne(): string {
    if (!this.jourSelectionne) {
      return '';
    }
    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }).format(this.jourSelectionne.date);
  }

  changerMois(decalage: number): void {
    this.moisAffiche = new Date(
      this.moisAffiche.getFullYear(), this.moisAffiche.getMonth() + decalage, 1);
    this.chargerCalendrier();
  }

  revenirAujourdhui(): void {
    const maintenant = new Date();
    this.moisAffiche = new Date(maintenant.getFullYear(), maintenant.getMonth(), 1);
    this.chargerCalendrier();
  }

  selectionnerJour(jour: JourCalendrier): void {
    this.jourSelectionne = jour;
    this.popupJourOuverte = true;
  }

  fermerPopupJour(): void {
    this.popupJourOuverte = false;
  }

  chargerCalendrier(): void {
    const debut = this.premierJourVisible();
    const fin = new Date(debut);
    fin.setDate(fin.getDate() + 41);
    this.chargementCalendrier = true;
    this.erreurCalendrier = '';
    this.googleAgendaErreur = '';
    const dateDebut = this.dateIso(debut);
    const dateFin = this.dateIso(fin);
    const seances$ = this.activiteService.getCalendrier(dateDebut, dateFin).pipe(catchError(() => {
      this.erreurCalendrier = "Le calendrier des séances n'est pas disponible pour le moment.";
      return of([] as SeanceCalendrier[]);
    }));
    const google$ = this.googleAgendaIds.length > 0
      ? this.activiteService.getCalendrierGoogle(dateDebut, dateFin, this.googleAgendaIds).pipe(catchError(() => {
          this.googleAgendaErreur = "Les agendas Google publics ne sont pas disponibles pour le moment.";
          return of({ evenements: [] as EvenementGoogleAgenda[], erreurs: [] });
        }))
      : of({ evenements: [] as EvenementGoogleAgenda[], erreurs: [] });

    forkJoin({ seances: seances$, google: google$ }).subscribe(({ seances, google }) => {
      this.googleAgendaErreur ||= google.erreurs.join(' ');
      this.construireCalendrier(debut, seances, google.evenements);
      this.chargementCalendrier = false;
    });
  }

  heure(dateHeure: string): string {
    return dateHeure?.substring(11, 16) || '';
  }

  etatLibelle(etat?: SeanceCalendrier['etatSeance']): string {
    return etat === 'ANNULEE' ? 'Annulée'
      : etat === 'REALISEE' ? 'Réalisée'
      : etat === 'MODIFIEE' ? 'Modifiée'
      : 'Programmée';
  }

  iconeEtat(etat?: SeanceCalendrier['etatSeance']) {
    return etat === 'ANNULEE' ? this.faXmark
      : etat === 'REALISEE' ? this.faCheck
      : etat === 'MODIFIEE' ? this.faTriangleExclamation
      : this.faClock;
  }

  classeEtat(etat?: SeanceCalendrier['etatSeance']): string {
    return `etat-${(etat || 'PROGRAMMEE').toLowerCase()}`;
  }

  private premierJourVisible(): Date {
    const premierDuMois = new Date(this.moisAffiche.getFullYear(), this.moisAffiche.getMonth(), 1);
    const decalageDepuisLundi = (premierDuMois.getDay() + 6) % 7;
    const debut = new Date(premierDuMois);
    debut.setDate(debut.getDate() - decalageDepuisLundi);
    return debut;
  }

  private construireCalendrier(debut: Date, seances: SeanceCalendrier[], google: EvenementGoogleAgenda[]): void {
    const evenementsParJour = new Map<string, EvenementCalendrier[]>();
    const evenements: EvenementCalendrier[] = [
      ...seances.map(seance => ({
        id: `seance-${seance.id}`,
        titre: seance.activiteNom,
        lieu: seance.salle,
        adresseSalle: seance.adresseSalle,
        commentaire: seance.commentaire,
        couleurSalle: seance.couleurSalle,
        couleurSection: seance.couleurSection,
        lien: seance.lien,
        debut: seance.debut,
        fin: seance.fin,
        source: 'SEANCE' as const,
        journeeEntiere: false,
        etatSeance: seance.etatSeance
      })),
      ...google.map(evenement => ({ ...evenement, source: 'GOOGLE' as const }))
    ];
    evenements.forEach(evenement => {
      let date = new Date(`${evenement.debut.substring(0, 10)}T12:00:00`);
      const dernierJour = new Date(`${evenement.fin.substring(0, 10)}T12:00:00`);
      if (evenement.journeeEntiere) dernierJour.setDate(dernierJour.getDate() - 1);
      do {
        const iso = this.dateIso(date);
        evenementsParJour.set(iso, [...(evenementsParJour.get(iso) || []), evenement]);
        date.setDate(date.getDate() + 1);
      } while (date <= dernierJour);
    });

    const aujourdHui = this.dateIso(new Date());
    this.calendrier = Array.from({ length: 42 }, (_, index) => {
      const date = new Date(debut);
      date.setDate(debut.getDate() + index);
      const iso = this.dateIso(date);
      return {
        date,
        iso,
        numero: date.getDate(),
        dansLeMois: date.getMonth() === this.moisAffiche.getMonth(),
        aujourdhui: iso === aujourdHui,
        evenements: (evenementsParJour.get(iso) || []).sort((a, b) => a.debut.localeCompare(b.debut))
      };
    });

    const ancienneSelection = this.jourSelectionne?.iso;
    this.jourSelectionne = this.calendrier.find(jour => jour.iso === ancienneSelection)
      || this.calendrier.find(jour => jour.iso === aujourdHui)
      || this.calendrier.find(jour => jour.dansLeMois && jour.evenements.length > 0)
      || this.calendrier.find(jour => jour.dansLeMois)
      || null;
  }

  private dateIso(date: Date): string {
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${mois}-${jour}`;
  }

  heureEvenement(evenement: EvenementCalendrier): string {
    return evenement.journeeEntiere ? 'Journée' : this.heure(evenement.debut);
  }

  classeEvenement(evenement: EvenementCalendrier): string {
    return evenement.source === 'GOOGLE' ? 'source-google' : this.classeEtat(evenement.etatSeance);
  }

  couleurEvenement(evenement: EvenementCalendrier): string {
    if (evenement.source === 'SEANCE') {
      return evenement.couleurSection || '#5CBBAF';
    }
    return this.agendasGoogle.find(agenda => agenda.source === evenement.agendaSource)?.couleur || '#D29438';
  }

  nomAgenda(evenement: EvenementCalendrier): string {
    return this.agendasGoogle.find(agenda => agenda.source === evenement.agendaSource)?.nom
      || evenement.agenda
      || 'Agenda Google';
  }

  lienActivite(evenement: EvenementCalendrier): string | null {
    if (evenement.source !== 'SEANCE' || !evenement.lien?.trim()) {
      return null;
    }
    const lien = evenement.lien.trim();
    return /^https?:\/\//i.test(lien) ? lien : `https://${lien}`;
  }

  lienAdresseSalle(evenement: EvenementCalendrier): string | null {
    if (!evenement.adresseSalle?.trim()) {
      return null;
    }
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(evenement.adresseSalle.trim())}`;
  }

  sourceEvenement(evenement: EvenementCalendrier): string {
    return evenement.source === 'GOOGLE' ? `Google · ${this.nomAgenda(evenement)}` : this.etatLibelle(evenement.etatSeance);
  }
  onSubmitInscription(): void {
    if (!this.loginPage.inscriptionOpen() || this.inscriptionSubmitting || this.isSuccessful) return;
    const { username, password } = this.form;
    this.inscriptionSubmitting = true;
    this.inscriptionError = '';
    this.authService.register(username, password).pipe(
      tap(() => this.isSuccessful = true),
      switchMap(() => this.authService.login(username, password)),
      finalize(() => this.inscriptionSubmitting = false)
    ).subscribe({
      next: () => {
        this.isLoggedIn = true;
        this.roles = this.tokenStorage.getUser().roles;
        this.sessionNavigation.reloadAccueil();
      },
      error: error => {
        this.inscriptionError = this.isSuccessful
          ? 'Votre compte a été créé, mais la connexion a échoué. Utilisez le bouton Connexion pour vous connecter.'
          : error.error?.message || "Votre inscription a échoué. Veuillez réessayer.";
      }
    });
  }

  onSubmit(): void {
    const { username, password } = this.form;

    this.authService.login(username, password).subscribe(
        data => {
          this.isLoginFailed = false;
          this.isLoggedIn = true;
          this.roles = this.tokenStorage.getUser().roles;
          this.sessionNavigation.reloadAccueil();

        },
        err => {
          this.errorMessage = err.error.message;
          this.showWarning("La connexion a échouée, mauvais mot de passe")
        }
      );
  }



  forgotMdp() {

    const { username, password } = this.form;
    this.authService.reinitPassword(username).subscribe(
      data => {

        this.reinitMDPDone = true;
        this.oublieMDP = false;
        this.isresetFailed = false;
      },
      err => {
        this.errorMessage = err.error.message;
        this.isresetFailed = true;
      }
    );
  }


  verifMailExist() {
    this.form.username = this.form.username.toLowerCase()
    this.form.username = this.form.username.trimEnd()
    this.form.username = this.form.username.trimStart()
  }

  showWarning(message: string) {
    this.toastr.warning(message, 'Attention');
  }
  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }
}
