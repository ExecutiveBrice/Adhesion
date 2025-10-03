import { Component, OnInit, Input, inject } from '@angular/core';
import { NgbModalConfig, NgbModal, NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ActiviteService } from 'src/app/_services/activite.service';
import { Activite, Adherent, AdherentLite } from 'src/app/models';
import { FormGroup, FormBuilder } from '@angular/forms';
import {
  faCircleQuestion,
  faEnvelope,
  faCircleXmark,
  faCloudDownloadAlt,
  faBook,
  faScaleBalanced,
  faPencilSquare,
  faSquarePlus,
  faSquareMinus,
  faCircleCheck,
  faUserPlus,
  faCartPlus
} from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ParamService } from 'src/app/_services/param.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Route, Router } from '@angular/router';
import { ModalActivite } from 'src/app/template/modal-activite/modal.activite';



@Component({
  selector: 'ngbd-modal-component',
  templateUrl: './activites.component.html',
  styleUrls: ['./activites.component.css'],
})
export class ActivitesComponent implements OnInit {
  faCircleQuestion=faCircleQuestion
  faEnvelope = faEnvelope;
  faCircleXmark = faCircleXmark;
  faCloudDownloadAlt = faCloudDownloadAlt;
  faScaleBalanced = faScaleBalanced;
  faBook = faBook;
  faCartPlus = faCartPlus;
  faCircleCheck = faCircleCheck;
  faSquareMinus = faSquareMinus;
  faPencilSquare = faPencilSquare;
  faSquarePlus = faSquarePlus;
  newActivite: Activite = new Activite;
  activites: Activite[] = [];
  isFailed = false;
  ordre: string = 'nom';
  search: string = "";
  errorMessage = '';
  isLoginFailed = false;
  sens: boolean = false;
  filtres: Map<string, boolean> = new Map<string, boolean>();
  editProfileForm!: FormGroup;

  showAdmin: boolean = false;
  showSecretaire: boolean = false;

  constructor(
    public activiteService: ActiviteService,
    public paramService: ParamService,
    private tokenStorageService: TokenStorageService,
    public router: Router
    ) { }

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
    } else {
      this.router.navigate(['login']);
    }

    this.getActivites();
  }




  private modalService = inject(NgbModal);
  openActivite(activite: Activite) {

    const modalRef = this.modalService.open(ModalActivite, {
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.activite = activite;


    modalRef.result.then((data) => {
      console.log(data)

      this.getActivites();
      // on close
    }, (reason) => {
      console.log(reason)

      // on dismiss
    });
  }








  getActivites() {
    this.activiteService.getAll().subscribe(
      data => {
        this.activites = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }



  filtre(nomFiltre: string, sens: boolean) {

    if (this.filtres.get(nomFiltre) != sens) {
      this.filtres.delete(nomFiltre)
    }

    if (this.filtres.get(nomFiltre) == sens) {
      this.filtres.delete(nomFiltre)
    } else {
      this.filtres.set(nomFiltre, sens);
    }
    this.activiteService.getAll().subscribe(
      data => {
        if (this.filtres.get('basket')) {
          this.activites = data.filter(a => a.groupe == "ALOD_B");
        } else if (!this.filtres.get('basket')) {
          this.activites = data.filter(a => a.groupe == "ALOD_G");
        }

        if (this.filtres.size == 0) {
          this.activites = data;
        }
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }
}
