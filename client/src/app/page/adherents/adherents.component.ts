import { Component, OnInit } from '@angular/core';
import { AdherentService } from '../../_services/adherent.service';
import { Activite, ActiviteDropDown, Adherent } from 'src/app/models';
import { faPen, faUsersRays, faSkull, faUsers, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from 'src/app/_services/auth.service';
import { ParamService } from 'src/app/_services/param.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { FilterAdhesionByPipe } from 'src/app/_helpers/filterAdhesion.pipe';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ActivitesNm1 } from 'src/app/models/activitesNm1';
import { TribuService } from 'src/app/_services/tribu.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-adherents',
  templateUrl: './adherents.component.html',
  styleUrls: ['./adherents.component.css']
})
export class AdherentsComponent implements OnInit {
  faCircleCheck = faCircleCheck
  faCircleXmark = faCircleXmark
  faSquareMinus = faSquareMinus
  faSquarePlus = faSquarePlus
  faPen = faPen;
  faUsersRays = faUsersRays;
  faUsers = faUsers;
  faSkull = faSkull;
  faEnvelope = faEnvelope;
  faPencilSquare = faPencilSquare;
  adherents: Adherent[] = [];
  cloned: Adherent[] = [];

  loadder:boolean=true
  errorMessage = '';
  ordre: string = 'nom';
  search: string = "";
  type: string = 'Mineur';
  sens: boolean = false;
  showAdmin: boolean = false;
  showSecretaire: boolean = false;
  filtres: Map<string, any> = new Map<string, any>();
  subscription = new Subscription()
  activitesListe: ActiviteDropDown[] = [];
  activites: Activite[] = []
  constructor(
    private toastr: ToastrService,
    public activiteService: ActiviteService,
    public tribuService: TribuService,
    private filterByPipe: FilterAdhesionByPipe,
    private excelService: ExcelService,
    private authService: AuthService,
    private adherentService: AdherentService,
    private tokenStorageService: TokenStorageService,
    private modalService: NgbModal,
    public paramService: ParamService,
    public router: Router) { }


  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }

  showSuccess(message: string) {
    this.toastr.info(message, 'Information');
  }

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
    } else {
      this.router.navigate(['login']);
    }
    this.adherentService.getAllLite().subscribe({
      next: (data) => {
        console.log(data)
        this.cloned = data.map(x => Object.assign({}, x));
        this.cloned.forEach(adh => {
          adh.nomPrenom = adh.nom+adh.prenom
          adh.activitesNm1.forEach(act =>  adh.activitesNm1Text = " "+act.nom +" " + act.horaire)
          adh.adhesions.forEach(adhesion =>  adh.activitesText = " "+adhesion.activite?.nom +" " + adhesion.activite?.horaire)
        })
        this.filtrage()
        this.loadder=false


      },
      error: (error) => {
        console.log(error)
        this.showError(error.error.message)
      }
    })
    this.activiteService.fillObjects(this.activites, this.activitesListe, undefined);
  }

  exportAsXLSX(): void {
    console.log(this.adherents)
    
    const adherents:Adherent[] = JSON.parse(JSON.stringify((this.adherents)));
    adherents.forEach(adherent => {
      adherent.email = (adherent.emailRepresentant && adherent.representant != undefined)?adherent.representant.user.username:adherent.user.username
      adherent.tel =(adherent.telephoneRepresentant && adherent.representant != undefined)?adherent.representant.telephone:adherent.telephone
      adherent.adress =(adherent.adresseRepresentant && adherent.representant != undefined)?adherent.representant.adresse +' '+ (adherent.representant.codePostal != null?adherent.representant.codePostal:'') + ' ' + (adherent.representant.ville != null?adherent.representant.ville:'') : adherent.adresse + ' ' + (adherent.codePostal != null?adherent.codePostal:'') + ' ' + (adherent.ville != null?adherent.ville:'');
    })
    this.excelService.exportAsExcelFile(adherents, 'adherents');
  }


  addSearch(search: string) {
    if (search.length > 2) {
      this.addFiltre('nomPrenom', search)
    } else if (this.filtres.has('nomPrenom')) {
      this.removeFiltre('nomPrenom')
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
    this.adherents = this.filterByPipe.transform(this.cloned.map(x => Object.assign({}, x)), this.filtres)
  }

  newUser: string = ""
  openModalAddUser(targetModal: any) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });
  }

  dismiss() {
    this.modalService.dismissAll();
  }


  onSubmitAddUser(email: string) {
    this.modalService.dismissAll();

    this.authService.registerAnonymous(email).subscribe({
      next: (adherent) => {

        let activitesNm1: ActivitesNm1[] = []
        this.activitesListe.forEach(activiteMineur => {
          if (activiteMineur.selected) {
            let activiteNm1 = new ActivitesNm1();
            activiteNm1.nom = activiteMineur.nom
            activiteNm1.horaire = this.type
            activiteNm1.tribu = adherent.tribu
            activitesNm1.push(activiteNm1)
          }
        })

        this.showSuccess("L'adhérent " + adherent.user.username + " à bien été créé")
        this.tribuService.addActivitesNm1(adherent.tribu.uuid, activitesNm1).subscribe({
          next: (response) => {
            console.log(response)

            this.router.navigate(['inscription', adherent.tribu.uuid]);
          },
          error: (error) => {
            console.log(error)
            this.showError(error.error.message)
          }
        });


      },
      error: (error) => {
        if (error.s)
          console.log(error)
        this.showError(error.error.message)
      }
    });

  }

  opennewTab(page: string) {
    window.open(page, '_blank');
  }





}
