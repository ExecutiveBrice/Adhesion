import { Component, OnInit } from '@angular/core';
import { UserService } from '../../_services/user.service';
import { ParamService } from '../../_services/param.service';

import { ParamBoolean, ParamNumber, ParamText, UserLite } from 'src/app/models';
import { Observable } from 'rxjs';
import { faEnvelope, faCircleQuestion, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';



@Component({
  selector: 'app-board-admin',
  templateUrl: './board-admin.component.html',
  styleUrls: ['./board-admin.component.css']
})
export class BoardAdminComponent implements OnInit {
  faEnvelope = faEnvelope;
  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faSquareMinus = faSquareMinus;
  paramBooleans: ParamBoolean[] = [];
  paramTexts: ParamText[] = [];
  usersLite: UserLite[] = [];
  adminsLite: UserLite[] = [];
  administrateursLite: UserLite[] = [];
  bureauxLite: UserLite[] = [];
  secretairesLite: UserLite[] = [];
  profsLite: UserLite[] = [];
  comptablesLite: UserLite[] = [];
  constructor(private paramService: ParamService, private userService: UserService) { }

  ngOnInit(): void {
    this.getAllBoolean()
    this.getAllText()
    this.getAllNumber()
    this.fillLists()
  }


  fillLists() {
    this.userService.getAllLite().subscribe(
      data => {

        this.usersLite = data;
        this.adminsLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_ADMIN').length > 0)
        this.administrateursLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_ADMINISTRATEUR').length > 0)
        this.bureauxLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_BUREAU').length > 0)
        this.secretairesLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_SECRETAIRE').length > 0)
        this.profsLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_PROF').length > 0)
        this.comptablesLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_COMPTABLE').length > 0)
      },
      err => {
        ;
      }
    );
  }

  grantUser(email: string, role: string) {
    this.userService.grantUser(role, email).subscribe(
      data => {
        
        this.fillLists()
      },
      err => {
        ;
      }
    );
  }

  unGrantUser(email: string, role: string) {
    this.userService.unGrantUser(role, email).subscribe(
      data => {
        
        this.fillLists()
      },
      err => {
        ;
      }
    );
  }

  updateParamText(param: ParamText) {
    this.paramService.saveText(param).subscribe(
      data => {
        
      },
      err => {
        ;
      }
    );
  }
  updateParamBoolean(param: ParamBoolean) {
    this.paramService.saveBoolean(param).subscribe(
      data => {
      },
      err => {
        ;
      }
    );
  }

  updateParamNumber(param: ParamNumber) {
    this.paramService.saveNumber(param).subscribe(
      data => {
        
      },
      err => {
        ;
      }
    );
  }


  getAllBoolean() {
    this.paramService.getAllBoolean().subscribe(
      data => {
        this.paramBooleans = data;

      },
      err => {
        ;
      }
    );
  }

  getAllText() {
    this.paramService.getAllText().subscribe(
      data => {
        this.paramTexts = data;
        
      },
      err => {
        ;
      }
    );
  }
  paramNumbers: ParamNumber[] = []
  getAllNumber() {
    this.paramService.getAllNumber().subscribe(
      data => {
        this.paramNumbers = data;
        
      },
      err => {
        ;
      }
    );
  }
}
