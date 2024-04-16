import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Router } from '@angular/router';
import { ParamTransmissionService } from 'src/app/_helpers/transmission.service';
import { faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { Subscription } from 'rxjs';
import { ParamService } from 'src/app/_services/param.service';
import { ToastrService } from 'ngx-toastr';

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
  textRGPD: string = ""

  maintenance: Boolean = false;
  isSuccessful = false;
  isSignUpFailed = false;
  validRgpd = false;
  testRgpd = false;
  inscriptionOpen: boolean = false;


  constructor(
    private toastr: ToastrService,
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

        this.textRGPD = data.filter(param => param.paramName == "RGPD")[0].paramValue;
        
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
                this.router.navigate(['inscription', '']);
              }
            }
          }

        },
        error: (error) => {
          this.maintenance = true;
        }
      });


  }
  onSubmitInscritpion(): void{
    const { username, password } = this.form;
    if (this.inscriptionOpen){

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
              window.location.reload();

            },
            err => {
              this.errorMessage = err.error.message;
              this.showWarning(err.error.message)
              this.isLoginFailed = true;
            }
          );
        },
        err => {
          this.errorMessage = err.error.message;
          this.showWarning(err.error.message)
          this.isSignUpFailed = true;
        }
      );
    }else{
      this.showWarning("Les inscriptions ne sont pas encore ouverte,<br /> veuillez revenir à partir du 01/06/2024")
    }
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
          window.location.reload();

        },
        err => {
          this.errorMessage = err.error.message;
          this.showWarning("La connexion a échouée, mauvais mot de passe")
        }
      );


    }else{
      this.showWarning("Cette adresse e-mail n'existe pas dans l'application<br />Veuillez corriger l'adresse e-mail ou vous inscrire")
    }
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

  showWarning(message: string) {
    this.toastr.warning(message, 'Attention');
  }
  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }
}