import {Component, OnInit} from '@angular/core';
import {
  ActiviteDropDown,
  Adhesion,
  AdhesionExcel,
  Document,
  HoraireDropDown,
  Notification,
  Paiement
} from 'src/app/models';
import {AdhesionService} from 'src/app/_services/adhesion.service';
import {
  faEarth,
  faBasketball,
  faFilterCircleXmark,
  faFilter,
  faPen,
  faEye,
  faEnvelope,
  faCircleUser,
  faFlag,
  faCircleXmark,
  faCircleExclamation,
  faBook,
  faScaleBalanced,
  faPencilSquare,
  faSquarePlus,
  faSquareMinus,
  faCircleCheck,
  faUserPlus,
  faL
} from '@fortawesome/free-solid-svg-icons';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TokenStorageService} from 'src/app/_services/token-storage.service';
import {Router} from '@angular/router';
import {ParamService} from 'src/app/_services/param.service';
import {ActiviteService} from 'src/app/_services/activite.service';
import {ExcelService} from 'src/app/_services/excel.service';
import {FilterAdhesionByPipe} from 'src/app/_helpers/filterAdhesion.pipe';
import {DatePipe} from '@angular/common';

import {AdherentService} from 'src/app/_services/adherent.service';
import {ToastrService} from 'ngx-toastr';


@Component({
  selector: 'app-adherents',
  templateUrl: './adhesions.component.html',
  styleUrls: ['./adhesions.component.css']
})
export class AdhesionsComponent implements OnInit {
  faSeedling = faEarth
  faBasketball = faBasketball
  faFilterCircleXmark = faFilterCircleXmark
  faFilter = faFilter
  faCircleUser = faCircleUser
  faCircleExclamation = faCircleExclamation
  faPen = faPen
  faSquarePlus = faSquarePlus
  faEye = faEye
  faEnvelope = faEnvelope;
  faSquareMinus = faSquareMinus;
  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faFlag = faFlag;
  adhesions: Adhesion[] = [];
  adhesionsCopy: Adhesion[] = [];


  validPaiementSecretariat:boolean|null=null;
  validDocumentSecretariat:boolean|null=null;
  statutActuel: string = "";
  flag:boolean|null=null;



  showAdmin: boolean = false;
  showSecretaire: boolean = false;
  choixSection: string = ""
  visuelselection: string = "";

