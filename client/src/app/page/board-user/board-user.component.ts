import {Component, OnInit, inject} from '@angular/core';
import {UserService} from '../../_services/user.service';
import {
  Accord,
  Activite,
  ActiviteDropDown,
  Adherent,
  Adhesion,
  Document,
  HoraireDropDown,
  ParamText,
  Tribu,
  User
} from '../../models';
import {NgbDateStruct, NgbCalendar, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ActiviteService} from '../../_services/activite.service';
import {AdherentService} from 'src/app/_services/adherent.service';
import {
  faCirclePause,
  faClock,
  faSquareXmark,
  faFileSignature,
  faSquareCaretLeft,
  faSquareCaretDown,
  faEye,
  faCircleQuestion,
  faCircleXmark,
  faCloudDownloadAlt,
  faBook,
  faScaleBalanced,
  faPencilSquare,
  faSquarePlus,
  faSquareMinus,
  faCircleCheck,
  faUserPlus
} from '@fortawesome/free-solid-svg-icons';
import {AdhesionService} from 'src/app/_services/adhesion.service';
import {ActivatedRoute, Router} from '@angular/router';
import {TokenStorageService} from 'src/app/_services/token-storage.service';
import {Subscription} from 'rxjs';
import {ParamService} from 'src/app/_services/param.service';
import {jsPDF} from "jspdf";
import {DatePipe} from '@angular/common';
import {TribuService} from 'src/app/_services/tribu.service';
import {ToastrService} from 'ngx-toastr';
import {UserComponent} from 'src/app/template/user/user.component';
import {UtilService} from 'src/app/_services/util.service';
import {FileService} from 'src/app/_services/file.service';

@Component({
  selector: 'app-board-user',
  templateUrl: './board-user.component.html',
  styleUrls: ['./board-user.component.css']
})
export class BoardUserComponent implements OnInit {
  faClock = faClock
  faCirclePause = faCirclePause
  faFileSignature = faFileSignature;
  faSquareXmark = faSquareXmark;
  faSquareCaretLeft = faSquareCaretLeft
  faSquareCaretDown = faSquareCaretDown
  faEye = faEye
  faCircleQuestion = faCircleQuestion;
  faCircleXmark = faCircleXmark;
  faCloudDownloadAlt = faCloudDownloadAlt;
  faScaleBalanced = faScaleBalanced;
  faBook = faBook;
  faUserPlus = faUserPlus;
  faCircleCheck = faCircleCheck;
  faSquareMinus = faSquareMinus;
  faPencilSquare = faPencilSquare;
  faSquarePlus = faSquarePlus;

  date: { year: number; month: number } | undefined;
  content?: string;
  edit?: boolean
  swticj: boolean = false
  editAdhRefActivite?: boolean

  activitesMajeur: ActiviteDropDown[] = [];
  activitesMineur: ActiviteDropDown[] = [];
  newAdhesions: Adhesion[] = [];

  tribu: Tribu = new Tribu;
  helloassoAlod: boolean = false;
  helloassoAlod3X: boolean = false;

  helloassoAlodMajoration: boolean = false;
  helloassoAlod3XMajoration: boolean = false;

  testRgpd: boolean = false
  isFailed = false;
  validSecretariat: boolean = false;
  validDossier: boolean = false;
  mobile: boolean = false;
  dossierIncomplet = false;
  adherentsOpen = false
  adhesionsOpen = false
  PaiementsOpen = false
  totalRestantDu = 0;
  tribuUuid: string = "";
  subscription = new Subscription()

  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  showHelloAsso?: boolean = false;
  showHelloAsso3X?: boolean = false;

  // Default export is a4 paper, portrait, using millimeters for units
  doc: jsPDF = new jsPDF('p', 'mm', 'a4', true);

  constructor(
    private toastr: ToastrService,
    private tribuService: TribuService,
    private adherentService: AdherentService,
    private utilService: UtilService,
    public activiteService: ActiviteService,
    public router: Router,
    public route: ActivatedRoute,
    private tokenStorageService: TokenStorageService,
    public paramService: ParamService,
    public fileService: FileService,
    private datePipe: DatePipe) {
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
    this.getAllText()
    this.paramService.getAllBoolean().subscribe({
      next: (data) => {
        this.showHelloAsso = data.filter(param => param.paramName == "Show_HelloAsso")[0].paramValue;
        this.showHelloAsso3X = data.filter(param => param.paramName == "Show_HelloAsso3X")[0].paramValue;
      },
      error: (error) => {
        this.showHelloAsso = false;
        this.showHelloAsso3X = false;
      }
    });

    this.paramService.isClose()
      .subscribe({
        next: (data) => {
          if (data) {
            this.router.navigate(['login']);
          }
        },
        error: (error) => {
          this.isFailed = true;
          this.showError(error.message)
        }
      });

    this.paramService.getAllText().subscribe({
      next: (data) => {
        this.showSuccess(data.filter(param => param.paramName == "Text_MonAdhesion")[0].paramValue)
      },
      error: (error) => {
        this.isFailed = true;
        this.showError(error.message)

      }
    });

    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }

