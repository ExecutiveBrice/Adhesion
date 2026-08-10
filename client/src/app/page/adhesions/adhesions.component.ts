import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ActiviteDropDown,
  AdhesionLite,
  Document,
  HoraireDropDown,
  Paiement
} from 'src/app/models';
import {AdhesionService} from 'src/app/_services/adhesion.service';
import {
  faPen,
  faCircleUser,
  faFlag,
  faCircleXmark,
  faCircleExclamation,
  faSquareMinus,
  faCircleCheck,
} from '@fortawesome/free-solid-svg-icons';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TokenStorageService} from 'src/app/_services/token-storage.service';
import {Router} from '@angular/router';
import {ParamService} from 'src/app/_services/param.service';
import {ActiviteService} from 'src/app/_services/activite.service';
import {AdherentService} from 'src/app/_services/adherent.service';
import { ToastService } from '../../_services/toast.service';
import {Subject, debounceTime, distinctUntilChanged} from 'rxjs';
import { FormsModule } from '@angular/forms';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownButtonItem, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgClass, DatePipe } from '@angular/common';
import { OrderByPipe } from '../../_helpers/sort.pipe';


@Component({
    selector: 'app-adherents',
    templateUrl: './adhesions.component.html',
    styleUrls: ['./adhesions.component.css'],
    imports: [FormsModule, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownButtonItem, NgbDropdownItem, FaIconComponent, NgClass, DatePipe, OrderByPipe]
})
export class AdhesionsComponent implements OnInit {
  private toastr = inject(ToastService);
  private adherentService = inject(AdherentService);
  private activiteService = inject(ActiviteService);
  private adhesionService = inject(AdhesionService);
  private tokenStorageService = inject(TokenStorageService);
  private modalService = inject(NgbModal);
  paramService = inject(ParamService);
  router = inject(Router);

  faCircleUser = faCircleUser
  faCircleExclamation = faCircleExclamation
  faPen = faPen
  faSquareMinus = faSquareMinus;
  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faFlag = faFlag;
  adhesions: AdhesionLite[] = [];
  page = 1;
  pageSize = 20;
  readonly pageSizes = [10, 20, 50, 100];
  totalElements = 0;
  totalPages = 0;
  searchTerm = '';
  sortField = 'adherent.nom';
  sortDirection: 'asc' | 'desc' = 'asc';

  validPaiementSecretariat: boolean | null = null;
  validDocumentSecretariat: boolean | null = null;
  statutActuel = '';
  flag: boolean | null = null;
  statusOptions: string[] = [];

  readonly generalStatuses = [
    'Attente validation adhérent',
    'Attente validation secrétariat',
    'Attente paiement',
    'Validée, en attente du certificat médical',
    'Validée groupement sportif',
    'Sur liste d\'attente',
    'Validée',
    'Annulée'
  ];

  readonly basketStatuses = [
    'Attente validation adhérent',
    'Attente validation secrétariat',
    'Attente création licence',
    'Licence FFBB à compléter',
    'Retour ALOD Basket',
    'Licence générée',
    'Retour Comité',
    'Licence T',
    'Sur liste d\'attente',
    'Annulée'
  ];

