import { Component, OnInit, inject, Input } from '@angular/core';
import { Accord, Activite, ActiviteDropDown, Adherent, Adhesion, Document, HoraireDropDown, Tribu } from '../../models';

import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { faRefresh, faCirclePause, faClock, faPiggyBank, faSkull, faFileSignature, faSquareCaretLeft, faSquareCaretDown, faEye, faCircleQuestion, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { AdhesionService } from 'src/app/_services/adhesion.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { jsPDF } from "jspdf";
import { DatePipe } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { UtilService } from 'src/app/_services/util.service';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FileService } from 'src/app/_services/file.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';


@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})
export class UserComponent implements OnInit {


  @Input()
  adherent!: Adherent;
  @Input()
  isRepresentant!: boolean;
  @Input()
  tribu!: Tribu;

  activeModal = inject(NgbActiveModal);
  faRefresh = faRefresh;
  faClock = faClock;
  faCirclePause = faCirclePause;
  faPiggyBank = faPiggyBank;
  faFileSignature = faFileSignature;
  faSkull = faSkull;
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
  adultes: Adherent[] = []
  editAdhRefActivite?: boolean
  openActivites: boolean = false;
  activitesListe: ActiviteDropDown[] = [];
  newAdhesions: Adhesion[] = [];
  adhesions: Adhesion[] = [];
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
  isOpen: boolean = false;
  isInscriptionOpen: boolean = false;
  subscription = new Subscription()

  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  showHelloAsso: boolean | null = false;

  activites: Activite[] = []
  // Default export is a4 paper, portrait, using millimeters for units
  doc: jsPDF = new jsPDF('p', 'mm', 'a4', true);

