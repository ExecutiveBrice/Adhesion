import { Component } from '@angular/core';
import { TokenStorageService } from './_services/token-storage.service';
import { ParamService } from './_services/param.service';
import { Subscription } from 'rxjs';
import { ParamTransmissionService } from './_helpers/transmission.service';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  isCollapsed = true
  private roles: string[] = [];
  isLoggedIn = false;
  showAdmin = false;
  showBureau = false;
  showAdminstrateur = false;
  showSecretaire = false;
  showProf=false;
  showComptable=false;
  username?: string;
  maintenance: Boolean = false
  subscription = new Subscription()

  constructor(
    public transmissionService: ParamTransmissionService,
    private paramService: ParamService,
    private tokenStorageService: TokenStorageService
    ) { }

  ngOnInit(): void {
    this.isLoggedIn = !!this.tokenStorageService.getToken();

    if (this.isLoggedIn) {
      const user = this.tokenStorageService.getUser();
      this.roles = user.roles;

      this.showAdmin = this.roles.includes('ROLE_ADMIN');
      this.showBureau = this.roles.includes('ROLE_BUREAU');
      this.showAdminstrateur = this.roles.includes('ROLE_ADMINISTRATEUR');
      this.showSecretaire = this.roles.includes('ROLE_SECRETAIRE');
      this.showProf = this.roles.includes('ROLE_PROF');
      this.showComptable = this.roles.includes('ROLE_COMPTABLE');
      
      this.username = user.username;
    }

        this.paramService.isClose()
        .subscribe({
          next: (data) => {
            this.maintenance = data;
          },
          error: (error) => {

            this.maintenance =  true;
          }
        });


  }


  logout(): void {
    this.tokenStorageService.signOut();
    window.location.reload();
  }
}
