import { Component, OnInit, ViewChild } from '@angular/core';
import { UserService } from '../../_services/user.service';
import { Accord, Activite, ActiviteDropDown, Adherent, Adhesion, Document, HoraireDropDown, Tribu, User } from '../../models';
import { NgbDateStruct, NgbCalendar, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { faSkull, faSquareCaretLeft, faSquareCaretDown, faEye, faCircleQuestion, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { AdhesionService } from 'src/app/_services/adhesion.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { jsPDF } from "jspdf";
import { DatePipe } from '@angular/common';
import { ModalConfig } from '../modal/modal.config'
import { ModalComponent } from '../modal/modal.component';
import { TribuService } from 'src/app/_services/tribu.service';

@Component({
  selector: 'app-board-user',
  templateUrl: './board-user.component.html',
  styleUrls: ['./board-user.component.css']
})
export class BoardUserComponent implements OnInit {

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
  errorMessage = "Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />";
  subscription = new Subscription()
  message: string = ""
  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  showHelloAsso: boolean | null = false;

  // Default export is a4 paper, portrait, using millimeters for units
  doc: jsPDF = new jsPDF('p', 'mm', 'a4', true);

  constructor(
    private tribuService: TribuService,
    private adherentService: AdherentService,
    private adhesionService: AdhesionService,
    public activiteService: ActiviteService,
    public router: Router,
    public route: ActivatedRoute,
    private tokenStorageService: TokenStorageService,
    public paramService: ParamService,
    private modalService: NgbModal,
    private datePipe: DatePipe) { }


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
          this.message = this.message + error.message + "<br />"
        }
      });

    this.paramService.getAllText().subscribe({
      next: (data) => {
        this.message = data.filter(param => param.paramName == "Text_MonAdhesion")[0].paramValue;
      },
      error: (error) => {
        this.isFailed = true;
        this.message = this.message + error.message + "<br />"
      }
    });

    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }
    let tribuUuid = this.route.snapshot.paramMap.get('tribuUuid');

    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
