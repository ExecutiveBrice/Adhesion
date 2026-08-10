import { Component, OnInit, inject } from '@angular/core';
import { AdherentService } from '../../_services/adherent.service';
import { ActiviteLite, Adherent, AdherentLite } from 'src/app/models';
import { faSquareCaretLeft, faSquareCaretDown, faSkull, faUsers, faEnvelope, faCircleXmark, faFlag, faPiggyBank, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/_services/auth.service';
import { ParamService } from 'src/app/_services/param.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { DatePipe } from '@angular/common';
import { OrderByPipe } from '../../_helpers/sort.pipe';

@Component({
    selector: 'app-profs',
    templateUrl: './profs.component.html',
    styleUrls: ['./profs.component.css'],
    imports: [FaIconComponent, DatePipe, OrderByPipe]
})
export class ProfsComponent implements OnInit {
  private authService = inject(AuthService);
  private adherentService = inject(AdherentService);
  private tokenStorageService = inject(TokenStorageService);
  private modalService = inject(NgbModal);
  paramService = inject(ParamService);
  router = inject(Router);

  faSquareCaretLeft=faSquareCaretLeft;
  faSquareCaretDown=faSquareCaretDown;
  faCircleCheck=faCircleCheck;
  faCircleXmark = faCircleXmark;
  faPiggyBank=faPiggyBank;
  faFlag=faFlag;
  faUsers = faUsers;
  faSkull = faSkull;
  faEnvelope = faEnvelope;
  faPencilSquare = faPencilSquare;
  activitesLite: ActiviteLite[] = [];
  mobile:boolean = false;
  isFailed: boolean = false;
  errorMessage = '';
  ordre: string = 'nom';
  search: string = "";
  sens: boolean = false;
  showAdmin: boolean = false;
  filtres: Map<string, boolean> = new Map<string, boolean>();
  ngOnInit(): void {
    if (this.tokenStorageService.getUser().roles) {
      this.showAdmin = this.tokenStorageService.getUser().roles.includes('ROLE_ADMIN');
    } else {
      this.router.navigate(['login']);
    }

    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }

    this.adherentService.getAllCours().subscribe(
      data => {
        console.log(data)
        this.activitesLite = data
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
        
      }
    );
  }





}