  constructor(
    private toastr: ToastrService,
    private datePipe: DatePipe,
    private adherentService: AdherentService,
    private filterAdhesionBy: FilterAdhesionByPipe,
    private excelService: ExcelService,
    private activiteService: ActiviteService,
    private adhesionService: AdhesionService,
    private tokenStorageService: TokenStorageService,
    private modalService: NgbModal,
    public paramService: ParamService,
    public router: Router
  ) {
  }

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
        this.choixSection = "activite#Aquagym"
        this.visuelselection = "Aquagym-Tous horaires"
      }
    } else {
      this.router.navigate(['login']);
    }
    this.getAdhesion()
    this.getActivites()
  }

  loadder: boolean = true

  getAdhesion() {
    this.loadder = true
    this.adhesions = []
    this.adhesionsCopy = []
    this.adhesionService.getAllLiteBysection(this.choixSection).subscribe({
      next: (data) => {
        console.log("data")
        console.log(data)
        data.forEach(value => value.nomprenom = value.adherent.nom + value.adherent.prenom)
        this.adhesions = data;
        this.adhesionsCopy = data;
        this.loadder = false
        console.log("loadder")
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
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


  updateFlag(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updateFlag(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.flag = data.flag;
        adhesion.derniereModifs = data.derniereModifs
        adhesion.derniereVisites = data.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  updateDocumentsSecretariat(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updateDocumentsSecretariat(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.validDocumentSecretariat = data.validDocumentSecretariat;
        adhesion.derniereModifs = data.derniereModifs
        adhesion.derniereVisites = data.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }


  updatePaiementSecretariat(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updatePaiementSecretariat(adhesion.id, statut).subscribe({
      next: (data) => {
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat;
        adhesion.derniereModifs = data.derniereModifs
        adhesion.derniereVisites = data.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  choisirStatut(adhesion: Adhesion, statutActuel: string) {
    this.adhesionService.choisirStatut(adhesion.id, statutActuel).subscribe({
      next: (data) => {
        this.showSuccess("Changement de statut de l'adhésion réussie pour l'adhérent " + adhesion.adherent.prenom + " " + adhesion.adherent.nom)
        adhesion.statutActuel = data.statutActuel;
        adhesion.derniereModifs = data.derniereModifs
        adhesion.derniereVisites = data.derniereVisites
        adhesion.validDocumentSecretariat = data.validDocumentSecretariat
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat
        adhesion.accords = data.accords
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

  enregistrerRemarque(adhesion: Adhesion) {
    this.adhesionService.enregistrerRemarque(adhesion.id, adhesion.remarqueSecretariat).subscribe({
      next: (data) => {
        adhesion.remarqueSecretariat = data.remarqueSecretariat
        adhesion.derniereModifs = data.derniereModifs
        adhesion.derniereVisites = data.derniereVisites
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }



  dismissSupress() {
    this.modalService.dismissAll();

  }

  acceptSupress(adhesion: Adhesion) {
    this.modalService.dismissAll();
    this.adhesionService.deleteAdhesion(adhesion.id).subscribe({
      next: (data) => {
        this.showSuccess("Suppresson de l'adhésion réussie pour l'adhérent " + adhesion.adherent.prenom + " " + adhesion.adherent.nom)
        this.adhesions = this.adhesions.filter(adh => adh.id != adhesion.id)
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    })
  }

  selectedAdhesion: Adhesion = new Adhesion;

  openModal(targetModal: any, adhesion: Adhesion) {
    this.selectedAdhesion = adhesion;

    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });


  }

  dismiss(selectedAdhesion: Adhesion) {
    this.modalService.dismissAll();
  }

  dismissDoc() {
    this.modalService.dismissAll();
  }

  onSubmit() {
    this.modalService.dismissAll();
  }

  retraitPaiement(adhesion: Adhesion, paiementId: number) {
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

  updatePaiement(adhesion: Adhesion, paiement: Paiement) {
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

  addNewPaiement(adhesion: Adhesion) {
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

  calculSomme(adhesion: Adhesion): number {
    return adhesion.paiements.map(paiement => paiement.montant).reduce((a, b) => a + b, 0)
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


  exportAsXLSX(): void {
    let adhesions: AdhesionExcel[] = []
    this.adhesions.forEach(adhesion => {
      let newAdhesionExcel = new AdhesionExcel;
      if (adhesion.adherent) {
        newAdhesionExcel.nomAdherent = adhesion.adherent.nom
        newAdhesionExcel.telephone = adhesion.adherent.representant != undefined ? adhesion.adherent.representant.telephone : adhesion.adherent.telephone;
        newAdhesionExcel.prenomAdherent = adhesion.adherent.prenom
        newAdhesionExcel.emailAdherent = adhesion.adherent.representant != undefined ? adhesion.adherent.representant.user.username : adhesion.adherent.user.username
      }


      if (adhesion.activite) {
        newAdhesionExcel.nomActivite = adhesion.activite.nom + " " + adhesion.activite.horaire
      }
      newAdhesionExcel.statutActuel = adhesion.statutActuel

      if (adhesion.paiements[0]) {
        newAdhesionExcel.dateReglement1 = adhesion.paiements[0].dateReglement
        newAdhesionExcel.typeReglement1 = adhesion.paiements[0].typeReglement
        newAdhesionExcel.paiement1 = adhesion.paiements[0].montant
      }
      if (adhesion.paiements[1]) {
        newAdhesionExcel.dateReglement2 = adhesion.paiements[1].dateReglement
        newAdhesionExcel.typeReglement2 = adhesion.paiements[1].typeReglement
        newAdhesionExcel.paiement2 = adhesion.paiements[1].montant
      }
      if (adhesion.paiements[2]) {
        newAdhesionExcel.dateReglement3 = adhesion.paiements[2].dateReglement
        newAdhesionExcel.typeReglement3 = adhesion.paiements[2].typeReglement
        newAdhesionExcel.paiement3 = adhesion.paiements[2].montant
      }
      newAdhesionExcel.flag = adhesion.flag
      newAdhesionExcel.remarqueSecretariat = adhesion.remarqueSecretariat
      newAdhesionExcel.validDocumentSecretariat = adhesion.validDocumentSecretariat
      newAdhesionExcel.validPaiementSecretariat = adhesion.validPaiementSecretariat

      adhesions.push(newAdhesionExcel)
    })
    this.excelService.exportAsExcelFile(adhesions, 'adhesions');
  }


  updateVisiteAdhesion(adhesion: Adhesion) {
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

  updateVisiteAdherent(adhesion: Adhesion) {
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


  verifyAdhesion(adhesion: Adhesion): boolean {

    let lastVisite = adhesion.derniereVisites.length > 0 ? adhesion.derniereVisites.reduce(function (r, a) {
      return r.date > a.date ? r : a;
    }) : undefined

    if (lastVisite != undefined && lastVisite.user.id == this.tokenStorageService.getUser().id) {
      return false
    } else {
      return true
    }
  }


  verifyAdherent(adhesion: Adhesion): boolean {

    let lastVisite = adhesion.adherent.derniereVisites.length > 0 ? adhesion.adherent?.derniereVisites?.reduce(function (r, a) {
      return r.date > a.date ? r : a;
    }) : undefined

    if (lastVisite != undefined && lastVisite.user.id == this.tokenStorageService.getUser().id) {
      return false
    } else {
      return true
    }
  }
}
