import { Component, OnInit } from '@angular/core';
import { ActiviteLite } from 'src/app/models';
import { faSquareCaretLeft, faSquareCaretDown, faSkull, faUsers, faEnvelope, faCircleXmark, faFlag, faPiggyBank, , faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';

import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { HttpErrorResponse } from '@angular/common/http';


@Component({
  selector: 'app-seances',
  templateUrl: './seances.component.html',
  styleUrls: ['./seances.component.css']
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

    private activiteService: ActiviteService,
    private tokenStorageService: TokenStorageService,
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

    this.activiteService.getSeancesDuJour().subscribe({
      next: (data) => {

console.log(data)
      },
      error: (error: HttpErrorResponse) => {
        console.log(error)


      }
    });
  }





}
