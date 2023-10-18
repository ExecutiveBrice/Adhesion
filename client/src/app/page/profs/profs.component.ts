import { Component, OnInit } from '@angular/core';
import { AdherentService } from '../../_services/adherent.service';
import { ActiviteLite, Adherent, AdherentLite } from 'src/app/models';
import { faSquareCaretLeft, faSquareCaretDown, faSkull, faUsers, faEnvelope, faCircleXmark, faFlag, faPiggyBank, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
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

    if (window.innerWidth <= 1080) { // 768px portrait
      this.mobile = true;
    }

    this.adherentService.getAllCours().subscribe(
      data => {
        this.activitesLite = data
      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
        
      }
    );
  }





}
