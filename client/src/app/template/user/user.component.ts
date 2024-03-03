import { Component, OnInit, inject, Input } from '@angular/core';
import { Accord, Activite, ActiviteDropDown, Adherent, Adhesion, Document, HoraireDropDown, Tribu } from '../../models';

import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { faCirclePause, faClock, faPiggyBank, faSkull, faFileSignature, faSquareCaretLeft, faSquareCaretDown, faEye, faCircleQuestion, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
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
  activitesMajeur: ActiviteDropDown[] = [];
  activitesMineur: ActiviteDropDown[] = [];
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

  subscription = new Subscription()

  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  showHelloAsso: boolean | null = false;

  // Default export is a4 paper, portrait, using millimeters for units
  doc: jsPDF = new jsPDF('p', 'mm', 'a4', true);

  constructor(
    private toastr: ToastrService,
    private adherentService: AdherentService,
    private adhesionService: AdhesionService,
    public activiteService: ActiviteService,
    public utilService: UtilService,
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
    this.toastr.warning('Attention', message);
  }
  showError(message: string) {
    this.toastr.error('Erreur', "Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message);
  }

  windows_width: number = 0;
  ngOnInit(): void {
console.log(this.tribu)
    this.adultes = this.tribu.adherents.filter(adh => adh.mineur == false && adh.id != this.adherent.id);
    this.adherent.user.username = this.adherent.user.username.endsWith('mailfictif.com')?'':this.adherent.user.username;
    if(this.isRepresentant){
      this.adherent.mineur = false;
    }
    console.log(this.adherent)
    console.log(window.innerWidth)
    this.windows_width = window.innerWidth;
    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }
    this.fillObjects();
    this.fillFiles();
  }



  chooseAdherent(adherent: Adherent) {
    console.log(adherent)
    this.adherent.representant = adherent;
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


  choixActivites(adherent: Adherent) {
    console.log(adherent)
    let activites: ActiviteDropDown[];
    if (adherent.mineur) {
      activites = this.activitesMineur;
    } else {
      activites = this.activitesMajeur;
    }
    activites.forEach(activite => activite.horaires.forEach(horaire => {
      horaire.dejaInscrit = false;
    }))

    activites.forEach(activite => activite.horaires.forEach(horaire => {
      adherent.adhesions.forEach(adhesion => {
        if (adhesion.activite?.id == horaire.id) {
          horaire.dejaInscrit = true;
        }
      })
    }))

    this.utilService.openModalActivite(activites).then((data) => {
      console.log(data)
      // on close
      this.choisir(adherent, data);
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });

  }

  activites: Activite[] = []
  fillObjects() {
    let activiteBasket = new ActiviteDropDown
    activiteBasket.nom = "Basket"
    let horaireDropDownCompetition = new HoraireDropDown
    horaireDropDownCompetition.id = 999
    horaireDropDownCompetition.nom = "Compétition"
    horaireDropDownCompetition.complete = false
    activiteBasket.horaires.push(horaireDropDownCompetition)
    this.activitesMineur.push(activiteBasket)

    let activiteBasketMajeur = new ActiviteDropDown
    activiteBasketMajeur.nom = "Basket"
    let horaireDropDownMCompetition = new HoraireDropDown
    horaireDropDownMCompetition.id = 999
    horaireDropDownMCompetition.nom = "Compétition"
    horaireDropDownMCompetition.complete = false
    activiteBasketMajeur.horaires.push(horaireDropDownMCompetition)

    let horaireDropDownLoisir = new HoraireDropDown
    horaireDropDownLoisir.id = 998
    horaireDropDownLoisir.nom = "Loisir"
    horaireDropDownLoisir.complete = false
    activiteBasketMajeur.horaires.push(horaireDropDownLoisir)

    let horaireDropDownDirigeant = new HoraireDropDown
    horaireDropDownDirigeant.id = 997
    horaireDropDownDirigeant.nom = "Dirigeant Joueur"
    horaireDropDownDirigeant.complete = false
    activiteBasketMajeur.horaires.push(horaireDropDownDirigeant)

    let horaireDropDownDirigeantNon = new HoraireDropDown
    horaireDropDownDirigeantNon.id = 996
    horaireDropDownDirigeantNon.nom = "Dirigeant Non Joueur"
    horaireDropDownDirigeantNon.complete = false
    activiteBasketMajeur.horaires.push(horaireDropDownDirigeantNon)

    this.activitesMajeur.push(activiteBasketMajeur)

    this.activiteService.getAll().subscribe(
      data => {
        this.activites = data;
        data.forEach(act => {

          if (act.groupe == "ALOD_G") {
            if (act.pourEnfant == false || act.pourEnfant == null) {
              if (this.activitesMajeur.filter(activiteDropDown => activiteDropDown.nom == act.nom).length > 0) {
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                horaireDropDown.complete = act.complete
                this.activitesMajeur.filter(activiteDropDown => activiteDropDown.nom == act.nom)[0].horaires.push(horaireDropDown)
              } else {
                let activiteDropDown = new ActiviteDropDown()

                activiteDropDown.nom = act.nom
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                horaireDropDown.complete = act.complete
                activiteDropDown.horaires.push(horaireDropDown)
                this.activitesMajeur.push(activiteDropDown)
              }
            } else if (act.pourEnfant == true || act.pourEnfant == null) {
              if (this.activitesMineur.filter(activiteDropDown => activiteDropDown.nom == act.nom).length > 0) {
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                horaireDropDown.complete = act.complete
                this.activitesMineur.filter(activiteDropDown => activiteDropDown.nom == act.nom)[0].horaires.push(horaireDropDown)
              } else {
                let activiteDropDown = new ActiviteDropDown()
                activiteDropDown.nom = act.nom
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                horaireDropDown.complete = act.complete
                activiteDropDown.horaires.push(horaireDropDown)
                this.activitesMineur.push(activiteDropDown)
              }
            }
          }
        });
        console.log(this.activitesMajeur)
        console.log(this.activitesMineur)
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  onSubmit(adherent: Adherent): void {
    adherent.telephone = this.cleaning(adherent.telephone)
    adherent.user.username = this.cleaning(adherent.user.username)
    console.log(adherent)
    this.adherentService.update(adherent).subscribe(
      data => {
        this.adherent = data;
        console.log(data)
        if(this.isRepresentant){
          this.activeModal.close(data)
        }
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }


  choisir(adherent: Adherent, activiteId: number) {
    let newAdhesionActivite: Activite = new Activite;
    if (adherent.naissance && activiteId == 999) {
      let anneeRef = new Date().getFullYear()
      let anneeAdh = new Date(adherent.naissance).getFullYear()

      let age = anneeRef - anneeAdh
      if (age < 7) {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "U7")[0]
      } else if (age < 9) {
        if (adherent.genre == "Masculin") {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U9M")[0]
        } else {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U9F")[0]
        }
      } else if (age < 11) {
        if (adherent.genre == "Masculin") {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U11M")[0]
        } else {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U11F")[0]
        }
      } else if (age < 13) {
        if (adherent.genre == "Masculin") {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U13M")[0]
        } else {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U13F")[0]
        }
      } else if (age < 15) {
        if (adherent.genre == "Masculin") {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U15M")[0]
        } else {
          newAdhesionActivite = this.activites.filter(act => act.horaire == "U15F")[0]
        }
      } else if (age < 17 && adherent.genre == "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "U17M")[0]
      } else if (age < 18 && adherent.genre != "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "U18F")[0]
      } else if (age < 20 && adherent.genre == "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "U20M")[0]
      } else if (adherent.genre == "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "Senior M")[0]
      } else if (adherent.genre != "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "Senior F")[0]
      }
    } else if (activiteId == 998) {
      if (adherent.genre == "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "Loisir M")[0]
      } else if (adherent.genre != "Masculin") {
        newAdhesionActivite = this.activites.filter(act => act.horaire == "Loisir F")[0]
      }
    } else if (activiteId == 997) {
      newAdhesionActivite = this.activites.filter(act => act.horaire == "Dirigeant Joueur")[0]
    } else if (activiteId == 996) {
      newAdhesionActivite = this.activites.filter(act => act.horaire == "Dirigeant Non Joueur")[0]
    } else {
      newAdhesionActivite = this.activites.filter(act => act.id == activiteId)[0]
    }

    this.adhesionService.add(adherent.id, newAdhesionActivite.id).subscribe(
      adhesionBdd => {
        adherent.adhesions.push(adhesionBdd)
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  updateAccord(accord: Accord, etat: boolean) {

    if (accord.nom == 'Reglement Interieur') {
      this.adhesions.forEach(adhesion => {
        accord.etat = etat;
        accord.datePassage = new Date
      })
    } else {
      accord.etat = etat;
      accord.datePassage = new Date
    }
  }

  isAccordRI(accords: Accord[]): boolean {
    return accords.filter(acc => acc.nom == "RGPD" && acc.etat == null).length == 0
  }

  isAllAccordsDone(accords: Accord[]): boolean {
    return accords.filter(acc => acc.etat == null).length == 0
  }

  isAdherentComplet(adherent: Adherent) {
    if (adherent.mineur && (adherent.representant != null && ((adherent.emailRepresentant || !adherent.emailRepresentant && adherent.user.username && adherent.user.username.length > 0) &&
      (adherent.telephoneRepresentant || !adherent.telephoneRepresentant && adherent.telephone && adherent.telephone.length > 0) &&
      (adherent.adresseRepresentant || !adherent.adresseRepresentant && adherent.adresse && adherent.adresse.length > 0))
    )) {
      return true;
    }
    else if (adherent.user.username && adherent.user.username.length > 0 && adherent.telephone && adherent.telephone.length > 0 && adherent.adresse && adherent.adresse.length > 0) {
      return true;
    }
    return false;

  }


  updateAdhesion(adhesion: Adhesion) {
    this.adhesionService.validation(adhesion.accords, adhesion.id).subscribe(
      data => {
        adhesion.statutActuel = data.statutActuel;
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }

  deleteAdhesion(adhesion: Adhesion) {
    this.newAdhesions = this.newAdhesions.filter(adh => adh.id != adhesion.id)
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
  fillFiles(){
    this.fileService.getAllFilesName(this.adherent.id).subscribe(data => {
      this.adherent.documents = data
       console.log(data)
      },
      error => {
        this.isFailed = true;
        this.showError(error.message)
      }
    );
  }
  openEditModal(doc: string) {
    console.log(doc)
    this.utilService.openModalPDF(doc, this.adherent.id).then((data) => {
      console.log(data)
      // on close
    }, (reason) => { 
      console.log(reason)
      // on dismiss
    });
  }

  openAddPDFModal(adherent: Adherent) {
    this.utilService.openModalPDF(undefined, this.adherent.id).then((data:Document) => {
      console.log(data)
      this.uploadPDF(data)
      // on close
    }, (reason) => {
      console.log(reason)
      // on dismiss
    });
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

  




  openPDF(adhesion: Adhesion) {

    if (adhesion.adherent && adhesion.activite) {

      let prix = 0
      adhesion.paiements.forEach(paiement => prix += paiement.montant)
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
      this.doc.text("atteste que " + adhesion.adherent.prenom + " " + adhesion.adherent.nom + " né le " + this.datePipe.transform(adhesion.adherent.naissance, 'dd/MM/yyyy'), 10, 130);
      this.doc.text("résident au " + adhesion.adherent.representant?.adresse ? adhesion.adherent.representant.adresse : adhesion.adherent.adresse, 10, 140);
      this.doc.text("est inscrit à l'activité " + adhesion.activite.nom + " pour l'année scolaire 2023/2024", 10, 150);
      this.doc.text("et est à jour de sa cotisation de " + prix + "€ payée par " + adhesion.paiements[0].typeReglement, 10, 160);

      this.doc.addImage("assets/signature.png", "JPEG", 100, 200, 77, 43);

      this.doc.save("Attestation_" + adhesion.adherent.prenom + "_" + adhesion.adherent.nom + "_20232024.pdf");
    }
    this.doc = new jsPDF('p', 'mm', 'a4', true);
  }

  adhesionValide(adh: Adhesion): boolean {
    return adh.statutActuel.startsWith("Attente licence en ligne")
      || adh.statutActuel.startsWith("Validée")
      || adh.statutActuel.startsWith("Licence générée")
      || adh.statutActuel.startsWith("Retour Comité")
      || adh.statutActuel.startsWith("Licence T");
  }

  openAccordModal(accord: Accord, consultation: boolean) {

    this.utilService.openModal(
      accord.text, accord.title, true, accord.refusable ? true : false, consultation, "lg").then((reponse) => {
        console.log(reponse)
        if (reponse == "accepter") {
          this.updateAccord(accord, true)
        }
        if (reponse == "refuser") {
          this.updateAccord(accord, false)
        }
        // on close
      }, (reason) => {
        console.log(reason)
        // on dismiss
      });
  }


  suppressionAdhesionModal(adhesion: Adhesion) {
    this.utilService.openModal(
      "Vous êtes sur le point de supprimer l'adhesion de " + adhesion.adherent.prenom + " au " +
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
        adhesion.accords = data;
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
}