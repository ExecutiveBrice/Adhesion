import { Component, OnInit } from '@angular/core';
import { ActiviteDropDown, Adhesion, AdhesionExcel, Document, HoraireDropDown, Notification, Paiement } from 'src/app/models';
import { AdhesionService } from 'src/app/_services/adhesion.service';
import { faEarth, faBasketball, faFilterCircleXmark, faFilter, faPen, faEye, faEnvelope, faCircleUser, faFlag, faCircleXmark, faCircleExclamation, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { ParamService } from 'src/app/_services/param.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { FilterAdhesionByPipe } from 'src/app/_helpers/filterAdhesion.pipe';
import { DatePipe } from '@angular/common';
import { FilterByPipe } from 'src/app/_helpers/filter.pipe';
import { AdherentService } from 'src/app/_services/adherent.service';
const ADHESIONS = 'ADHESIONS';

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
  faCircleExclamation=faCircleExclamation
  faPen = faPen
  faSquarePlus = faSquarePlus
  faEye = faEye
  faEnvelope = faEnvelope;
  faSquareMinus = faSquareMinus;
  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faFlag = faFlag;
  adhesions: Adhesion[] = [];
  cloned: Adhesion[] = [];
  isFailed: boolean = false;
  errorMessage = '';
  ordre: string = 'nom';
  search: string = "";
  sens: boolean = false;
  filtres: Map<string, any> = new Map<string, any>();
  showAdmin: boolean = false;
  showSecretaire: boolean = false;
  choixSection:string=""
  constructor(
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
  ) { }

  ngOnInit(): void {

    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
      if (this.tokenStorageService.getUser().username == "alodbasket@free.fr" || this.tokenStorageService.getUser().username == "laurence.basket@yahoo.com" || this.tokenStorageService.getUser().username == "c.rullie@free.fr" ) {
        this.choixSection = "activite#Basket"
      }else{
        this.choixSection = "activite#Aquagym"
      }
    } else {
      this.router.navigate(['login']);
    }
    this.getAdhesion()
    this.getActivites()
  }

  getAdhesion(){
    this.adhesionService.getAllLiteBysection(this.choixSection).subscribe(
      data => {
        console.log(data)
        this.cloned = data.map(x => Object.assign({}, x));
        this.adhesions = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  activites: ActiviteDropDown[] = []
  getActivites() {

    this.activiteService.getAll().subscribe(
      data => {
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
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  changeActivite(adhesion: Adhesion, activiteId: number) {
    this.adhesionService.changeActivite(adhesion.id, activiteId).subscribe(
      data => {
        adhesion.activite = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  updateFlag(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updateFlag(adhesion.id, statut).subscribe(
      data => {
        adhesion.flag = data.flag;
        adhesion.derniereModifs=data.derniereModifs
        adhesion.derniereVisites=data.derniereVisites
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  updateDocumentsSecretariat(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updateDocumentsSecretariat(adhesion.id, statut).subscribe(
      data => {
        adhesion.validDocumentSecretariat = data.validDocumentSecretariat;
        adhesion.derniereModifs=data.derniereModifs
        adhesion.derniereVisites=data.derniereVisites
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }


  updatePaiementSecretariat(adhesion: Adhesion, statut: boolean) {
    this.adhesionService.updatePaiementSecretariat(adhesion.id, statut).subscribe(
      data => {
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat;
        adhesion.derniereModifs=data.derniereModifs
        adhesion.derniereVisites=data.derniereVisites
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }
  choisirStatut(adhesion: Adhesion, statutActuel: string) {
    this.adhesionService.choisirStatut(adhesion.id, statutActuel).subscribe(
      data => {
        adhesion.statutActuel = data.statutActuel;
        adhesion.derniereModifs=data.derniereModifs
        adhesion.derniereVisites=data.derniereVisites
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  opennewTab(page : string){
    window.open(page, '_blank');
  }
  
  choisirPaiement(adhesion: Adhesion, typePaiement: string) {
    this.adhesionService.updateTypePaiement(adhesion.id, typePaiement).subscribe(
      data => {

        adhesion.typeReglement = typePaiement;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }
  enregistrerRemarque(adhesion: Adhesion) {
    this.adhesionService.enregistrerRemarque(adhesion.id, adhesion.remarqueSecretariat).subscribe(
      data => {
        adhesion.remarqueSecretariat = data.remarqueSecretariat
        adhesion.derniereModifs=data.derniereModifs
        adhesion.derniereVisites=data.derniereVisites
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }


  addSearch(search: string) {
    if (search.length > 2) {
      this.addFiltre('adherent.nomPrenom', search)
    } else {
      this.removeFiltre('adherent.nomPrenom')
    }
  }

  removeFiltre(nomFiltre: string) {

    if (this.filtres.has(nomFiltre)) {
      this.filtres.delete(nomFiltre)
    }

    this.filtrage()
  }

  addFiltre(nomFiltre: string, valeurFiltre: any) {

    if (this.filtres.has(nomFiltre)) {
      this.filtres.delete(nomFiltre)
    }
    if (valeurFiltre.length != 0) {
      this.filtres.set(nomFiltre, valeurFiltre);

    }

    this.filtrage()
  }

  filtrage() {

    this.adhesions = this.filterAdhesionBy.transform(this.cloned.map(x => Object.assign({}, x)), this.filtres)

  }


  dismissSupress() {
    this.modalService.dismissAll();

  }

  acceptSupress(adhesion: Adhesion) {
    this.modalService.dismissAll();
    this.adhesionService.deleteAdhesion(adhesion.id).subscribe(
      data => {
        this.adhesions = this.adhesions.filter(adh => adh.id != adhesion.id)
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    )
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
      }
    });
  }

  addNewPaiement(adhesion: Adhesion) {
    this.adhesionService.savePaiement(adhesion.id, new Paiement).subscribe({
      next: (data) => {
        adhesion.paiements = data.paiements
      },
      error: (error) => {
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
    this.pdfEditSrc = "data:" + doc.type + ";base64," + doc.file
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
        newAdhesionExcel.nomAdherent = adhesion.adherent.prenom + " " + adhesion.adherent.nom
        newAdhesionExcel.emailAdherent = adhesion.adherent.email
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


  updateVisiteAdhesion(adhesion:Adhesion){
    this.adhesionService.addVisite(adhesion.id).subscribe({
      next: (response) => {
        adhesion.derniereModifs=response.derniereModifs
        adhesion.derniereVisites=response.derniereVisites
      },
      error: (error) => {
        this.isFailed = true;
        this.errorMessage = error.message
      }
    });
  }

  updateVisiteAdherent(adhesion:Adhesion){
    this.adherentService.addVisite(adhesion.adherent.id).subscribe({
      next: (response) => {
        adhesion.adherent.derniereModifs=response.derniereModifs
        adhesion.adherent.derniereVisites=response.derniereVisites
      },
      error: (error) => {
        this.isFailed = true;
        this.errorMessage = error.message
      }
    });
  }


  verifyAdhesion(adhesion:Adhesion):boolean{
    let visite = adhesion.derniereVisites?.find(notif => notif.user.id == this.tokenStorageService.getUser().id);
    let modif: Notification = new Notification;
    adhesion.derniereModifs?.forEach(notif => {
      if(modif.id == 0){
        modif = notif
      }else if(Date.parse(modif.date) < Date.parse(notif.date)){
        modif = notif
      }
    });

    if(visite != undefined && (modif.id == 0 || Date.parse(modif.date) < Date.parse(visite.date))){
      return false
    }

    return true;
  }


  verifyAdherent(adhesion:Adhesion):boolean{
    let visite = adhesion.adherent?.derniereVisites?.find(visite => visite.user.id == this.tokenStorageService.getUser().id);
    let modif: Notification = new Notification;
    adhesion.adherent?.derniereModifs?.forEach(notif => {
      if(modif.id == 0){
        modif = notif
      }else if(Date.parse(modif.date) < Date.parse(notif.date)){
        modif = notif
      }
    });

    if(visite != undefined && (modif.id == 0 || Date.parse(modif.date) < Date.parse(visite.date))){
      return false
    }

    return true;
  }
}
