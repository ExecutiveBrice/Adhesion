import { Component, OnInit, inject } from '@angular/core';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { UserService } from '../../_services/user.service';
import { ParamService } from '../../_services/param.service';

import { AgendaGoogleConfiguration, ERole, ParamBoolean, ParamNumber, ParamText, SalleConfiguration, SectionConfiguration, UserLite } from 'src/app/models';
import { forkJoin } from 'rxjs';
import { TacheSeanceConfiguration } from '../../models/responsabiliteSeance';
import { ICONE_TACHE_PAR_DEFAUT, COULEUR_TACHE_PAR_DEFAUT } from '../../_helpers/tache-icons';
import { IconeTacheSelectComponent } from '../../template/icone-tache-select/icone-tache-select.component';
import { faCalendarDays, faCircleCheck, faCircleXmark, faFont, faHashtag, faLayerGroup, faLocationDot, faPencilSquare, faPlus, faSliders, faTrash, faUserShield, faWrench } from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import {AuthService} from "../../_services/auth.service";
import {TokenStorageService} from "../../_services/token-storage.service";
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { NgbAccordionBody, NgbAccordionButton, NgbAccordionCollapse, NgbAccordionDirective, NgbAccordionHeader, NgbAccordionItem } from '@ng-bootstrap/ng-bootstrap/accordion';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { OrderByPipe } from '../../_helpers/sort.pipe';
import { UserCheckboxDropdownComponent } from '../../template/user-checkbox-dropdown/user-checkbox-dropdown.component';
import { UtilisateurSelectionnable } from '../../models/utilisateurSelectionnable';

type RoleUtilisateur = ERole.ROLE_ADMIN | ERole.ROLE_MEMBRECA | ERole.ROLE_BUREAU |
  ERole.ROLE_SECRETAIRE | ERole.ROLE_COMPTABLE | ERole.ROLE_ENCADRANT |
  ERole.ROLE_COMMUNICATION_GLOBAL | ERole.ROLE_RESPONSABLE_BOUTIQUE;