    const uuid = this.route.snapshot.paramMap.get('tribuUuid');
    this.tribuUuid = uuid != null ? uuid : "";

    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
      this.getTribu()
    } else {
      this.router.navigate(['login']);
    }
  }

  ajoutPossible() {
    if (this.tribu.adherents.filter(adh => !adh.completAdhesion).length > 0) {
      return false
    } else {
      return true
    }
  }

  supprimable(adherent: Adherent): boolean {
    if (this.showAdmin) {
      return true
    }
    if (this.tribu.adherents.length == 1) {
      return false
    }
    if (!adherent.completAdhesion) {
      return true
    }
    if (!adherent.mineur && this.tribu.adherents.filter(adh => !adh.mineur && adh.completAdhesion).length == 1) {
      return false
    }
    return true;
  }

  adhesionComplete(adherent: Adherent): boolean {
    return adherent.adhesions.find(adh => adh.statutActuel == 'Validée') != undefined;
  }

  getTribu() {

    if (this.showSecretaire && this.tribuUuid) {
      this.tribuService.getTribuByUuid(this.tribuUuid).subscribe(
        data => {
          console.log(data)
          this.tribu = data

          this.calculRestantDu()
        },
        error => {
          this.isFailed = true;
          this.showError(error.message)
        }
      );
    } else {
      this.tribuService.getConnected().subscribe(
        data => {
          console.log(data)
          this.tribu = data

          this.calculRestantDu()
        },
        error => {
          this.isFailed = true;
          this.showError(error.message)
        }
      );
    }
  }

// 4ec4076ae13f41a38c78f6c15e67cc96@developer.sumup.com

//vjcskSV3KDdhbbWEjvNJ


  adhesionPaiement: Adhesion[] = []
  majoration: boolean = false;

  calculRestantDu() {
    this.totalRestantDu = 0
    this.adhesionPaiement = []

    this.tribu.adherents.forEach(adher => {
      adher.adhesions.forEach(adhes => {
        if (!adhes.validPaiementSecretariat && adhes.statutActuel != 'Annulée' && adhes.statutActuel != 'Sur liste d\'attente' && adhes.statutActuel != 'Attente validation adhérent') {
          this.totalRestantDu = this.totalRestantDu + adhes.tarif;

          adhes.adherent = new Adherent()
          adhes.adherent.prenom = adher.prenom

          if (adhes.majoration == true) {
            this.majoration = true
          }

          this.adhesionPaiement.push(adhes)
        }
      })
    })
    if (this.totalRestantDu > 0) {
      this.PaiementsOpen = true
    }
  }

  afficheAlerte: boolean = false;

  afficheAlerteDonneesPerso() {
    this.afficheAlerte = true;
    setTimeout(() => {
      this.afficheAlerte = false
    }, 5000);
  }

  adhesionValide(adh: Adhesion): boolean {
    return adh.statutActuel.startsWith("Licence FFBB à compléter")
      || adh.statutActuel.startsWith("Validée")
      || adh.statutActuel.startsWith("Licence générée")
      || adh.statutActuel.startsWith("Retour Comité")
      || adh.statutActuel.startsWith("Licence T");
  }

  supressionAdherent(adherent: Adherent) {
    this.utilService.openModal(
      "Vous êtes sur le point de supprimer l'adhérent " + adherent.prenom + "<br/>" +
      (adherent.adhesions != undefined && adherent.adhesions.length > 0 ? "Toutes ses adhésions seront également supprimées" : "")
      , "Suppression", true, true, false, "md").then((reponse) => {
      console.log(reponse)
      if (reponse == "accepter") {
        this.acceptSupressAdherent(adherent);
      }
      // on close
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });
  }

  acceptSupressAdherent(adherent: Adherent) {
    this.adherentService.deleteAdherent(adherent.id).subscribe(
      data => {
        this.tribu.adherents = this.tribu.adherents.filter(adh => adh.id != adherent.id);
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    )
  }

  addAdherent() {
    let adherent = new Adherent();
    adherent.user = new User();
    adherent.mineur = false;
    adherent.completAdhesion = false;
    adherent.tribuId = this.tribu.uuid;
    let accord = new Accord()
    accord.nom = "RGPD";
    accord.text = this.paramTexts.filter(param => param.paramName == "RGPD")[0].paramValue;
    accord.text = this.paramTexts.filter(param => param.paramName == "RGPD")[0].paramValue;
    accord.etat = true;
    accord.refusable = false;
    accord.title = "RGPD";
    accord.valide = "J'accepte l'utilisation de mes données personnelles"
    adherent.accords.push(accord);


    let accordImage = new Accord()
    accordImage.nom = "DroitImage";
    accordImage.text = this.paramTexts.filter(param => param.paramName == "DroitImage")[0].paramValue;
    accordImage.etat = true;
    accordImage.refusable = true;
    accordImage.title = "DroitImage";
    accordImage.valide = "J'accèpte l'utilisation de mon image"
    accordImage.refus = "Je refuse l'utilisation de mon image";
    adherent.accords.push(accordImage);
    this.tribu.adherents.push(adherent)
    this.openAdherent(adherent)
  }

  paramTexts: ParamText[] = [];

  getAllText() {
    this.paramService.getAllText().subscribe(
      data => {
        this.paramTexts = data;

      },
      err => {
        ;
      }
    );
  }

  private modalService = inject(NgbModal);

  openAdherent(adherent: Adherent) {

    const modalRef = this.modalService.open(UserComponent, {
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.tribu = this.tribu;
    modalRef.componentInstance.adherent = adherent;


    modalRef.result.then((data) => {
      console.log(data)
      this.getTribu();
      console.log(data)
      // on close
    }, (reason) => {
      console.log(reason)
      this.getTribu();
      // on dismiss
    });
  }


}