console.log(tribuUuid)
      if (this.showSecretaire && tribuUuid ) {
        this.tribuService.getTribuByUuid(tribuUuid).subscribe(
          data => {
           this.fillUser(data)
          },
          err => {
            this.isFailed = true;
            this.message = this.message + err.message + "<br />"
          }
        );

      } else {
        this.tribuService.getConnected().subscribe(
          data => {
            this.fillUser(data)
          },
          err => {
            this.isFailed = true;
            this.message = this.message + err.message + "<br />"
          }
        );
      }
    } else {
      this.router.navigate(['login']);
    }
  }

  fillUser(tribu: Tribu) {
    this.tribu = tribu;
    this.tribu.adherents.forEach(adh => {
      adh.adhesions.forEach(adhesion => this.adhesions.push(adhesion))
      this.sanitize(adh);
    });
    this.fillObjects();
  }

  sanitize(adh: Adherent) {
    adh.adhesions = adh.adhesions == null || undefined ? [] : adh.adhesions;
    adh.adresse = adh.adresse == null || undefined ? "" : adh.adresse;
    adh.email = adh.email == null || undefined ? "" : adh.email;
    adh.naissance = adh.naissance == null || undefined ? undefined : adh.naissance;
    adh.nom = adh.nom == null || undefined ? "" : adh.nom;
    adh.prenom = adh.prenom == null || undefined ? "" : adh.prenom;
    adh.telephone = adh.telephone == null || undefined ? "" : adh.telephone;
    adh.referent = adh.referent == null || undefined ? false : adh.referent;
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
      },
      err => {
        this.isFailed = true;
        this.message = this.message + err.message + "<br />"
      }
    );

    if (this.tribu.adherents.filter(adh => adh.prenom.length < 1).length > 0) {
      this.infoAdherent = true;
    }

    this.calculCompletudeAdhesion()
    this.calculCompletudeAdherent()
    this.calculRestantDu()
  }

  adherentsTotaux: number = 0
  adherentsACompleter: number = 0
  calculCompletudeAdherent() {
    this.adherentsTotaux = 0
    this.adherentsACompleter = 0
    this.tribu.adherents.forEach(adherent => {
      if (this.adhesions.filter(adh => adh.adherent?.id == adherent.id).length > 0) {
        this.adherentsTotaux += 1
      }
      if (!adherent.completAdhesion) {
        this.adherentsACompleter += 1
      }
    })
    if (this.adherentsACompleter > 0 || this.adhesions.length == 0) {
      this.adherentsOpen = true
    } else {
      this.adherentsOpen = false
    }
  }

  adhesionsACompleter: number = 0
  calculCompletudeAdhesion() {
    this.adhesionsACompleter = 0;
    this.adhesionsACompleter += this.adhesions.filter(adh => adh.statutActuel == 'Attente validation adhérent').length
    if (this.adhesionsACompleter > 0) {
      this.dossierIncomplet = true;
      this.adhesionsOpen = true
    } else {
      this.dossierIncomplet = false;
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

  onSubmit(adherent: Adherent): void {
    this.infoAdherent = false;
    adherent.telephone = this.cleaning(adherent.telephone)
    adherent.email = this.cleaning(adherent.email)

    if (adherent.id != 0) {
      this.adherentService.update(adherent).subscribe(
        data => {
          window.location.reload();
        },
        err => {
          this.isFailed = true;
          this.message = this.message + err.message + "<br />"
        }
      );
    } else {
      this.adherentService.save(adherent, this.tribu.id).subscribe(
        data => {
          window.location.reload();
        },
        err => {
          this.isFailed = true;
          this.message = this.message + err.message + "<br />"
        }
      );
    }
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
    let newAdhesion = new Adhesion;

    newAdhesion.id = this.newAdhesions.length + 1
    newAdhesion.activite = newAdhesionActivite;
    newAdhesion.adherent = adherent;
    newAdhesion.tarif = newAdhesionActivite.tarif
    this.newAdhesions.push(newAdhesion)
    this.calculCompletudeAdhesion()
    this.calculRestantDu()
  }

  saveAdhesions(adhesions: Adhesion[]) {
    adhesions.forEach(adh => {

      if (adh.adherent) {
        let newAdherent = new Adherent(0)
        newAdherent.id = adh.adherent.id
        adh.adherent = newAdherent
      }
      if (adh.activite) {
        let newActivite = new Activite()
        newActivite.id = adh.activite.id
        adh.activite = newActivite
      }
    })
    this.adhesionService.add(adhesions).subscribe(
      data => {
        this.newAdhesions = []
        data.forEach(adh => this.adhesions.push(adh))
        this.calculCompletudeAdhesion()
        this.calculCompletudeAdherent()
        this.calculRestantDu()
      },
      err => {
        this.isFailed = true;
        this.message = this.message + err.message + "<br />"
      }
    );
  }

  updateAccord(accord: Accord, etat: boolean | undefined) {

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

  updateAdhesion(adhesion: Adhesion) {

    if (adhesion.adherent) {
      let newAdherent = new Adherent(0)
      newAdherent.id = adhesion.adherent.id
      adhesion.adherent = newAdherent
    }
    if (adhesion.activite) {
      let newActivite = new Activite()
      newActivite.id = adhesion.activite.id
      adhesion.activite = newActivite
    }

    this.adhesionService.update(adhesion).subscribe(
      data => {
        adhesion.statutActuel = data.statutActuel;
        this.calculCompletudeAdhesion()
        this.calculRestantDu()
      },
      err => {
        this.isFailed = true;
        this.message = this.message + err.message + "<br />"
      }
    );
  }

  deleteAdhesion(adhesion: Adhesion) {
    this.newAdhesions = this.newAdhesions.filter(adh => adh.id != adhesion.id)
  }

  infoAdherent: boolean = false
  addAdherent() {
    let adherent = new Adherent(0);
    let accordRGPD = new Accord
    accordRGPD.nom = 'RGPD'
    adherent.accords.push(accordRGPD)
    let accordImage = new Accord
    accordImage.nom = 'Droit Image'
    adherent.accords.push(accordImage)
    this.infoAdherent = true;
    this.tribu.adherents.push(adherent);
    this.calculCompletudeAdherent()
  }

  removeAdherent(addAdherent: Adherent) {
    this.tribu.adherents = this.tribu.adherents.filter(adh => adh.id != 0)
    this.calculCompletudeAdherent()
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

  afficheAlerte: boolean = false;
  afficheAlerteDonneesPerso() {
    this.afficheAlerte = true;
    setTimeout(() => { this.afficheAlerte = false }, 5000);
  }

  uploadFile() {
    this.modalService.dismissAll();
    this.adherentService.addDocument(this.pdfFile, this.pdfAdherent.id).subscribe(
      data => {
        if (this.pdfAdherent.documents.length > 0) {
          this.pdfAdherent.documents.push(data)
        } else {
          let docs = []
          docs.push(data)
          this.pdfAdherent.documents = docs;
        }
      },
      err => {
        this.isFailed = true;
        this.message = this.message + err.message + "<br />"
      }
    );
  }

  deleteDoc(doc: Document, adherent: Adherent) {
    this.adherentService.deleteDoc(doc.id, adherent.id).subscribe(
      data => {
        adherent.documents = adherent.documents.filter(document => document.id != doc.id)
      },
      err => {
        this.isFailed = true;
        this.message = this.message + err.message + "<br />"
      }
    );
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
  pdfAdherent: Adherent = new Adherent(0)
  pdfFile: any
  pdfEditSrc: string = ""

  openModal(targetModal: any, adherent: Adherent) {
    this.modalService.open(targetModal, {
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    this.pdfAdherent = adherent
  }

  messagePopup: string = ""
  isFailedPopup: boolean = false
  loadFile(event: any) {
    if (event.target.files && event.target.files[0]) {
      if (event.target.files[0].size > 1000000) {
        this.isFailedPopup = true;
        this.messagePopup = "La taille maximale du fichier est de 1Mo"
      } else if (event.target.files[0].type != "application/pdf") {
        this.isFailedPopup = true;
        this.messagePopup = "Le fichier doit etre un PDF"
      } else {
        this.isFailedPopup = false;
        var reader = new FileReader();
        reader.onload = (e: any) => {
          this.pdfFile = event.target.files[0]
          this.pdfName = event.target.files[0].name

          this.pdfSrc = e.target.result;
        }
        reader.readAsDataURL(event.target.files[0]);
      }
    }
  }

  dismiss() {
    this.modalService.dismissAll();
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
      this.doc.text("résident au " + adhesion.adherent.adresseReferent ? adhesion.adherent.adresse : adhesion.adherent.adresse, 10, 140);
      this.doc.text("est inscrit à l'activité " + adhesion.activite.nom + " pour l'année scolaire 2023/2024", 10, 150);
      this.doc.text("et est à jour de sa cotisation de " + prix + "€ payée en " + adhesion.paiements[0].typeReglement, 10, 160);

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

  @ViewChild('modal')
  private modalComponent!: ModalComponent;
  public modalConfig: ModalConfig = {
    modalTitle: "titre",
    modalBody: "contenu",
    dismissButtonLabel: "X",
    closeButtonLabel: "Close",
    acceptButtonLabel: "Accepte"
  };

  async openModal2(titre: string, body: string, refus: boolean, backClick: boolean) {
    this.modalConfig.modalTitle = titre
    this.modalConfig.modalBody = body
    return await this.modalComponent.open()
  }

  selectedAdherent: Adherent = new Adherent(0)
  openConfirmModal(targetModal: any, adherent: Adherent) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });
    this.selectedAdherent = adherent;
  }

  acceptSupress(adherent: Adherent) {
    this.modalService.dismissAll();
    this.adherentService.deleteAdherent(adherent.id).subscribe(
      data => {
        window.location.reload();
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
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
          this.errorMessage = error.message
        }
      });
    }
  }

  openConfirmModalAccordAdherent(targetModal: any, adherent: Adherent, accord: Accord) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });
    this.selectedAdherent = adherent;
    this.selectedAccord = accord;
  }

  selectedAdhesion: Adhesion = new Adhesion
  openConfirmModalAccordAdhesion(targetModal: any, adhesion: Adhesion, accord: Accord) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });
    this.selectedAdhesion = adhesion;
    this.selectedAccord = accord;
  }

  dismissSupress() {
    this.modalService.dismissAll();
  }

  consultation: boolean = true;
  selectedAccord: Accord = new Accord
  openAccordModal(targetModal: any, accord: Accord, consultation: boolean) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: consultation
    });
    this.selectedAccord = accord;
    this.consultation = consultation
  }

  reponseAccord(accord: Accord, response: boolean) {
    this.updateAccord(accord, response)
    this.modalService.dismissAll();
  }

  cleaning(chaine: string) {
    return chaine.toLowerCase().replaceAll(" ", "")
  }

  ajoutAccord(adherent: Adherent, nomAccord: string) {
    this.adherentService.addAccord(adherent.id, nomAccord).subscribe(
      data => {
        adherent.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  retraitAccord(adherent: Adherent, accord: Accord) {
    this.modalService.dismissAll();
    this.adherentService.removeAccord(adherent.id, accord.nom).subscribe(
      data => {
        adherent.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  ajoutAccordAdhesion(adhesion: Adhesion, nomAccord: string) {
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

  retraitAccordAdhesion(adhesion: Adhesion, accord: Accord) {
    this.modalService.dismissAll();
    this.adhesionService.removeAccord(adhesion.id, accord.nom).subscribe(
      data => {
        adhesion.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }
}