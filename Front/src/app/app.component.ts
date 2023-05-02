import { Component } from '@angular/core';
import { TokenStorageService } from './_services/token-storage.service';
import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { ParamService } from './_services/param.service';
import { Param } from './models/param';
import { ParamStorageService } from './_services/param-storage.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  isCollapsed = true
  private roles: string[] = [];
  isLoggedIn = false;
  showAdminBoard = false;
  shoBureauBoard = false;
  showAdminstrateurBoard = false;
  showSecretaireBoard = false;
  username?: string;
  params: Param[] = []
  maintenance: boolean = false
  inscriptionOpen: boolean = true


  constructor(private paramService: ParamService, private tokenStorageService: TokenStorageService, public paramStorageService: ParamStorageService) { }

  ngOnInit(): void {
    this.isLoggedIn = !!this.tokenStorageService.getToken();

    if (this.isLoggedIn) {
      const user = this.tokenStorageService.getUser();
      this.roles = user.roles;

      this.showAdminBoard = this.roles.includes('ROLE_ADMIN');
      this.shoBureauBoard = this.roles.includes('ROLE_BUREAU');
      this.showAdminstrateurBoard = this.roles.includes('ROLE_ADMINISTRATEUR');
      this.showSecretaireBoard = this.roles.includes('ROLE_SECRETAIRE');

      this.username = user.username;
    }


    this.paramService.getAll().subscribe(
      data => {
        console.log(data)
        this.params = data;
        window.sessionStorage.setItem("PARAMS", JSON.stringify(data));
        this.maintenance = this.paramStorageService.isOpen();

        this.inscriptionOpen = this.params.filter(param => param.paramName == "Inscription" && param.paramValue == "True").length > 0;
      },
      err => {
        JSON.parse(err.error).message;
      }
    );
  }


  logout(): void {
    this.tokenStorageService.signOut();
    window.location.reload();
  }
}
