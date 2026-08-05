import { Component, OnInit } from '@angular/core';
import { UserService } from '../../_services/user.service';
import { ParamService } from '../../_services/param.service';

import { AgendaGoogleConfiguration, ParamBoolean, ParamNumber, ParamText, UserLite } from 'src/app/models';
import { forkJoin } from 'rxjs';
import { faCalendarDays, faCircleCheck, faCircleXmark, faEnvelope, faPlus, faSquareMinus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import {AuthService} from "../../_services/auth.service";
import {TokenStorageService} from "../../_services/token-storage.service";



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
  faCalendarDays = faCalendarDays;
  faPlus = faPlus;
  faTrash = faTrash;
  paramBooleans: ParamBoolean[] = [];
  paramTexts: ParamText[] = [];
  agendasGoogle: AgendaGoogleConfiguration[] = [];
  nouvelAgendaNom = '';
  nouvelAgendaSource = '';
  nouvelAgendaCouleur = '#4285F4';
  agendaEnregistrement = false;
  agendaMessage = '';
  agendaErreur = '';
  usersLite: UserLite[] = [];
  adminsLite: UserLite[] = [];
  administrateursLite: UserLite[] = [];
  bureauxLite: UserLite[] = [];
  secretairesLite: UserLite[] = [];
  profsLite: UserLite[] = [];
  comptablesLite: UserLite[] = [];


  constructor(    private tokenStorage: TokenStorageService,private authService: AuthService, private paramService: ParamService, private userService: UserService,  private adherentService: AdherentService) { }

  ngOnInit(): void {
    this.getAllBoolean()
    this.getAllText()
    this.getAllNumber()
    this.getAgendasGoogle()
    this.fillLists()
  }


  fillLists() {
    this.userService.getAllLite().subscribe(
      data => {
console.log(data)
        this.usersLite = data;
        this.adminsLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_ADMIN').length > 0)
        this.administrateursLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_ADMINISTRATEUR').length > 0)
        this.bureauxLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_BUREAU').length > 0)
        this.secretairesLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_SECRETAIRE').length > 0)
        this.profsLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_PROF').length > 0)
        this.comptablesLite = data.filter(adh => adh.roles.filter(role => role.name === 'ROLE_COMPTABLE').length > 0)
      },
      err => {

      }
    );
  }


  impersonate(email: string) {
    this.authService.impersonate(email).subscribe(
      data => {
        console.log(data)
        this.tokenStorage.saveToken(data.token);
        this.tokenStorage.saveUser(data);
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

  getAgendasGoogle(): void {
    this.paramService.getAgendasGoogle().subscribe({
      next: agendas => this.agendasGoogle = agendas,
      error: () => this.agendaErreur = "La configuration des agendas n'a pas pu être chargée."
    });
  }

  ajouterAgenda(): void {
    const nom = this.nouvelAgendaNom.trim();
    const source = this.nouvelAgendaSource.trim();
    this.agendaErreur = '';
    this.agendaMessage = '';
    if (!nom || !source) {
      this.agendaErreur = "Saisissez un nom et l'identifiant ou l'URL publique du Google Agenda.";
      return;
    }
    if (this.agendasGoogle.length >= 10) {
      this.agendaErreur = "Le nombre d'agendas Google est limité à 10.";
      return;
    }
    this.agendaEnregistrement = true;
    this.paramService.createAgendaGoogle({
      nom,
      source,
      couleur: this.nouvelAgendaCouleur
    }).subscribe({
      next: agenda => {
        this.agendasGoogle = [...this.agendasGoogle, agenda].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nouvelAgendaNom = '';
        this.nouvelAgendaSource = '';
        this.nouvelAgendaCouleur = this.prochaineCouleur();
        this.agendaEnregistrement = false;
        this.agendaMessage = 'Agenda ajouté.';
      },
      error: response => this.afficherErreurAgenda(response)
    });
  }

  supprimerAgenda(index: number): void {
    const agenda = this.agendasGoogle[index];
    if (agenda.id == null || this.agendaEnregistrement) {
      return;
    }
    this.agendaEnregistrement = true;
    this.agendaErreur = '';
    this.agendaMessage = '';
    this.paramService.deleteAgendaGoogle(agenda.id).subscribe({
      next: () => {
        this.agendasGoogle = this.agendasGoogle.filter(item => item.id !== agenda.id);
        this.agendaEnregistrement = false;
        this.agendaMessage = 'Agenda supprimé.';
      },
      error: response => this.afficherErreurAgenda(response)
    });
  }

  enregistrerAgendas(): void {
    if (this.agendaEnregistrement) {
      return;
    }
    if (this.agendasGoogle.some(agenda => !agenda.nom.trim() || !agenda.source.trim())) {
      this.agendaErreur = "Le nom et l'identifiant public sont obligatoires pour chaque agenda.";
      return;
    }
    this.agendaEnregistrement = true;
    this.agendaErreur = '';
    this.agendaMessage = '';
    const misesAJour = this.agendasGoogle
      .filter(agenda => agenda.id != null)
      .map(agenda => this.paramService.updateAgendaGoogle(agenda));
    if (misesAJour.length === 0) {
      this.agendaEnregistrement = false;
      return;
    }
    forkJoin(misesAJour).subscribe({
      next: agendas => {
        this.agendasGoogle = agendas.sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.agendaEnregistrement = false;
        this.agendaMessage = 'Agendas enregistrés.';
      },
      error: response => this.afficherErreurAgenda(response)
    });
  }

  private afficherErreurAgenda(response: any): void {
    this.agendaEnregistrement = false;
    this.agendaErreur = response?.error?.message
      || response?.error?.detail
      || "La configuration des agendas n'a pas pu être enregistrée.";
  }

  private prochaineCouleur(): string {
    const palette = ['#4285F4', '#DB4437', '#F4B400', '#0F9D58', '#AB47BC', '#00ACC1'];
    return palette[this.agendasGoogle.length % palette.length];
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
        this.paramTexts = data.filter(param => param.paramName !== 'Google_Agendas');

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


  nouvelleAnnee(){
    this.adherentService.nouvelleAnnee().subscribe(
      data => {
      console.log(data)

      },
      err => {
        ;
      }
    );
  }
}
