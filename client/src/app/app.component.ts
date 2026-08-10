import { Component, inject } from '@angular/core';
import { TokenStorageService } from './_services/token-storage.service';
import { ParamService } from './_services/param.service';
import { ParamTransmissionService } from './_helpers/transmission.service';
import { ToastService } from './_services/toast.service';
import { NgbCollapse } from '@ng-bootstrap/ng-bootstrap/collapse';
import { RouterLinkActive, RouterLink, RouterOutlet } from '@angular/router';
import { NgbToast, NgbToastHeader } from '@ng-bootstrap/ng-bootstrap/toast';


@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    imports: [NgbCollapse, RouterLinkActive, RouterLink, RouterOutlet, NgbToast, NgbToastHeader]
})
export class AppComponent {
  readonly toastService = inject(ToastService);
  transmissionService = inject(ParamTransmissionService);
  private paramService = inject(ParamService);
  private tokenStorageService = inject(TokenStorageService);

  isCollapsed = true
  private roles: string[] = [];
  isLoggedIn = false;
  showAdmin = false;
  showBureau = false;
  showAdminstrateur = false;
  showSecretaire = false;
  showProf=false;
  showReferent=false;
  showSeances=false;
  showComptable=false;
  username?: string;
  maintenance: Boolean = false
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
      this.showReferent = this.roles.includes('ROLE_REFERENT');
      this.showSeances = this.showProf || this.showReferent;
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
