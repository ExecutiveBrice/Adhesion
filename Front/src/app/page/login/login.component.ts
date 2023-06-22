import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Router } from '@angular/router';
import { ParamTransmissionService } from 'src/app/_helpers/transmission.service';
import { faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  form: any = {
    username: null,
    password: null
  };
  faCircleCheck = faCircleCheck;
  subscription = new Subscription()
  isLoggedIn = false;
  isLoginFailed = false;
  isresetFailed = false;
  oublieMDP = false;
  newInscription = false;
  errorMessage = '';
  reinitMDPDone = false;
  roles: string[] = [];
  messageConnexion: string = ""
  messageInscription: string = ""
  textMaintenance: string = ""

  maintenance: Boolean = false;
  isSuccessful = false;
  isSignUpFailed = false;
  validRgpd = false;
  testRgpd = false;
  inscriptionOpen: boolean | null = false;


  constructor(
    public transmissionService: ParamTransmissionService,
    private authService: AuthService,
    private tokenStorage: TokenStorageService,
    public router: Router,
    public paramService: ParamService) { }

  ngOnInit(): void {

    this.paramService.getAllText().subscribe({
      next: (data) => {
        this.messageConnexion = data.filter(param => param.paramName == "Text_Accueil")[0].paramValue;
        this.messageInscription = data.filter(param => param.paramName == "Text_Inscription")[0].paramValue;
        this.textMaintenance = data.filter(param => param.paramName == "Text_Maintenance")[0].paramValue;
        

      },
      error: (error) => {
      }
    });
    this.paramService.getAllBoolean().subscribe({
      next: (data) => {
        this.inscriptionOpen = data.filter(param => param.paramName == "Inscription")[0].paramValue;
      },
      error: (error) => {
        this.inscriptionOpen = false;
      }
    });
    this.paramService.isClose()
      .subscribe({
        next: (data) => {
          this.maintenance = data;

          if (this.tokenStorage.getToken()) {
            this.isLoggedIn = true;
            this.roles = this.tokenStorage.getUser().roles;
            if (this.roles.includes('ROLE_ADMIN')) {
              this.router.navigate(['admin']);
            } else if (this.roles.includes('ROLE_SECRETAIRE')) {
              this.router.navigate(['adhesions']);
            } else if (this.roles.includes('ROLE_USER')) {
              if (!this.maintenance) {
                this.router.navigate(['inscription', '0']);
              }
            }
          }

        },
        error: (error) => {
          this.maintenance = true;
        }
      });


  }

  onSubmit(): void {
    const { username, password } = this.form;

    if (this.userExist) {
      this.authService.login(username, password).subscribe(
        data => {
          this.tokenStorage.saveToken(data.token);
          this.tokenStorage.saveUser(data);

          this.isLoginFailed = false;
          this.isLoggedIn = true;
          this.roles = this.tokenStorage.getUser().roles;
          this.reloadPage();

        },
        err => {
          this.errorMessage = err.error.message;
          this.isLoginFailed = true;
        }
      );


    } else {

      this.authService.register(username, password).subscribe(
        data => {

          this.isSuccessful = true;
          this.isSignUpFailed = false;

          this.authService.login(username, password).subscribe(
            data => {
              this.tokenStorage.saveToken(data.token);
              this.tokenStorage.saveUser(data);

              this.isLoginFailed = false;
              this.isLoggedIn = true;
              this.roles = this.tokenStorage.getUser().roles;
              this.reloadPage();

            },
            err => {
              this.errorMessage = err.error.message;
              this.isLoginFailed = true;
            }
          );
        },
        err => {
          this.errorMessage = err.error.message;
          this.isSignUpFailed = true;
        }
      );
    }
  }

  reloadPage(): void {
    window.location.reload();
  }

  forgotMdp() {
    const { username, password } = this.form;
    this.authService.reinitPassword(username).subscribe(
      data => {

        this.reinitMDPDone = true;
        this.oublieMDP = false;
        this.isresetFailed = false;
      },
      err => {
        this.errorMessage = err.error.message;
        this.isresetFailed = true;
      }
    );
  }

  afficheAlerte = false;
  afficheAlerteDonneesPerso() {
    this.afficheAlerte = true;
    setTimeout(() => { this.afficheAlerte = false }, 5000);
  }


  userExist: boolean = true;
  verifMailExist() {
    this.form.username = this.form.username.toLowerCase()
    this.form.username = this.form.username.trimEnd()
    this.form.username = this.form.username.trimStart()
    this.authService.userExist(this.form.username).subscribe(
      data => {

        this.userExist = true;
      },
      err => {
        this.userExist = false;
      }
    );
  }
}