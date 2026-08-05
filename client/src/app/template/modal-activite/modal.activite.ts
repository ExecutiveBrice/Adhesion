import { Component, inject, Input, OnDestroy, OnInit } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { Activite, Adherent, AdherentLite, SalleConfiguration } from 'src/app/models';
import { Seance } from 'src/app/models/seance';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ParamService } from 'src/app/_services/param.service';
import { faCircleQuestion, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'modal',
  templateUrl: './modal.activite.html',
  styleUrls: ['./modal.activite.css']
})
export class ModalActivite implements OnInit, OnDestroy {
  faCircleQuestion = faCircleQuestion
  faEnvelope = faEnvelope;
  faCircleXmark = faCircleXmark;
  faCloudDownloadAlt = faCloudDownloadAlt;
  faScaleBalanced = faScaleBalanced;
  faBook = faBook;
  faUserPlus = faUserPlus;
  faCircleCheck = faCircleCheck;
  faSquareMinus = faSquareMinus;
  faPencilSquare = faPencilSquare;
  faSquarePlus = faSquarePlus;
  faTrash = faTrash;


  activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt = faExternalLinkSquareAlt;
  @Input()
  activite!: Activite;

  profs: AdherentLite[] = []
  salles: SalleConfiguration[] = [];
  seances: Seance[] = [];
  nombreSeances = 1;
  dateDebutSeances = '';
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

  constructor(
    private toastr: ToastrService,
    private adherentService: AdherentService,
    public activiteService: ActiviteService,
    private paramService: ParamService,
  ) { }

  ngOnInit(): void {
    this.dateDebutSeances = this.aujourdHui();
    this.getProfs();
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


  ajouterProf(newActivite: Activite, adherent: Adherent) {
    newActivite.profs.push(adherent);
  }

  retirerProf(newActivite: Activite, adherent: Adherent) {
    newActivite.profs = newActivite.profs.filter(prof => prof.id != adherent.id);
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

  getSalles(): void {
    this.paramService.getSalles().subscribe({
      next: salles => this.salles = salles,
      error: err => this.showError(err.message)
    });
  }

  comparerSalles(salleA?: SalleConfiguration, salleB?: SalleConfiguration): boolean {
    return salleA?.id === salleB?.id;
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
    if (!this.activite.id || !this.dateDebutSeances || this.nombreSeances < 1) {
      return;
    }

    this.ajoutSeancesEnCours = true;
    this.activiteService.ajouterSeances(
      this.activite.id,
      this.nombreSeances,
      this.dateDebutSeances
    ).subscribe({
      next: data => {
        this.seances = this.preparerSeances(data);
        this.ajoutSeancesEnCours = false;
        this.toastr.success(
          `${this.nombreSeances} séance(s) ajoutée(s)`,
          'Séances enregistrées'
        );
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
    const maintenant = new Date();
    const dateLocale = new Date(maintenant.getTime() - maintenant.getTimezoneOffset() * 60_000);
    return dateLocale.toISOString().slice(0, 10);
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