  constructor(
    private toastr: ToastrService,
    private adherentService: AdherentService,
    private adhesionService: AdhesionService,
    public activiteService: ActiviteService,
    public utilService: UtilService,
    private tokenStorageService: TokenStorageService,
    public router: Router,
    public route: ActivatedRoute,
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
    this.toastr.warning(message, 'Attention');
  }
  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }

  windows_width: number = 0;
  ngOnInit(): void {

    this.paramService.getAllBoolean().subscribe({
      next: (data) => {
        this.isInscriptionOpen = data.filter(param => param.paramName == "Inscription")[0].paramValue;
        this.isOpen = data.filter(param => param.paramName == "Ouvert")[0].paramValue;
      },
      error: (error) => {
        this.isInscriptionOpen = false;
        this.isOpen = false;
      }
    });

    this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
    this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');


    this.adultes = this.tribu.adherents.filter(adh => adh.mineur == false && adh.representant == null && adh.id != this.adherent.id);

    if (this.isRepresentant) {
      this.adherent.mineur = false;
    }

    console.log(window.innerWidth)
    this.windows_width = window.innerWidth;
    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }

    this.activiteService.fillObjects(this.activites, this.activitesListe, this.adherent);
    console.log(this.activites)
    this.fillFiles();
  }



  chooseAdherent(adherent: Adherent) {
    this.adherent.representant = adherent;
    this.adherent.emailRepresentant = true;
    this.adherent.adresseRepresentant = true;
    this.adherent.telephoneRepresentant = true;
  }

  private modalService = inject(NgbModal);
  newAdherent() {
    this.adherentService.newAdherentDansTribu(this.tribu.uuid).subscribe({
      next: (data) => {
        const modalRef = this.modalService.open(UserComponent, {
          size: 'xl',
          centered: true,
          backdrop: 'static',
          scrollable: true
        });

        modalRef.componentInstance.tribu = this.tribu;
        modalRef.componentInstance.adherent = data;
        modalRef.componentInstance.isRepresentant = true;
        modalRef.result.then((data) => {
          console.log(data)
          this.adherent.representant = data
          // on close
        }, (reason) => {
          console.log(reason)
          // on dismiss
        });
      },
      error: (error) => {
        this.isFailed = true;
        this.showError(error.message)
      }
    });
  }

  changeActivite(adhesion: Adhesion, activiteId: number) {
    this.adhesionService.changeActivite(adhesion.id, activiteId).subscribe({
      next: (data) => {
        this.showSuccess("Changement d'activité réussie pour l'adhérent "+this.adherent.prenom +" "+this.adherent.nom)
        adhesion.activite = data;
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });
  }

  choixActivites(adherent: Adherent) {
    console.log(this.activitesListe)
    this.utilService.openModalChoixActivite(this.activitesListe, this.showAdmin, this.showSecretaire).then((data) => {
      console.log(data)
      // on close
      this.choisir(adherent, data);
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });

  }


  udpadteMineur(adherent: Adherent) {
    let anneeRef = new Date().getFullYear()
    let anneeAdh = new Date(adherent.naissance).getFullYear()

    let age = anneeRef - anneeAdh
    if (age >= 18) {
      adherent.mineur = false
    } else {
      adherent.mineur = true
    }
  }

  onSubmit(adherent: Adherent): void {
    console.log(adherent)
    if(adherent.mineur && this.tribu.adherents.length == 1){
      this.toastr.error("Le premier adhérent doit être majeur", 'Erreur')
    }else if (adherent.mineur && !adherent.representant) {
      this.toastr.error("L'adhérent mineur doit avoir un représentant majeur", 'Erreur')
    } else {
      console.log(adherent)
      adherent.telephone = this.cleaning(adherent.telephone)
      if(adherent.user != null){
        adherent.user.username = this.cleaning(adherent.user.username)
      }

      this.adherentService.update(adherent).subscribe(
        data => {
          this.showSuccess("L'adhérent à bien été mis à jour")
          this.adherent = data;
          this.activites = []
          this.activitesListe = []
          this.activiteService.fillObjects(this.activites, this.activitesListe, this.adherent);
          if (this.isRepresentant) {
            this.activeModal.close(data)
          }
        },
        error => {
          console.log(error)
          this.isFailed = true;
          if (error.status == 409) {
            this.toastr.error("Cette adresse e-mail est déjà utilisée. Veuillez en choisir une autre", 'Erreur')
          } else {
            this.showError(error.error)
          }



        }
      );
    }
  }


  choisir(adherent: Adherent, activiteId: number) {
    this.adhesionService.add(adherent.id, activiteId).subscribe(
      adhesionBdd => {
        this.showSuccess("L'adhésion à bien été ajoutée, veuillez valider les accords en enregistrant l'adhésion")
        adherent.adhesions.push(adhesionBdd)
        this.activitesListe.find(adh => adh.nom == adhesionBdd.activite?.nom)!.horaires.find(hor => hor.id == activiteId)!.dejaInscrit = true
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }


  isAllAccordsDone(accords: Accord[]): boolean {
    return accords.filter(acc => acc.etat == null).length == 0
  }

  isAdherentComplet(adherent: Adherent) {
    if (adherent.representant != null && ((adherent.emailRepresentant || !adherent.emailRepresentant && adherent.user.username && adherent.user.username.length > 0) &&
      (adherent.telephoneRepresentant || !adherent.telephoneRepresentant && adherent.telephone && adherent.telephone.length > 0) &&
      (adherent.adresseRepresentant || !adherent.adresseRepresentant && adherent.adresse && adherent.adresse.length > 0))
    ) {
      return true;
    }
    else if (adherent.user.username && adherent.user.username.length > 0 && adherent.telephone && adherent.telephone.length > 0 && adherent.adresse && adherent.adresse.length > 0) {
      return true;
    }
    return false;
  }



  updateDejaLicencie(adhesion: Adhesion) {

    setTimeout(() => {
      this.adhesionService.updateDejaLicencie(adhesion.id, adhesion.dejaLicencie!).subscribe(
        data => {
          adhesion.statutActuel = data.statutActuel;
          adhesion.accords = data.accords;
        },
        error => {
          this.isFailed = true;
          this.showError(error.message)
        }
      );
    }, 100);
  }

  updateAdhesion(adhesion: Adhesion) {

    this.adhesionService.validation(adhesion.accords, adhesion.id).subscribe(
      data => {
        this.showSuccess("L'adhésion à bien été enregistrée, vous pouvez réaliser le paiement")
        adhesion.statutActuel = data.statutActuel;
        adhesion.accords = data.accords;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );

  }

  openAttestationSante() {
    window.open('https://www.alod.fr/wp-content/uploads/2022/04/INSCRIPTION-Questionnaire_de_sante.pdf', '_blank');
  }

  openCertifMed(activite: Activite) {
    if (activite.groupe == "ALOD_G") {
      window.open('https://cd.ufolep.org/vienne/vienne_d/data_1/pdf/ce/certificatmdicalufolep86.pdf', '_blank');
    } else {
      window.open('https://www.alod.fr/wp-content/uploads/2023/05/certif20232024.pdf', '_blank');
    }
  }

  openSurclassement() {
    window.open('https://www.alod.fr/wp-content/uploads/2023/05/Certificat-surclassement-2023-2024.pdf', '_blank');
  }



  deleteDoc(file: Document) {
    this.fileService.delete(this.adherent.id, file.nom).subscribe(
      data => {
        this.adherent.documents = this.adherent.documents.filter(document => document != file.nom)
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }
  fillFiles() {
    this.fileService.getAllFilesName(this.adherent.id).subscribe(data => {
      this.adherent.documents = data
    },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }
  openEditModal(doc: string) {
    this.utilService.openModalPDF(doc, this.adherent.id).then((data) => {
      console.log(data)
      // on close
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });
  }

  openAddPDFModal(adherent: Adherent) {
    this.utilService.openModalPDF(undefined, this.adherent.id).then((data: Document) => {
      console.log(data)
      this.uploadPDF(data)
      // on close
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });
  }

  downloadPDF(file: string) {
    this.fileService.get(this.adherent.id, file).subscribe(res => {

      //window.open("data:application/pdf;base64," + data, '_self');
      let url = window.URL.createObjectURL(this.b64toBlob(res));
      let a = document.createElement('a');
      document.body.appendChild(a);
      a.setAttribute('style', 'display: none');
      a.href = url;
      a.download = file;
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
    },
      error => {
        console.log('😢 Oh no!', error);
      });

  }


  b64toBlob(b64Data: string, contentType = 'application/pdf', sliceSize = 512): Blob {
    const byteCharacters = atob(b64Data);
    const byteArrays = [];

    for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
      const slice = byteCharacters.slice(offset, offset + sliceSize);

      const byteNumbers = new Array(slice.length);
      for (let i = 0; i < slice.length; i++) {
        byteNumbers[i] = slice.charCodeAt(i);
      }

      const byteArray = new Uint8Array(byteNumbers);
      byteArrays.push(byteArray);
    }

    const blob = new Blob(byteArrays, { type: contentType });
    return blob;
  }

  uploadPDF(file: Document) {

    this.fileService.update(this.adherent.id, file.nom, file.content.replace("data:application/pdf;base64,", "")).subscribe(data => {
      console.log(data)
      if (this.adherent.documents.length > 0) {
        this.adherent.documents.push(file.nom)
      } else {
        let docs = []
        docs.push(file.nom)
        this.adherent.documents = docs;
      }
    },
      error => {
        console.log('😢 Oh no!', error);
      });
  }



  adhesionValide(adh: Adhesion): boolean {
    return adh.statutActuel.startsWith("Licence FFBB à compléter")
      || adh.statutActuel.startsWith("Validée")
      || adh.statutActuel.startsWith("Licence générée")
      || adh.statutActuel.startsWith("Retour Comité")
      || adh.statutActuel.startsWith("Licence T");
  }

  openAccordModal(accord: Accord, consultation: boolean) {

    this.utilService.openModal(
      accord.text, accord.title, true, accord.refusable ? true : false, consultation, "lg").then((reponse) => {
        console.log(reponse)
        // on close
      }, (reason) => {
        console.log(reason)
        // on dismiss
      });
  }


  suppressionAdhesionModal(adhesion: Adhesion) {
    this.utilService.openModal(
      "Vous êtes sur le point de supprimer l'adhesion de " + this.adherent.prenom + " au " +
      adhesion.activite?.nom + " " + adhesion.activite?.horaire, "Suppression de l'adhésion", true, true, false, "lg").then((data) => {
        console.log(data)
        this.acceptSupressAdhesion(adhesion);
        // on close
      }, (reason) => {
        console.log(reason)
        // on dismiss
      });
  }

  acceptSupressAdhesion(adhesion: Adhesion) {
    this.adhesionService.deleteAdhesion(adhesion.id).subscribe(
      data => {
        this.adherent.adhesions = this.adherent.adhesions.filter(adh => adh.id != adhesion.id)
        this.activitesListe.find(adh => adh.nom == adhesion.activite?.nom)!.horaires.find(hor => hor.id == adhesion.activite?.id)!.dejaInscrit = false
        this.showSuccess("Votre demande d'adhésion à bien été supprimée")
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    )
  }

  adherentEdit(adherent: Adherent) {
    adherent.edit = !adherent.edit
    if (adherent.edit) {
      this.adherentService.addVisite(adherent.id).subscribe({
        next: (response) => {
          console.log("adherent.edit")
        },
        error: (error) => {
          this.isFailed = true;
          this.showError(error.message)
        }
      });
    }
  }

  openConfirmModalAccordAdherent(adherent: Adherent, accord: Accord) {
    this.utilService.openModal(
      "Vous êtes sur le point de supprimer l'accord " + adherent.nom +
      " de " + adherent.prenom, "Suppression Accord", true, true, false, "lg").then((reponse) => {
        console.log(reponse)
        if (reponse == "accepter") {
          this.retraitAccord(adherent, accord)
        }
        // on close
      }, (reason) => {
        console.log(reason)
        // on dismiss
      });
  }


  cleaning(chaine: string) {
    return chaine?.toLowerCase().replaceAll(" ", "")
  }

  ajoutAccord(adherent: Adherent, nomAccord: string) {
    this.adherentService.addAccord(adherent.id, nomAccord).subscribe(
      data => {
        console.log(data)
        adherent.accords = data;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  retraitAccord(adherent: Adherent, accord: Accord) {

    this.adherentService.removeAccord(adherent.id, accord.nom).subscribe(
      data => {
        adherent.accords = data;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  ajoutAccordAdhesion(adhesion: Adhesion, nomAccord: string) {
    this.adhesionService.addAccord(adhesion.id, nomAccord).subscribe(
      data => {
        adhesion.accords = data.accords;
        adhesion.statutActuel = data.statutActuel
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  retraitAccordAdhesion(adhesion: Adhesion, accord: Accord) {
    this.adhesionService.removeAccord(adhesion.id, accord.nom).subscribe(
      data => {
        adhesion.accords = data;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }


  addSurclassement(adhesion: Adhesion, surClassement: Activite) {
    this.adhesionService.saveSurclassement(adhesion.id, surClassement.id).subscribe(
      data => {
        adhesion.surClassement = data.surClassement;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  removeSurclassement(adhesion: Adhesion) {
    this.adhesionService.deleteSurclassement(adhesion.id).subscribe(
      data => {
        adhesion.surClassement = undefined;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }


  regenerate(adherent: Adherent) {
    this.adherentService.regenerate(adherent.id).subscribe({
      next: (response) => {
        console.log(response)

        this.showSuccess("La régénération de l'attestation de l'adhérent est terminée")
      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    });

  }
}
