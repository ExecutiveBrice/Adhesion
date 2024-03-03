import { Component, OnInit, inject } from '@angular/core';
import { UserService } from '../../_services/user.service';
import { Accord, Activite, ActiviteDropDown, Adherent, Adhesion, Document, HoraireDropDown, Tribu, User } from '../../models';
import { NgbDateStruct, NgbCalendar, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { faCirclePause, faClock, faSquareXmark, faFileSignature, faSquareCaretLeft, faSquareCaretDown, faEye, faCircleQuestion, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { AdhesionService } from 'src/app/_services/adhesion.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { jsPDF } from "jspdf";
import { DatePipe } from '@angular/common';
import { TribuService } from 'src/app/_services/tribu.service';
import { ToastrService } from 'ngx-toastr';
import { UserComponent } from 'src/app/template/user/user.component';
import { UtilService } from 'src/app/_services/util.service';
import { FileService } from 'src/app/_services/file.service';

@Component({
  selector: 'app-board-user',
  templateUrl: './board-user.component.html',
  styleUrls: ['./board-user.component.css']
})
export class BoardUserComponent implements OnInit {
  faClock =faClock
  faCirclePause=faCirclePause
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
  adhesions: Adhesion[] = [];
  tribu: Tribu = new Tribu;
  helloassoAlod: boolean = false;
  helloassoAlod3X: boolean = false;
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

  showHelloAsso: boolean | null = false;

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
    private datePipe: DatePipe) { }

  showSuccess(message: string) {
    this.toastr.success(message, 'Information');
  }
  showSecretariat() {
    this.toastr.warning("Le secrétariat validera votre dossier lorsqu'il sera complet", "Secrétariat");
  }

  showWarning(message: string) {
    this.toastr.warning('Attention', message);
  }
  showError(message: string) {
    this.toastr.error('Erreur', "Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message);
  }




  ngOnInit(): void {

    this.paramService.getAllBoolean().subscribe({
      next: (data) => {
        this.showHelloAsso = data.filter(param => param.paramName == "Show_HelloAsso")[0].paramValue;
      },
      error: (error) => {
        this.showHelloAsso = false;
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

  supprimable(adherent:Adherent):boolean{
    if (this.tribu.adherents.filter(adh => !adh.mineur).length > 1){
      return true
    }
    if (adherent.mineur){
      return true;
    }
    return false;
  }

  adhesionComplete(adherent:Adherent):boolean{
    return adherent.adhesions.find(adh => adh.statutActuel == 'Validée') != undefined;
  }

  getTribu() {
    this.adhesions = []
    if (this.showSecretaire && this.tribuUuid) {
      this.tribuService.getTribuByUuid(this.tribuUuid).subscribe(
        data => {
          console.log(data)
          this.tribu = data
          this.tribu.adherents.forEach(adh => {
            adh.adhesions.forEach(adhesion => this.adhesions.push(adhesion))
          });
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
          this.tribu.adherents.forEach(adh => {
            adh.adhesions.forEach(adhesion => this.adhesions.push(adhesion))
          });
          this.calculRestantDu()
        },
        error => {
          this.isFailed = true;
          this.showError(error.message)
        }
      );
    }
  }

  adhesionsACompleter: number = 0
  calculCompletudeAdhesion() {
    this.adhesionsACompleter = 0;
    this.adhesionsACompleter += this.adhesions.filter(adh => adh.statutActuel == 'Attente validation adhérent').length
    if (this.adhesionsACompleter > 0) {
      this.showSecretariat()
      this.adhesionsOpen = true
    } else {
      this.adhesionsOpen = false
    }
  }

  adhesionPaiement: Adhesion[] = []
  calculRestantDu() {
    this.totalRestantDu = 0
    this.adhesionPaiement = []

    this.adhesions.forEach(adh => {
      if (!adh.validPaiementSecretariat && adh.statutActuel != 'Annulée' && adh.statutActuel != 'Liste d\'attente') {
        this.totalRestantDu = this.totalRestantDu + adh.tarif;
        this.adhesionPaiement.push(adh)
      }
    })
    if (this.totalRestantDu > 0) {
      this.PaiementsOpen = true
    }
  }

  afficheAlerte: boolean = false;
  afficheAlerteDonneesPerso() {
    this.afficheAlerte = true;
    setTimeout(() => { this.afficheAlerte = false }, 5000);
  }

  adhesionValide(adh: Adhesion): boolean {
    return adh.statutActuel.startsWith("Attente licence en ligne")
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
    this.adherentService.newAdherentDansTribu(this.tribu.uuid).subscribe({
      next: (data) => {
        this.tribu.adherents.push(data)
        this.showSuccess("Utilisteur bien ajouté, complétez le pour lui ajouter une activité")
      },
      error: (error) => {
        this.isFailed = true;
        this.showError(error.message)
      }
    });
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

  openPDF(adherent: Adherent) {

    const adhesionsValide = adherent.adhesions.filter(adh => adh.validPaiementSecretariat);
    let prix = 0
    adhesionsValide.forEach(adh => adh.paiements.forEach(paiement => prix += paiement.montant));
    if (adhesionsValide.length >= 1) {

      this.doc.setFontSize(7)
      this.doc.addImage("assets/logo.png", "JPEG", 15, 10, 52, 33);
      this.doc.text("Agrément à «Jeunesse et Sports» : le 27/08/1967 n° 44 S 42", 42, 50, { align: "center" });
      this.doc.text("Déclaration en Préfecture de Nantes : le 12/02/1960 n° 6669", 42, 55, { align: "center" });

      this.doc.setFontSize(12)
      this.doc.text("Fait à Rezé", 150, 15);
      this.doc.text("Le : " + this.datePipe.transform(new Date, 'dd/MM/yyyy'), 150, 20);

      this.doc.setFontSize(30)
      this.doc.text("Attestation", 105, 100, { align: "center" });

      this.doc.setFontSize(12)
      this.doc.text("Je soussigné MAURY Morgane en qualité de présidente de l'ALOD,", 10, 120);
      this.doc.text("atteste que " + adherent.prenom + " " + adherent.nom + " né le " + this.datePipe.transform(adherent.naissance, 'dd/MM/yyyy'), 10, 130);
      this.doc.text("résident au " + adherent.representant && adherent.representant?.adresse ? adherent.representant.adresse : adherent.adresse, 10, 140);

      if (adherent.adhesions.length == 1) {
        this.doc.text("est inscrit(e) à l'activité suivante pour l'année scolaire 2024/2025 : ", 10, 150);
      } else {
        this.doc.text("est inscrit(e) aux activités suivantes pour l'année scolaire 2024/2025 :", 10, 150);
      }

      let ligneText = 160;
      adherent.adhesions.forEach(adh => {
        this.doc.text(" -" + adh.activite?.nom, 10, ligneText);
        ligneText = ligneText + 10;
      })
      this.doc.text("et est à jour de sa cotisation de " + prix + "€", 10, ligneText);
      this.doc.addImage("assets/signature.png", "JPEG", 100, 230, 77, 43);

      this.doc.save("Attestation_" + adherent.prenom + "_" + adherent.nom + "_20232024.pdf");
    }
    this.doc = new jsPDF('p', 'mm', 'a4', true);
  }
 

}