  private readonly searchChanges = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);



  showAdmin: boolean = false;
  showSecretaire: boolean = false;
  choixSection: string = ""
  visuelselection: string = "";

  showSuccess(message: string) {
    this.toastr.info(message, 'Information');
  }

  showSecretariat() {
    this.toastr.warning("Le secrétariat validera votre dossier lorsqu'il sera complet", "Secrétariat");
  }

  showWarning(message: string) {
    this.toastr.warning(message, 'Attention');
  }

  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }


  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
      if (this.tokenStorageService.getUser().username == "alodbasket@free.fr" || this.tokenStorageService.getUser().username == "laurence.basket@yahoo.com" || this.tokenStorageService.getUser().username == "xlcharonnat@yahoo.fr" || this.tokenStorageService.getUser().username == "c.rullie@free.fr") {
        this.choixSection = "activite#Basket"
        this.visuelselection = "Basket-Tous horaires"
      } else {
        this.choixSection = "Toutes"
        this.visuelselection = "Toutes les adhésions"
      }
    } else {
      this.router.navigate(['login']);
      return;
    }

    this.searchChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.applyFilters());

    this.getAdhesion();
    this.getActivites();
    this.getStatuses();
  }

  loadder: boolean = true

  getAdhesion(resetPage: boolean = false) {
    if (resetPage) {
      this.page = 1;
    }
    this.loadder = true;
    this.adhesionService.getPage({
      sections: this.choixSection,
      page: this.page - 1,
      size: this.pageSize,
      search: this.searchTerm.trim(),
      status: this.statutActuel,
      paymentValidated: this.validPaiementSecretariat,
      documentsValidated: this.validDocumentSecretariat,
      flagged: this.flag,
      sort: `${this.sortField},${this.sortDirection}`
    }).subscribe({
      next: (data) => {
        this.adhesions = data.content;
        this.totalElements = data.totalElements;
        this.totalPages = data.totalPages;
        this.page = data.number + 1;
        this.loadder = false;
      },
      error: (error) => {
        console.log(error);
        this.loadder = false;
        this.showError(error.error?.message || error.message);
      }
    });
  }

  onSearchChange(value: string) {
    this.searchChanges.next(value.trim());
  }

  applyFilters() {
    this.getAdhesion(true);
  }

  resetFilters() {
    this.searchTerm = '';
    this.statutActuel = '';
    this.validPaiementSecretariat = null;
    this.validDocumentSecretariat = null;
    this.flag = null;
    this.getAdhesion(true);
  }

  selectSection(section: string, label: string) {
    this.choixSection = section;
    this.visuelselection = label;
    this.getAdhesion(true);
  }

  changeSort(field: string) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.getAdhesion(true);
  }

  sortIndicator(field: string): string {
    if (this.sortField !== field) {
      return '↕';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  goToPage(targetPage: number) {
    if (targetPage < 1 || targetPage > this.totalPages || targetPage === this.page) {
      return;
    }
    this.page = targetPage;
    this.getAdhesion();
  }

  onPageSizeChange() {
    this.getAdhesion(true);
  }

  get firstResult(): number {
    return this.totalElements === 0 ? 0 : (this.page - 1) * this.pageSize + 1;
  }

  get lastResult(): number {
    return Math.min(this.page * this.pageSize, this.totalElements);
  }

  get visiblePages(): number[] {
    const windowSize = Math.min(4, this.totalPages);
    const start = Math.max(1, Math.min(this.page - 1, this.totalPages - windowSize + 1));
    return Array.from({length: windowSize}, (_, index) => start + index);
  }

  get showLeadingEllipsis(): boolean {
    return this.visiblePages[0] > 1;
  }

  get showTrailingEllipsis(): boolean {
    return this.visiblePages[this.visiblePages.length - 1] < this.totalPages;
  }

  get hasActiveFilters(): boolean {
    return !!this.searchTerm.trim() || !!this.statutActuel || this.validPaiementSecretariat !== null
      || this.validDocumentSecretariat !== null || this.flag !== null;
  }

  statusesFor(adhesion: AdhesionLite): string[] {
    return adhesion.activite.groupe === 'ALOD_B' ? this.basketStatuses : this.generalStatuses;
  }

  trackByAdhesion(index: number, adhesion: AdhesionLite): number {
    return adhesion.id;
  }

  activites: ActiviteDropDown[] = []

  getActivites() {

    this.activiteService.getAll().subscribe({
      next: (data) => {
        data.forEach(act => {
          if (this.activites.filter(activiteDropDown => activiteDropDown.nom == act.nom).length > 0) {
            let horaireDropDown = new HoraireDropDown
            horaireDropDown.id = act.id
            horaireDropDown.nom = act.horaire
            this.activites.filter(activiteDropDown => activiteDropDown.nom == act.nom)[0].horaires.push(horaireDropDown)
          } else {
            let activiteDropDown = new ActiviteDropDown()
            activiteDropDown.nom = act.nom
            let horaireDropDown = new HoraireDropDown
            horaireDropDown.id = act.id
            horaireDropDown.nom = act.horaire
            activiteDropDown.horaires.push(horaireDropDown)
            this.activites.push(activiteDropDown)
          }
        });
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  getStatuses() {
    this.adhesionService.getStatuses().subscribe({
      next: statuses => this.statusOptions = statuses,
      error: error => {
        console.log(error);
        this.statusOptions = Array.from(new Set([...this.generalStatuses, ...this.basketStatuses])).sort();
      }
    });
  }


  updateFlag(adhesion: AdhesionLite, statut: boolean) {
    this.adhesionService.updateFlag(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.flag = data.flag;
        adhesion.derniereModifs = data.derniereModifs || adhesion.derniereModifs;
        adhesion.derniereVisites = data.derniereVisites || adhesion.derniereVisites;
        if (this.flag !== null) {
          this.reloadAfterFilteredMutation();
        }
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  updateDocumentsSecretariat(adhesion: AdhesionLite, statut: boolean) {
    this.adhesionService.updateDocumentsSecretariat(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.validDocumentSecretariat = data.validDocumentSecretariat;
        adhesion.derniereModifs = data.derniereModifs || adhesion.derniereModifs;
        adhesion.derniereVisites = data.derniereVisites || adhesion.derniereVisites;
        if (this.validDocumentSecretariat !== null) {
          this.reloadAfterFilteredMutation();
        }
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }


  updatePaiementSecretariat(adhesion: AdhesionLite, statut: boolean) {
    this.adhesionService.updatePaiementSecretariat(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat;
        adhesion.derniereModifs = data.derniereModifs || adhesion.derniereModifs;
        adhesion.derniereVisites = data.derniereVisites || adhesion.derniereVisites;
        if (this.validPaiementSecretariat !== null) {
          this.reloadAfterFilteredMutation();
        }
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  choisirStatut(adhesion: AdhesionLite, statutActuel: string) {
    this.adhesionService.choisirStatut(adhesion.id, statutActuel).subscribe({
      next: (data) => {
        this.showSuccess("Changement de statut de l'adhésion réussie pour l'adhérent " + adhesion.adherent.prenom + " " + adhesion.adherent.nom)
        adhesion.statutActuel = data.statutActuel.toString();
        adhesion.derniereModifs = data.derniereModifs || adhesion.derniereModifs;
        adhesion.derniereVisites = data.derniereVisites || adhesion.derniereVisites;
        adhesion.validDocumentSecretariat = data.validDocumentSecretariat
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat
        adhesion.accords = data.accords
        if (this.statutActuel && adhesion.statutActuel !== this.statutActuel) {
          this.reloadAfterFilteredMutation();
        }
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  opennewTab(page: string) {
    window.open(page, '_blank');
  }

  enregistrerRemarque(adhesion: AdhesionLite) {
    this.adhesionService.enregistrerRemarque(adhesion.id, adhesion.remarqueSecretariat).subscribe({
      next: (data) => {
        adhesion.remarqueSecretariat = data.remarqueSecretariat?.toString() || '';
        adhesion.derniereModifs = data.derniereModifs || adhesion.derniereModifs;
        adhesion.derniereVisites = data.derniereVisites || adhesion.derniereVisites;
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  private reloadAfterFilteredMutation() {
    if (this.adhesions.length === 1 && this.page > 1) {
      this.page--;
    }
    this.getAdhesion();
  }



  dismissSupress() {
    this.modalService.dismissAll();

  }

  acceptSupress(adhesion: AdhesionLite) {
    this.modalService.dismissAll();
    this.adhesionService.deleteAdhesion(adhesion.id).subscribe({
      next: (data) => {
        this.showSuccess("Suppresson de l'adhésion réussie pour l'adhérent " + adhesion.adherent.prenom + " " + adhesion.adherent.nom)
        if (this.adhesions.length === 1 && this.page > 1) {
          this.page--;
        }
        this.getAdhesion();
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    })
  }

  selectedAdhesion: AdhesionLite = new AdhesionLite();

  openModal(targetModal: any, adhesion: AdhesionLite) {
    this.selectedAdhesion = adhesion;

    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });


  }

  dismiss(selectedAdhesion: AdhesionLite) {
    this.modalService.dismissAll();
  }

  dismissDoc() {
    this.modalService.dismissAll();
  }

  onSubmit() {
    this.modalService.dismissAll();
  }

  retraitPaiement(adhesion: AdhesionLite, paiementId: number) {
    this.adhesionService.deletePaiement(adhesion.id, paiementId).subscribe({
      next: (data) => {
        adhesion.paiements = adhesion.paiements.filter(paiement => paiement.id != paiementId)
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });

    adhesion.paiements = adhesion.paiements.filter(paiement => paiement.id != paiementId)
  }

  updatePaiement(adhesion: AdhesionLite, paiement: Paiement) {
    this.adhesionService.savePaiement(adhesion.id, paiement).subscribe({
      next: (data) => {
        adhesion.paiements = data.paiements

      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  addNewPaiement(adhesion: AdhesionLite) {
    this.adhesionService.savePaiement(adhesion.id, new Paiement).subscribe({
      next: (data) => {
        adhesion.paiements = data.paiements
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  calculSomme(adhesion: AdhesionLite): number {
    return adhesion.paiements.reduce((total, paiement) => total + (paiement.montant || 0), 0);
  }

  openEditModal(targetModal: any, doc: Document) {
    this.modalService.open(targetModal, {
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });

    this.pdfName = doc.nom
    this.pdfEditSrc = "data:" + doc.type + ";base64," + doc.content
  }

  pdfName: string = ""
  pdfSrc: string = ""
  pdfFile: any
  pdfEditSrc: string = ""




  updateVisiteAdhesion(adhesion: AdhesionLite) {
    this.adhesionService.addVisite(adhesion.id).subscribe({
      next: (response) => {
        adhesion.derniereModifs = response.derniereModifs
        adhesion.derniereVisites = response.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  updateVisiteAdherent(adhesion: AdhesionLite) {
    this.adherentService.addVisite(adhesion.adherent.id).subscribe({
      next: (response) => {
        adhesion.adherent.derniereModifs = response.derniereModifs
        adhesion.adherent.derniereVisites = response.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }


  verifyAdhesion(adhesion: AdhesionLite): boolean {

    const visites = adhesion.derniereVisites || [];
    let lastVisite = visites.length > 0 ? visites.reduce(function (r, a) {
      return r.date > a.date ? r : a;
    }) : undefined

    if (lastVisite != undefined && lastVisite.user.id == this.tokenStorageService.getUser().id) {
      return false
    } else {
      return true
    }
  }


  verifyAdherent(adhesion: AdhesionLite): boolean {

    const visites = adhesion.adherent.derniereVisites || [];
    let lastVisite = visites.length > 0 ? visites.reduce(function (r, a) {
      return r.date > a.date ? r : a;
    }) : undefined

    if (lastVisite != undefined && lastVisite.user.id == this.tokenStorageService.getUser().id) {
      return false
    } else {
      return true
    }
  }
}
