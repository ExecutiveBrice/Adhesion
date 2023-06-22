import { Component, OnInit } from '@angular/core';
import { ActiviteDropDown, Adhesion, AdhesionExcel, Document, HoraireDropDown } from 'src/app/models';
import { AdhesionService } from 'src/app/_services/adhesion.service';
import { faEarth, faBasketball, faFilterCircleXmark, faFilter, faPen, faEye, faEnvelope, faSkull, faFlag, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { ParamService } from 'src/app/_services/param.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { FilterAdhesionByPipe } from 'src/app/_helpers/filterAdhesion.pipe';
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

  constructor(
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

    } else {
      this.router.navigate(['login']);
    }



    this.adhesionService.getAll().subscribe(
      data => {

        this.cloned = data.map(x => Object.assign({}, x));

        this.adhesions = data;
        if (this.tokenStorageService.getUser().username == "master_yoda@hotmail.com") {
          this.addFiltre('activite.groupe', 'ALOD_B')
        }

      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
    this.getActivites()
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
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }


  updatePaiementSecretariat(adhesion: Adhesion, tarif: number | null, dateReglement: Date | undefined, statut: boolean) {
    this.adhesionService.updatePaiementSecretariat(adhesion.id, tarif, dateReglement, statut).subscribe(
      data => {
        adhesion.validPaiementSecretariat = data.validPaiementSecretariat;
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
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }
  ajoutAccord(adhesion: Adhesion, nomAccord: string) {
    this.adhesionService.addAccord(adhesion.id, nomAccord).subscribe(
      data => {

        adhesion.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }
  retraitAccord(adhesion: Adhesion, nomAccord: string) {
    this.adhesionService.removeAccord(adhesion.id, nomAccord).subscribe(
      data => {

        adhesion.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
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



        adhesion = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }

  
  addSearch(search: string) {
    if (search.length > 3) {
      this.addFiltre('adherent.prenom', search)
      this.addFiltre('adherent.nom', search)


    } else {
      this.removeFiltre('adherent.nom')
      this.removeFiltre('adherent.prenom')
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

    this.adhesions = this.filterAdhesionBy.transform(this.cloned.map(x => Object.assign({}, x)) , this.filtres)

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
    let datedebut: Date = new Date
    datedebut.setFullYear(datedebut.getFullYear(), datedebut.getMonth(),  datedebut.getDate())

    if(!adhesion.dateReglement){
      adhesion.dateReglement = datedebut;
    }
    this.selectedAdhesion = adhesion;

    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });


  }

  dismiss(selectedAdhesion: Adhesion) {
    this.modalService.dismissAll();
    selectedAdhesion.tarif = selectedAdhesion.activite ? selectedAdhesion.activite.tarif : 0;
    selectedAdhesion.validPaiementSecretariat = false;
  }
  dismissDoc() {
    this.modalService.dismissAll();

  }


  onSubmit() {
    this.modalService.dismissAll();
    this.updatePaiementSecretariat(this.selectedAdhesion, this.selectedAdhesion.tarif, this.selectedAdhesion.dateReglement, true)
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


      if (adhesion.activite)
        newAdhesionExcel.nomActivite = adhesion.activite.nom + " " + adhesion.activite.horaire


      newAdhesionExcel.flag = adhesion.flag
      newAdhesionExcel.remarqueSecretariat = adhesion.remarqueSecretariat
      newAdhesionExcel.statutActuel = adhesion.statutActuel
      newAdhesionExcel.tarif = adhesion.tarif
      newAdhesionExcel.validDocumentSecretariat = adhesion.validDocumentSecretariat
      newAdhesionExcel.typeReglement = adhesion.typeReglement
      newAdhesionExcel.validPaiementSecretariat = adhesion.validPaiementSecretariat

      adhesions.push(newAdhesionExcel)
    })




    this.excelService.exportAsExcelFile(adhesions, 'adhesions');
  }
}
