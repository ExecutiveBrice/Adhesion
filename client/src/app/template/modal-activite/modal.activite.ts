import { Component, inject, Input, OnDestroy, OnInit } from '@angular/core'
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { Activite, AdherentLite, PlanificationHebdomadaire, SalleConfiguration } from 'src/app/models';
import { Seance } from 'src/app/models/seance';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { faCircleQuestion, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faCircleCheck, faUserPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { ToastService } from '../../_services/toast.service';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgClass, DatePipe } from '@angular/common';
import { UserCheckboxDropdownComponent } from '../user-checkbox-dropdown/user-checkbox-dropdown.component';

@Component({
    selector: 'modal',
    templateUrl: './modal.activite.html',
    styleUrls: ['./modal.activite.css'],
    imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, FormsModule, FaIconComponent, NgClass, DatePipe, UserCheckboxDropdownComponent]
})
export class ModalActivite implements OnInit, OnDestroy {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private toastr = inject(ToastService);
  private adherentService = inject(AdherentService);
  activiteService = inject(ActiviteService);
  private paramService = inject(ParamService);

  faCircleQuestion = faCircleQuestion
  faEnvelope = faEnvelope;
  faCircleXmark = faCircleXmark;
  faCloudDownloadAlt = faCloudDownloadAlt;
  faScaleBalanced = faScaleBalanced;
  faBook = faBook;
  faUserPlus = faUserPlus;
  faCircleCheck = faCircleCheck;
  faPencilSquare = faPencilSquare;
  faSquarePlus = faSquarePlus;
  faTrash = faTrash;


  activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt = faExternalLinkSquareAlt;
  @Input()
  activite!: Activite;

  profs: AdherentLite[] = []
  referents: AdherentLite[] = []
  salles: SalleConfiguration[] = [];
  seances: Seance[] = [];
  nombreSeances = 14;
  dateDebutSeances = '';
  planificationAjout?: PlanificationHebdomadaire;
  modalAjoutSeancesOuverte = false;
  categorieEnEdition?: PlanificationHebdomadaire;
  indexCategorieEnEdition?: number;
  modalEditionCategorieOuverte = false;
  chargementSeances = false;
  ajoutSeancesEnCours = false;
  enregistrementSeances = new Set<number>();
  suppressionSeances = new Set<number>();
  private commentaireTimers = new Map<number, ReturnType<typeof setTimeout>>();
  private horaireTimers = new Map<number, ReturnType<typeof setTimeout>>();
  readonly joursSemaine = [
    { valeur: 'MONDAY', libelle: 'Lundi' },
    { valeur: 'TUESDAY', libelle: 'Mardi' },
    { valeur: 'WEDNESDAY', libelle: 'Mercredi' },
    { valeur: 'THURSDAY', libelle: 'Jeudi' },
    { valeur: 'FRIDAY', libelle: 'Vendredi' },
    { valeur: 'SATURDAY', libelle: 'Samedi' },
    { valeur: 'SUNDAY', libelle: 'Dimanche' },
  ];

  ngOnInit(): void {
    this.activite.profs ??= [];
    this.activite.referents ??= [];
    this.initialiserPlanificationsHebdomadaires();
    this.dateDebutSeances = this.aujourdHui();
    this.getProfs();
    if (this.activite.id) {
      this.getReferents();
    }
    this.getSalles();
    if (this.activite.id) {
      this.getSeances();
    }
  }

  ngOnDestroy(): void {
    this.commentaireTimers.forEach(timer => clearTimeout(timer));
    this.horaireTimers.forEach(timer => clearTimeout(timer));
  }


  enregistrer() {
    this.retirerPlanificationHistoriqueSiVide();
    this.activiteService.save(this.activite).subscribe(
      data => {
        this.showSucces(data.nom + " " + data.horaire)
        this.activeModal.close('valider')
      },
      err => {
        this.showError(err.message)
      }
    );
  }


  getProfs() {
    this.adherentService.getByRole(3).subscribe(
      data => {
        this.profs = data;
      },
      err => {
        this.showError(err.message)
      }
    );

  }

  getReferents() {
    this.activiteService.getReferentsCandidates(this.activite.id).subscribe(
      data => this.referents = data,
      err => this.showError(err.message)
    );
  }

  getSalles(): void {
    this.paramService.getSalles().subscribe({
      next: salles => this.salles = salles,
      error: err => this.showError(err.message)
    });
  }

  comparerSalles(salleA?: SalleConfiguration, salleB?: SalleConfiguration): boolean {
    return salleA?.id === salleB?.id;
  }

  libelleJour(valeur: string): string {
    return this.joursSemaine.find(jour => jour.valeur === valeur)?.libelle ?? valeur;
  }

  nomsUtilisateurs(utilisateurs?: { prenom: string; nom: string }[]): string {
    return (utilisateurs ?? []).map(utilisateur => `${utilisateur.prenom} ${utilisateur.nom}`).join(', ');
  }

