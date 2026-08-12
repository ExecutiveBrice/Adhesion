import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdherentService } from '../../_services/adherent.service';
import { Activite, ActiviteDropDown, Adherent } from 'src/app/models';
import { faPen, faUsersRays, faSkull, faUsers, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthService } from 'src/app/_services/auth.service';
import { ParamService } from 'src/app/_services/param.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { FilterAdhesionByPipe } from 'src/app/_helpers/filterAdhesion.pipe';
import { ActiviteService } from 'src/app/_services/activite.service';
import { ActiviteNm1 } from 'src/app/models/activiteNm1';
import { TribuService } from 'src/app/_services/tribu.service';
import { ToastService } from '../../_services/toast.service';
import {AdherentFlat} from "../../models/adherentFlat";
import { FormsModule } from '@angular/forms';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { OrderByPipe } from '../../_helpers/sort.pipe';

@Component({
    selector: 'app-adherents',
    templateUrl: './adherents.component.html',
    styleUrls: ['./adherents.component.css'],
    imports: [FormsModule, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, FaIconComponent, OrderByPipe]
})
export class AdherentsComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private toastr = inject(ToastService);
  activiteService = inject(ActiviteService);
  tribuService = inject(TribuService);
  private authService = inject(AuthService);
  private adherentService = inject(AdherentService);
  private tokenStorageService = inject(TokenStorageService);
  private modalService = inject(NgbModal);
  paramService = inject(ParamService);
  router = inject(Router);

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
  pageSize = 10;
  readonly pageSizes = [10, 20, 50, 100];
  totalElements = 0;
  totalPages = 0;
  searchTerm = '';
  activitySearchTerm = '';
  activityNm1SearchTerm = '';
  private readonly searchChanges = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  loadder:boolean=true
  errorMessage = '';

  type: string = 'Mineur';
  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  activitesListe: ActiviteDropDown[] = [];
  activites: Activite[] = []


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
    this.searchChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.getAdherents(true));

    this.getAdherents();
    this.activiteService.fillObjects(this.activites, this.activitesListe, undefined);
  }

  getAdherents(resetPage: boolean = false) {
    if (resetPage) {
      this.page = 1;
    }
    this.loadder = true;
    this.adherentService.getPage(
      this.page - 1,
      this.pageSize,
      this.searchTerm.trim(),
      this.activitySearchTerm.trim(),
      this.activityNm1SearchTerm.trim()
    ).subscribe({
      next: (data) => {
        this.adherents = data.content.sort((first, second) =>
          (first.nom || '').localeCompare(second.nom || '', 'fr', {sensitivity: 'base'}) ||
          (first.prenom || '').localeCompare(second.prenom || '', 'fr', {sensitivity: 'base'})
        );
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

  onSearchChange(value: string) {
    this.searchChanges.next(value.trim());
  }

  resetFilters() {
    this.searchTerm = '';
    this.activitySearchTerm = '';
    this.activityNm1SearchTerm = '';
    this.getAdherents(true);
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

  get visiblePages(): number[] {
    const windowSize = Math.min(4, this.totalPages);
    const start = Math.max(1, Math.min(this.page - 1, this.totalPages - windowSize + 1));
    return Array.from({length: windowSize}, (_, index) => start + index);
  }

  get showLeadingEllipsis(): boolean {
    return this.visiblePages[0] > 1;
  }

  get showTrailingEllipsis(): boolean {
    return this.visiblePages[this.visiblePages.length - 1] < this.totalPages;
  }

  accordsDetails(accords: string): { nom: string, etat: boolean }[] {
    return (accords || '')
      .split(/\r?\n/)
      .filter(accord => accord.trim().length > 0)
      .map(accord => ({
        nom: accord.replace(/\s+(true|false)$/, ''),
        etat: accord.trim().endsWith('true')
      }));
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
