import { Component, OnInit } from '@angular/core';
import { AdherentService } from '../../_services/adherent.service';
import { Adherent, AdherentLite } from 'src/app/models';
import { faPen, faUsersRays, faSkull, faUsers, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from 'src/app/_services/auth.service';
import { ParamService } from 'src/app/_services/param.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { FilterAdhesionByPipe } from 'src/app/_helpers/filterAdhesion.pipe';

@Component({
  selector: 'app-adherents',
  templateUrl: './adherents.component.html',
  styleUrls: ['./adherents.component.css']
})
export class AdherentsComponent implements OnInit {
  faCircleCheck=faCircleCheck
  faCircleXmark=faCircleXmark
  faSquareMinus=faSquareMinus
  faSquarePlus=faSquarePlus
  faPen = faPen;
  faUsersRays = faUsersRays;
  faUsers = faUsers;
  faSkull = faSkull;
  faEnvelope = faEnvelope;
  faPencilSquare = faPencilSquare;
  adherents: AdherentLite[] = [];
  cloned: AdherentLite[] = [];
  adherentsReferent: AdherentLite[] = [];
  isFailed: boolean = false;
  errorMessage = '';
  ordre: string = 'nom';
  search: string = "";
  sens: boolean = false;
  showAdmin: boolean = false;
  showSecretaire: boolean = false;
  filtres: Map<string, any> = new Map<string, any>();
  subscription = new Subscription()

  constructor(
    private filterByPipe: FilterAdhesionByPipe,
    private excelService: ExcelService,
    private authService: AuthService,
    private adherentService: AdherentService,
    private tokenStorageService: TokenStorageService,
    private modalService: NgbModal,
    public paramService: ParamService,
    public router: Router) { }

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
      this.showSecretaire = this.tokenStorageService.getUser().roles.includes('ROLE_SECRETAIRE');
    } else {
      this.router.navigate(['login']);
    }
    this.adherentService.getAllLite().subscribe(
      data => {
        this.cloned = data.map(x => Object.assign({}, x));
        this.adherentsReferent = data.filter(a => a.referent)
        this.filtrage()
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      })
  }

  exportAsXLSX(): void {
    console.log(this.adherents)
    this.excelService.exportAsExcelFile(this.adherents, 'adherents');
  }


  addSearch(search: string) {
    if (search.length > 2) {
      this.addFiltre('nomPrenom', search)
    } else if(this.filtres.has('nomPrenom')) {
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

  selectedAdherent: AdherentLite = new AdherentLite(0)
  openModal(targetModal: any, adherent: AdherentLite) {
    this.modalService.open(targetModal, {
      centered: true,
      backdrop: 'static'
    });
    this.selectedAdherent = adherent;

  }

  dismiss() {
    this.modalService.dismissAll();

  }


  referentAdherent: AdherentLite = new AdherentLite(0)
  onSubmitChangeTribu() {
    this.modalService.dismissAll();


    this.adherentService.changeTribu(this.referentAdherent.id, this.selectedAdherent.id).subscribe(
      data => {

        this.selectedAdherent = data
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }


  onSubmitAddUser(email: string) {
    this.modalService.dismissAll();

    this.authService.registerAnonymous(email).subscribe(
      data => {

        this.router.navigate(['inscription', data.tribu.uuid]);
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );

  }

  opennewTab(page : string){
    window.open(page, '_blank');
  }

  acceptSupress(adherent: AdherentLite) {
    this.modalService.dismissAll();
    this.adherentService.deleteAdherent(adherent.id).subscribe(
      data => {
        this.adherents = this.adherents.filter(adh => adh.id != adherent.id)
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    )
  }

  dismissSupress() {
    this.modalService.dismissAll();

  }

  ajoutAccord(adherent: AdherentLite, nomAccord: string) {
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

  retraitAccord(adherent: AdherentLite, nomAccord: string) {
    this.adherentService.removeAccord(adherent.id, nomAccord).subscribe(
      data => {

        adherent.accords = data;
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message

      }
    );
  }

}
