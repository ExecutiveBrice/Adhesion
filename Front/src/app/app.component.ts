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
  shoBureauBoard = false;
  showAdminstrateurBoard = false;
  showSecretaire = false;
  username?: string;
  maintenance: Boolean = false
  inscriptionOpen: boolean | null = true
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
      this.shoBureauBoard = this.roles.includes('ROLE_BUREAU');
      this.showAdminstrateurBoard = this.roles.includes('ROLE_ADMINISTRATEUR');
      this.showSecretaire = this.roles.includes('ROLE_SECRETAIRE');
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

          this.paramService.getAllBoolean().subscribe({
          next: (data) => {
            this.inscriptionOpen = data.filter(param => param.paramName == "Inscription")[0].paramValue;
          },
          error: (error) => {

          }
        });
  }


  logout(): void {
    this.tokenStorageService.signOut();
    window.location.reload();
  }
}