@Component({
    selector: 'app-board-admin',
    templateUrl: './board-admin.component.html',
    styleUrls: ['./board-admin.component.css'],
    imports: [FaIconComponent, FormsModule, NgClass, NgbAccordionBody, NgbAccordionButton, NgbAccordionCollapse, NgbAccordionDirective, NgbAccordionHeader, NgbAccordionItem, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, NgbTooltip, OrderByPipe, UserCheckboxDropdownComponent, IconeTacheSelectComponent]
})
export class BoardAdminComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private tokenStorage = inject(TokenStorageService);
  private authService = inject(AuthService);
  private paramService = inject(ParamService);
  private userService = inject(UserService);
  private adherentService = inject(AdherentService);

  faCircleXmark = faCircleXmark;
  faCircleCheck = faCircleCheck;
  faCalendarDays = faCalendarDays;
  faLocationDot = faLocationDot;
  faPlus = faPlus;
  faTrash = faTrash;
  faSliders = faSliders;
  faHashtag = faHashtag;
  faFont = faFont;
  faUserShield = faUserShield;
  faWrench = faWrench;
  faLayerGroup = faLayerGroup;
  faPencilSquare = faPencilSquare;
  paramBooleans: ParamBoolean[] = [];
  paramTexts: ParamText[] = [];
  agendasGoogle: AgendaGoogleConfiguration[] = [];
  nouvelAgendaNom = '';
  nouvelAgendaSource = '';
  nouvelAgendaCouleur = '#4285F4';
  nouvelAgendaVisibleApp = true;
  agendaEnregistrement = false;
  agendaMessage = '';
  agendaErreur = '';
  salles: SalleConfiguration[] = [];
  tachesSeance: TacheSeanceConfiguration[] = [];
  nouvelleTacheSeanceNom = '';
  nouvelleTacheSeanceIcone = ICONE_TACHE_PAR_DEFAUT;
  nouvelleTacheSeanceCouleur = COULEUR_TACHE_PAR_DEFAUT;
  tacheSeanceEnregistrement = false;
  tacheSeanceMessage = '';
  tacheSeanceErreur = '';
  nouvelleSalleNom = '';
  nouvelleSalleAdresse = '';
  nouvelleSalleCouleur = '#0F9D58';
  salleEnregistrement = false;
  salleMessage = '';
  salleErreur = '';
  sections: SectionConfiguration[] = [];
  nouvelleSectionNom = '';
  nouveauSectionType: SectionConfiguration['type'] = 'NON_COMPETITIVE';
  nouvelleSectionCouleur = '#5CBBAF';
  sectionEnregistrement = false;
  sectionMessage = '';
  sectionErreur = '';
  sectionEnEdition?: number;
  usersLite: UserLite[] = [];
  utilisateursSelectionnables: UtilisateurSelectionnable[] = [];
  candidatsReferentsParSection: Record<number, UtilisateurSelectionnable[]> = {};
  selectionsRoles: Record<RoleUtilisateur, UtilisateurSelectionnable[]> = {
    [ERole.ROLE_ADMIN]: [],
    [ERole.ROLE_MEMBRECA]: [],
    [ERole.ROLE_BUREAU]: [],
    [ERole.ROLE_SECRETAIRE]: [],
    [ERole.ROLE_COMPTABLE]: [],
    [ERole.ROLE_ENCADRANT]: [],
    [ERole.ROLE_COMMUNICATION_GLOBAL]: [],
    [ERole.ROLE_RESPONSABLE_BOUTIQUE]: []
  };
  rolesEnCoursDeMiseAJour: Partial<Record<RoleUtilisateur, boolean>> = {};
  maintenanceEnCours = false;
  maintenanceMessage = '';
  maintenanceErreur = '';
  readonly rolesUtilisateurs: { code: RoleUtilisateur; libelle: string }[] = [
    { code: ERole.ROLE_ADMIN, libelle: 'Administrateurs du site' },
    { code: ERole.ROLE_MEMBRECA, libelle: "Membres du CA de l’ALOD" },
    { code: ERole.ROLE_BUREAU, libelle: 'Membres du bureau de l’ALOD' },
    { code: ERole.ROLE_SECRETAIRE, libelle: 'Secrétaires de l’ALOD' },
    { code: ERole.ROLE_COMPTABLE, libelle: 'Comptables de l’ALOD' },
    { code: ERole.ROLE_ENCADRANT, libelle: 'Encadrants de l’ALOD' },
    { code: ERole.ROLE_COMMUNICATION_GLOBAL, libelle: 'Communication globale' },
    { code: ERole.ROLE_RESPONSABLE_BOUTIQUE, libelle: 'Responsables de la boutique' }
  ];

  ngOnInit(): void {
    this.getAllBoolean()
    this.getAllText()
    this.getAllNumber()
    this.getAgendasGoogle()
    this.getSalles()
    this.getTachesSeance();
    this.getSections()
    this.fillLists()
  }


  fillLists() {
    this.userService.getAllLite().subscribe(
      data => {
        this.usersLite = data;
        this.utilisateursSelectionnables = data.map(user => ({
          id: user.id,
          prenom: '',
          nom: user.adherent || user.username
        }));
        this.selectionsRoles = this.rolesUtilisateurs.reduce((selections, role) => {
          selections[role.code] = this.utilisateursSelectionnables.filter(utilisateur =>
            data.some(user => user.id === utilisateur.id && user.roles.includes(role.code)));
          return selections;
        }, {} as Record<RoleUtilisateur, UtilisateurSelectionnable[]>);
      },
      err => {

      }
    );
  }

  modifierUtilisateursRole(role: RoleUtilisateur, selection: UtilisateurSelectionnable[]): void {
    const selectionPrecedente = this.selectionsRoles[role];
    const idsSelectionnes = new Set(selection.map(utilisateur => utilisateur.id));
    const idsPrecedents = new Set(selectionPrecedente.map(utilisateur => utilisateur.id));
    const utilisateursAjoutes = selection.filter(utilisateur => !idsPrecedents.has(utilisateur.id));
    const utilisateursRetires = selectionPrecedente.filter(utilisateur => !idsSelectionnes.has(utilisateur.id));

    this.selectionsRoles = { ...this.selectionsRoles, [role]: selection };
    const utilisateursParId = new Map(this.usersLite.map(utilisateur => [utilisateur.id, utilisateur]));
    const misesAJour = [
      ...utilisateursAjoutes.map(utilisateur => this.userService.grantUser(role, utilisateursParId.get(utilisateur.id)?.username ?? '')),
      ...utilisateursRetires.map(utilisateur => this.userService.unGrantUser(role, utilisateursParId.get(utilisateur.id)?.username ?? ''))
    ];

    if (misesAJour.length === 0) {
      return;
    }

    this.rolesEnCoursDeMiseAJour[role] = true;
    forkJoin(misesAJour).subscribe({
      next: () => {
        this.rolesEnCoursDeMiseAJour[role] = false;
        this.fillLists();
      },
      error: () => {
        this.rolesEnCoursDeMiseAJour[role] = false;
        this.fillLists();
      }
    });
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
      couleur: this.nouvelAgendaCouleur,
      isVisisbleApp: this.nouvelAgendaVisibleApp
    }).subscribe({
      next: agenda => {
        this.agendasGoogle = [...this.agendasGoogle, agenda].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nouvelAgendaNom = '';
        this.nouvelAgendaSource = '';
        this.nouvelAgendaCouleur = this.prochaineCouleur();
        this.nouvelAgendaVisibleApp = true;
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

  getSalles(): void {
    this.paramService.getSalles().subscribe({
      next: salles => this.salles = salles,
      error: () => this.salleErreur = 'La liste des salles n’a pas pu être chargée.'
    });
  }

  getTachesSeance(): void {
    this.paramService.getTachesSeance().subscribe({
      next: taches => this.tachesSeance = taches.map(tache => ({ ...tache,
        icone: tache.icone || ICONE_TACHE_PAR_DEFAUT, couleur: tache.couleur || COULEUR_TACHE_PAR_DEFAUT })),
      error: response => this.afficherErreurTacheSeance(response)
    });
  }

  ajouterTacheSeance(): void {
    if (this.tacheSeanceEnregistrement) return;
    this.tacheSeanceErreur = '';
    this.tacheSeanceMessage = '';
    const nom = this.nouvelleTacheSeanceNom.trim();
    if (!nom) {
      this.tacheSeanceErreur = 'Saisissez le nom de la tâche.';
      return;
    }
    this.tacheSeanceEnregistrement = true;
    this.paramService.createTacheSeance({ nom, icone: this.nouvelleTacheSeanceIcone,
      couleur: this.nouvelleTacheSeanceCouleur }).subscribe({
      next: tache => {
        this.tachesSeance = [...this.tachesSeance, tache].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nouvelleTacheSeanceNom = '';
        this.nouvelleTacheSeanceIcone = ICONE_TACHE_PAR_DEFAUT;
        this.nouvelleTacheSeanceCouleur = COULEUR_TACHE_PAR_DEFAUT;
        this.tacheSeanceEnregistrement = false;
        this.tacheSeanceMessage = 'Tâche ajoutée.';
      },
      error: response => this.afficherErreurTacheSeance(response)
    });
  }

  enregistrerTacheSeance(tache: TacheSeanceConfiguration): void {
    if (this.tacheSeanceEnregistrement || tache.id == null) return;
    this.tacheSeanceErreur = '';
    this.tacheSeanceMessage = '';
    if (!tache.nom.trim()) {
      this.tacheSeanceErreur = 'Le nom de la tâche est obligatoire.';
      return;
    }
    this.tacheSeanceEnregistrement = true;
    this.paramService.updateTacheSeance(tache).subscribe({
      next: resultat => {
        this.tachesSeance = this.tachesSeance.map(item => item.id === resultat.id ? resultat : item)
          .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.tacheSeanceEnregistrement = false;
        this.tacheSeanceMessage = 'Tâche enregistrée.';
      },
      error: response => this.afficherErreurTacheSeance(response)
    });
  }

  supprimerTacheSeance(tache: TacheSeanceConfiguration): void {
    if (this.tacheSeanceEnregistrement || tache.id == null) return;
    this.tacheSeanceErreur = '';
    this.tacheSeanceMessage = '';
    this.tacheSeanceEnregistrement = true;
    this.paramService.deleteTacheSeance(tache.id).subscribe({
      next: () => {
        this.tachesSeance = this.tachesSeance.filter(item => item.id !== tache.id);
        this.tacheSeanceEnregistrement = false;
        this.tacheSeanceMessage = 'Tâche supprimée.';
      },
      error: response => this.afficherErreurTacheSeance(response)
    });
  }

  private afficherErreurTacheSeance(response: any): void {
    this.tacheSeanceEnregistrement = false;
    this.tacheSeanceErreur = response?.error?.message || response?.error?.detail
      || 'Les tâches des séances n’ont pas pu être enregistrées ou chargées.';
  }

  ajouterSalle(): void {
    const nom = this.nouvelleSalleNom.trim();
    const adresse = this.nouvelleSalleAdresse.trim();
    this.salleErreur = '';
    this.salleMessage = '';
    if (!nom || !adresse) {
      this.salleErreur = 'Saisissez le nom et l’adresse de la salle.';
      return;
    }
    this.salleEnregistrement = true;
    this.paramService.createSalle({ nom, adresse, couleur: this.nouvelleSalleCouleur }).subscribe({
      next: salle => {
        this.salles = [...this.salles, salle].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nouvelleSalleNom = '';
        this.nouvelleSalleAdresse = '';
        this.nouvelleSalleCouleur = this.prochaineCouleurSalle();
        this.salleEnregistrement = false;
        this.salleMessage = 'Salle ajoutée.';
      },
      error: response => this.afficherErreurSalle(response)
    });
  }

  supprimerSalle(index: number): void {
    const salle = this.salles[index];
    if (salle.id == null || this.salleEnregistrement) {
      return;
    }
    this.salleEnregistrement = true;
    this.salleErreur = '';
    this.salleMessage = '';
    this.paramService.deleteSalle(salle.id).subscribe({
      next: () => {
        this.salles = this.salles.filter(item => item.id !== salle.id);
        this.salleEnregistrement = false;
        this.salleMessage = 'Salle supprimée.';
      },
      error: response => this.afficherErreurSalle(response)
    });
  }

  enregistrerSalles(): void {
    if (this.salleEnregistrement) {
      return;
    }
    if (this.salles.some(salle => !salle.nom.trim() || !salle.adresse.trim())) {
      this.salleErreur = 'Le nom et l’adresse sont obligatoires pour chaque salle.';
      return;
    }
    const misesAJour = this.salles.filter(salle => salle.id != null).map(salle => this.paramService.updateSalle(salle));
    if (misesAJour.length === 0) {
      return;
    }
    this.salleEnregistrement = true;
    this.salleErreur = '';
    this.salleMessage = '';
    forkJoin(misesAJour).subscribe({
      next: salles => {
        this.salles = salles.sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.salleEnregistrement = false;
        this.salleMessage = 'Salles enregistrées.';
      },
      error: response => this.afficherErreurSalle(response)
    });
  }

  private afficherErreurSalle(response: any): void {
    this.salleEnregistrement = false;
    this.salleErreur = response?.error?.message || response?.error?.detail
      || 'La configuration des salles n’a pas pu être enregistrée.';
  }

  private prochaineCouleurSalle(): string {
    const palette = ['#0F9D58', '#4285F4', '#DB4437', '#F4B400', '#AB47BC', '#00ACC1'];
    return palette[this.salles.length % palette.length];
  }

  getSections(): void {
    this.paramService.getSections().subscribe({
      next: sections => {
        this.sections = sections.map(section => ({ ...section, couleur: section.couleur || '#5CBBAF' }));
        this.sections.forEach(section => {
          if (section.id != null) {
            this.paramService.getSectionReferentCandidates(section.id).subscribe({
              next: candidats => this.candidatsReferentsParSection[section.id!] = candidats.map(candidat => ({
                id: candidat.id,
                prenom: '',
                nom: candidat.nom
              })),
              error: () => this.sectionErreur = 'Les référents possibles n’ont pas pu être chargés.'
            });
          }
        });
      },
      error: () => this.sectionErreur = 'La liste des sections n’a pas pu être chargée.'
    });
  }

  referentsSelectionnes(section: SectionConfiguration): UtilisateurSelectionnable[] {
    const idsSelectionnes = new Set(section.referentUserIds ?? []);
    return (this.candidatsReferentsParSection[section.id!] ?? [])
      .filter(utilisateur => idsSelectionnes.has(utilisateur.id));
  }

  modifierReferentsSection(section: SectionConfiguration, selection: UtilisateurSelectionnable[]): void {
    section.referentUserIds = selection.map(utilisateur => utilisateur.id);
  }

  ajouterSection(): void {
    const nom = this.nouvelleSectionNom.trim();
    this.sectionErreur = '';
    this.sectionMessage = '';
    if (!nom) {
      this.sectionErreur = 'Saisissez le nom de la section.';
      return;
    }
    this.sectionEnregistrement = true;
    this.paramService.createSection({ nom, type: this.nouveauSectionType, couleur: this.nouvelleSectionCouleur }).subscribe({
      next: section => {
        this.sections = [...this.sections, section].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nouvelleSectionNom = '';
        this.nouveauSectionType = 'NON_COMPETITIVE';
        this.nouvelleSectionCouleur = '#5CBBAF';
        this.sectionEnregistrement = false;
        this.sectionMessage = 'Section ajoutée.';
      },
      error: response => {
        this.sectionEnregistrement = false;
        this.sectionErreur = response?.error?.message || response?.error?.detail
          || 'La section n’a pas pu être ajoutée.';
      }
    });
  }

  modifierSection(index: number): void {
    if (!this.sectionEnregistrement) {
      this.sectionEnEdition = index;
      this.sectionMessage = '';
      this.sectionErreur = '';
    }
  }

  enregistrerSection(index: number): void {
    const section = this.sections[index];
    if (this.sectionEnregistrement || section.id == null) {
      return;
    }
    if (!section.nom.trim()) {
      this.sectionErreur = 'Le nom de la section est obligatoire.';
      return;
    }
    this.sectionEnregistrement = true;
    this.sectionErreur = '';
    this.sectionMessage = '';
    this.paramService.updateSection(section).subscribe({
      next: sectionEnregistree => {
        this.sections[index] = sectionEnregistree;
        this.sections = [...this.sections].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.sectionEnEdition = undefined;
        this.sectionEnregistrement = false;
        this.sectionMessage = 'Section enregistrée.';
      },
      error: response => {
        this.sectionEnregistrement = false;
        this.sectionErreur = response?.error?.message || response?.error?.detail
          || 'La section n’a pas pu être enregistrée.';
      }
    });
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


  executerMaintenance(action: 'nouvelleAnnee' | 'cleanNotification' | 'cleanUserAlone' | 'rappel'): void {
    const libelles = {
      nouvelleAnnee: 'Lancer la préparation de la nouvelle année',
      cleanNotification: 'Supprimer les notifications obsolètes',
      cleanUserAlone: 'Supprimer les comptes sans adhérent associé',
      rappel: 'Envoyer les rappels en attente'
    };
    if (this.maintenanceEnCours || !window.confirm(`${libelles[action]} ? Cette action sera journalisée.`)) {
      return;
    }
    this.maintenanceEnCours = true;
    this.maintenanceMessage = '';
    this.maintenanceErreur = '';
    const request = action === 'nouvelleAnnee'
      ? this.adherentService.nouvelleAnnee()
      : action === 'cleanNotification'
        ? this.adherentService.cleanNotification()
        : action === 'rappel'
          ? this.adherentService.envoyerRappels()
          : this.adherentService.cleanUserAlone();
    request.subscribe({
      next: resultat => {
        this.maintenanceEnCours = false;
        this.maintenanceMessage = action === 'rappel'
          ? `${resultat} rappel(s) envoyé(s).`
          : `${libelles[action]} : terminé.`;
      },
      error: response => {
        this.maintenanceEnCours = false;
        this.maintenanceErreur = response?.error?.message || 'L’action de maintenance a échoué.';
      }
    });
  }
}
