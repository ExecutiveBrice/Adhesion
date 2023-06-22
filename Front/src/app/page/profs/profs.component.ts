import { Component, OnInit } from '@angular/core';
import { AdherentService } from '../../_services/adherent.service';
import { ActiviteLite, Adherent, AdherentLite } from 'src/app/models';
import { faPen, faUsersRays, faSkull, faUsers, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from 'src/app/_services/auth.service';
import { ParamService } from 'src/app/_services/param.service';

@Component({
  selector: 'app-profs',
  templateUrl: './profs.component.html',
  styleUrls: ['./profs.component.css']
})
export class ProfsComponent implements OnInit {
  faCircleCheck=faCircleCheck;
  faCircleXmark = faCircleXmark;
  faUsers = faUsers;
  faSkull = faSkull;
  faEnvelope = faEnvelope;
  faPencilSquare = faPencilSquare;
  activiteLite: ActiviteLite[] = [];

  isFailed: boolean = false;
  errorMessage = '';
  ordre: string = 'nom';
  search: string = "";
  sens: boolean = false;
  showAdmin: boolean = false;
  filtres: Map<string, boolean> = new Map<string, boolean>();
  subscription = new Subscription()

  constructor(

    private authService: AuthService,
    private adherentService: AdherentService,
    private tokenStorageService: TokenStorageService,
    private modalService: NgbModal,
    public paramService: ParamService,
    public router: Router) { }

  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
    } else {
      this.router.navigate(['login']);
    }



    this.adherentService.getAllCours().subscribe(
      data => {
        
        this.activiteLite = data

      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
        
      }
    );
  }





}