  ouvrirModalEditionCategorie(index?: number): void {
    this.indexCategorieEnEdition = index;
    const planification = index === undefined
      ? {
      jour: '',
      horaireDebut: '',
      duree: null,
      descriptif: '',
      salle: undefined,
      profs: [],
      referents: []
    }
      : this.activite.planificationsHebdomadaires[index];
    this.categorieEnEdition = this.copierPlanification(planification);
    this.modalEditionCategorieOuverte = true;
  }

  fermerModalEditionCategorie(): void {
    this.modalEditionCategorieOuverte = false;
    this.categorieEnEdition = undefined;
    this.indexCategorieEnEdition = undefined;
  }

  enregistrerCategorie(): void {
    if (!this.categorieEnEdition) {
      return;
    }

    const categorie = this.copierPlanification(this.categorieEnEdition);
    if (this.indexCategorieEnEdition === undefined) {
      this.activite.planificationsHebdomadaires.push(categorie);
    } else {
      this.activite.planificationsHebdomadaires[this.indexCategorieEnEdition] = categorie;
    }
    this.fermerModalEditionCategorie();
  }

  retirerPlanificationHebdomadaire(index: number): void {
    this.activite.planificationsHebdomadaires.splice(index, 1);
  }

  ouvrirModalAjoutSeances(planification: PlanificationHebdomadaire): void {
    if (!this.activite.id || !planification.id) {
      return;
    }
    this.planificationAjout = planification;
    this.nombreSeances = 14;
    this.dateDebutSeances = this.aujourdHui();
    this.modalAjoutSeancesOuverte = true;
  }

  fermerModalAjoutSeances(): void {
    if (!this.ajoutSeancesEnCours) {
      this.modalAjoutSeancesOuverte = false;
      this.planificationAjout = undefined;
    }
  }

  getSeances() {
    this.chargementSeances = true;
    this.activiteService.getSeances(this.activite.id).subscribe({
      next: data => {
        this.seances = this.preparerSeances(data);
        this.chargementSeances = false;
      },
      error: err => {
        this.chargementSeances = false;
        this.showError(err.message);
      }
    });
  }

  ajouterSeances() {
    if (!this.activite.id || !this.planificationAjout?.id || !this.dateDebutSeances || this.nombreSeances < 1) {
      return;
    }

    this.ajoutSeancesEnCours = true;
    this.activiteService.ajouterSeancesPlanification(
      this.activite.id,
      this.planificationAjout.id,
      this.nombreSeances,
      this.dateDebutSeances
    ).subscribe({
      next: data => {
        this.seances = this.preparerSeances(data);
        this.ajoutSeancesEnCours = false;
        this.toastr.success(
          `${data.length} séance(s) ajoutée(s)`,
          'Séances enregistrées'
        );
        this.fermerModalAjoutSeances();
      },
      error: err => {
        this.ajoutSeancesEnCours = false;
        this.showError(err.message);
      }
    });
  }

  enregistrerEtatSeance(seance: Seance) {
    if (!this.activite.id || !seance.id) {
      return;
    }

    this.enregistrementSeances.add(seance.id);
    this.activiteService.modifierEtatSeance(this.activite.id, seance).subscribe({
      next: () => this.enregistrementSeances.delete(seance.id),
      error: err => {
        this.enregistrementSeances.delete(seance.id);
        this.getSeances();
        this.showError(err.message);
      }
    });
  }

  enregistrerCommentaireSeance(seance: Seance) {
    if (!this.activite.id || !seance.id) {
      return;
    }

    this.enregistrementSeances.add(seance.id);
    this.activiteService.modifierCommentaireSeance(this.activite.id, seance).subscribe({
      next: () => this.enregistrementSeances.delete(seance.id),
      error: err => {
        this.enregistrementSeances.delete(seance.id);
        this.getSeances();
        this.showError(err.message);
      }
    });
  }

  planifierEnregistrementCommentaire(seance: Seance) {
    const timerExistant = this.commentaireTimers.get(seance.id);
    if (timerExistant) {
      clearTimeout(timerExistant);
    }

    this.commentaireTimers.set(seance.id, setTimeout(() => {
      this.commentaireTimers.delete(seance.id);
      this.enregistrerCommentaireSeance(seance);
    }, 700));
  }

  enregistrerCommentaireAuBlur(seance: Seance) {
    const timerExistant = this.commentaireTimers.get(seance.id);
    if (timerExistant) {
      clearTimeout(timerExistant);
      this.commentaireTimers.delete(seance.id);
    }
    this.enregistrerCommentaireSeance(seance);
  }

  enregistrerHoraireSeance(seance: Seance) {
    if (!this.activite.id || !seance.id || !seance.dateEdition || !seance.heureEdition) {
      return;
    }

    this.enregistrementSeances.add(seance.id);
    this.activiteService.modifierHoraireSeance(this.activite.id, seance).subscribe({
      next: data => {
        seance.debut = data.debut;
        seance.fin = data.fin;
        this.preparerSeance(seance);
        this.enregistrementSeances.delete(seance.id);
      },
      error: err => {
        this.enregistrementSeances.delete(seance.id);
        this.getSeances();
        this.showError(err.message);
      }
    });
  }

