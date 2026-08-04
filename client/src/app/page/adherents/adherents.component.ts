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
import { ActiviteNm1 } from 'src/app/models/activiteNm1';
import { TribuService } from 'src/app/_services/tribu.service';
import { ToastrService } from 'ngx-toastr';
import {AdherentFlat} from "../../models/adherentFlat";

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
  adherents: AdherentFlat[] = [];
  page = 1;
  pageSize = 20;
  readonly pageSizes = [10, 20, 50, 100];
  totalElements = 0;
  totalPages = 0;

  loadder:boolean=true
  errorMessage = '';

  type: string = 'Mineur';
  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  activitesListe: ActiviteDropDown[] = [];
  activites: Activite[] = []
  constructor(
    private toastr: ToastrService,
    public activiteService: ActiviteService,
    public tribuService: TribuService,

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
    this.getAdherents();
    this.activiteService.fillObjects(this.activites, this.activitesListe, undefined);
  }

  getAdherents(resetPage: boolean = false) {
    if (resetPage) {
      this.page = 1;
    }
    this.loadder = true;
    this.adherentService.getPage(this.page - 1, this.pageSize).subscribe({
      next: (data) => {
        this.adherents = data.content;
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

  goToPage(targetPage: number) {
    if (targetPage < 1 || targetPage > this.totalPages || targetPage === this.page) {
      return;
    }
    this.page = targetPage;
    this.getAdherents();
  }

  onPageSizeChange() {
    this.getAdherents(true);
  }

  get firstResult(): number {
    return this.totalElements === 0 ? 0 : (this.page - 1) * this.pageSize + 1;
  }

  get lastResult(): number {
    return Math.min(this.page * this.pageSize, this.totalElements);
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

        let activitesNm1: ActiviteNm1[] = []
        this.activitesListe.forEach(activiteMineur => {
          if (activiteMineur.selected) {
            let activiteNm1 = new ActiviteNm1();
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