  enregistrerSalleSeance(seance: Seance) {
    if (!this.activite.id || !seance.id) {
      return;
    }

    this.enregistrementSeances.add(seance.id);
    this.activiteService.modifierSalleSeance(this.activite.id, seance).subscribe({
      next: data => {
        seance.salle = data.salle;
        this.enregistrementSeances.delete(seance.id);
      },
      error: err => {
        this.enregistrementSeances.delete(seance.id);
        this.getSeances();
        this.showError(err.message);
      }
    });
  }

  planifierEnregistrementHoraire(seance: Seance) {
    const timerExistant = this.horaireTimers.get(seance.id);
    if (timerExistant) {
      clearTimeout(timerExistant);
    }
    this.horaireTimers.set(seance.id, setTimeout(() => {
      this.horaireTimers.delete(seance.id);
      this.enregistrerHoraireSeance(seance);
    }, 500));
  }

  enregistrerHoraireAuBlur(seance: Seance) {
    const timerExistant = this.horaireTimers.get(seance.id);
    if (timerExistant) {
      clearTimeout(timerExistant);
      this.horaireTimers.delete(seance.id);
    }
    this.enregistrerHoraireSeance(seance);
  }

  supprimerSeance(seance: Seance) {
    if (!this.activite.id || !seance.id) {
      return;
    }

    const date = new Date(seance.debut).toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
    if (!confirm(`Supprimer la séance du ${date} ?`)) {
      return;
    }

    const timerCommentaire = this.commentaireTimers.get(seance.id);
    if (timerCommentaire) {
      clearTimeout(timerCommentaire);
      this.commentaireTimers.delete(seance.id);
    }
    const timerHoraire = this.horaireTimers.get(seance.id);
    if (timerHoraire) {
      clearTimeout(timerHoraire);
      this.horaireTimers.delete(seance.id);
    }
    this.suppressionSeances.add(seance.id);
    this.activiteService.supprimerSeance(this.activite.id, seance.id).subscribe({
      next: () => {
        this.suppressionSeances.delete(seance.id);
        this.seances = this.seances.filter(element => element.id !== seance.id);
        this.toastr.success('La séance a été supprimée', 'Séance supprimée');
      },
      error: err => {
        this.suppressionSeances.delete(seance.id);
        this.showError(err.message);
      }
    });
  }

  private aujourdHui(): string {
    const maintenant = new Date('2026-09-14T00:00:00.000Z');
    const dateLocale = new Date(maintenant.getTime() - maintenant.getTimezoneOffset() * 60_000);
    return dateLocale.toISOString().slice(0, 10);
  }

  private initialiserPlanificationsHebdomadaires(): void {
    this.activite.planificationsHebdomadaires ??= [];
    if (this.activite.planificationsHebdomadaires.length > 0) {
      const categoriesSansIntervenant = this.activite.planificationsHebdomadaires.every(planification =>
        !(planification.profs?.length || planification.referents?.length)
      );
      this.activite.planificationsHebdomadaires.forEach(planification => {
        // Les affectations précédentes étaient enregistrées au niveau de l'activité.
        // À la première édition, elles sont proposées pour chaque catégorie.
        planification.profs = categoriesSansIntervenant ? [...this.activite.profs] : (planification.profs ?? []);
        planification.referents = categoriesSansIntervenant ? [...this.activite.referents] : (planification.referents ?? []);
      });
      return;
    }

    // Les activités existantes n'avaient qu'un créneau : on le rend éditable
    // comme première ligne de la nouvelle planification.
    const planification: PlanificationHebdomadaire = {
      jour: this.activite.jour ?? '',
      horaireDebut: this.activite.horaireDebut ?? '',
      duree: this.activite.duree ?? null,
      descriptif: '',
      salle: this.activite.salle,
      profs: [...this.activite.profs],
      referents: [...this.activite.referents]
    };
    this.activite.planificationsHebdomadaires.push(planification);
  }

  private retirerPlanificationHistoriqueSiVide(): void {
    const aUnCreneau = this.activite.planificationsHebdomadaires.some(planification =>
      !!planification.jour || !!planification.horaireDebut || planification.duree != null
    );
    if (!aUnCreneau) {
      // Les anciennes propriétés ne doivent pas recréer un créneau supprimé.
      this.activite.jour = undefined!;
      this.activite.horaireDebut = undefined!;
      this.activite.duree = undefined!;
      this.activite.salle = undefined;
    }
  }

  private copierPlanification(planification: PlanificationHebdomadaire): PlanificationHebdomadaire {
    return {
      ...planification,
      profs: [...(planification.profs ?? [])],
      referents: [...(planification.referents ?? [])]
    };
  }

  private preparerSeances(seances: Seance[]): Seance[] {
    return seances.map(seance => this.preparerSeance(seance));
  }

  private preparerSeance(seance: Seance): Seance {
    seance.dateEdition = seance.debut?.slice(0, 10) || '';
    seance.heureEdition = seance.debut?.slice(11, 16) || '';
    return seance;
  }

  showSucces(message: string) {
    this.toastr.success(message, 'Enregistrement réussi pour l\'activité');
  }
  showWarning(message: string) {
    this.toastr.warning(message, 'Veuillez patienter,');
  }
  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }

}
