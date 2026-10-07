import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatConfiguration, ERole } from '../../models';
import {
  ManagedSection, SectionActivity, SectionActivityCreation, SectionManagementService, SectionMembership
} from '../../_services/section-management.service';

type Tab = 'chats' | 'activites' | 'adhesions';
type ChatDraft = { id?: number; nom: string; cible: 'SECTION' | 'ACTIVITE'; cibleId?: number };

@Component({
  selector: 'app-mes-sections',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mes-sections.component.html',
  styleUrl: './mes-sections.component.css'
})
export class MesSectionsComponent implements OnInit {
  private readonly service = inject(SectionManagementService);
  readonly jours = [
    { value: 'MONDAY', label: 'Lundi' }, { value: 'TUESDAY', label: 'Mardi' },
    { value: 'WEDNESDAY', label: 'Mercredi' }, { value: 'THURSDAY', label: 'Jeudi' },
    { value: 'FRIDAY', label: 'Vendredi' }, { value: 'SATURDAY', label: 'Samedi' },
    { value: 'SUNDAY', label: 'Dimanche' }
  ];
  readonly roles = [
    { code: ERole.ROLE_USER, label: 'Adhérents' },
    { code: ERole.ROLE_ENCADRANT, label: 'Encadrants' },
    { code: ERole.ROLE_REFERENT_ACTIVITE, label: 'Référents d’activité' },
    { code: ERole.ROLE_REFERENT_SECTION, label: 'Référents de section' },
    { code: ERole.ROLE_COMMUNICATION_SECTION, label: 'Communication de section' },
    { code: ERole.ROLE_SECRETAIRE, label: 'Secrétariat' },
    { code: ERole.ROLE_ADMIN, label: 'Administration' },
    { code: ERole.ROLE_BUREAU, label: 'Bureau' },
    { code: ERole.ROLE_MEMBRECA, label: 'Membres du CA' },
    { code: ERole.ROLE_COMPTABLE, label: 'Comptabilité' },
    { code: ERole.ROLE_COMMUNICATION_GLOBAL, label: 'Communication globale' },
    { code: ERole.ROLE_RESPONSABLE_BOUTIQUE, label: 'Responsables boutique' }
  ];
  readonly statuts = [
    'Attente validation adhérent', 'Attente validation secrétariat', 'Attente création licence',
    'Licence FFBB à compléter', 'Licence générée', 'Licence T',
    'Validée, en attente du certificat médical', 'Validée groupement sportif', 'Validée',
    "Sur liste d'attente", 'Retour ALOD Basket', 'Retour Comité', 'Annulée'
  ];
  sections: ManagedSection[] = [];
  sectionId?: number;
  tab: Tab = 'chats';
  loading = true;
  sectionLoading = false;
  saving = false;
  error = '';
  message = '';
  activities: SectionActivity[] = [];
  memberships: SectionMembership[] = [];
  chats: ChatConfiguration[] = [];
  membershipPage = 0;
  membershipTotalPages = 0;
  membershipTotal = 0;
  membershipSearch = '';
  membershipStatus = '';
  statusDrafts: Record<number, string> = {};
  activityDraft?: SectionActivity;
  creationDraft?: SectionActivityCreation;
  chatDraft?: ChatDraft;
  chatPermissions: Record<string, { lecture: boolean; ecriture: boolean }> = {};
  pendingDeleteChatId?: number;
  private selectionVersion = 0;
  private membershipLoadVersion = 0;

  ngOnInit(): void {
    this.service.sections().subscribe({
      next: sections => {
        this.sections = sections;
        this.loading = false;
        if (sections.length) this.selectSection(sections[0].id);
      },
      error: () => {
        this.loading = false;
        this.error = 'Impossible de charger vos sections.';
      }
    });
  }

  get selectedSection(): ManagedSection | undefined {
    return this.sections.find(section => section.id === this.sectionId);
  }

  selectSection(id: number): void {
    this.sectionId = Number(id);
    this.selectionVersion++;
    this.activityDraft = undefined;
    this.creationDraft = undefined;
    this.chatDraft = undefined;
    this.pendingDeleteChatId = undefined;
    this.activities = [];
    this.memberships = [];
    this.chats = [];
    this.membershipPage = 0;
    this.membershipSearch = '';
    this.membershipStatus = '';
    this.message = '';
    this.error = '';
    this.loadSection();
  }

  selectTab(tab: Tab): void {
    this.tab = tab;
    this.error = '';
    this.message = '';
  }

  private loadSection(): void {
    const sectionId = this.sectionId;
    if (sectionId == null) return;
    const version = this.selectionVersion;
    this.sectionLoading = true;
    let completed = 0;
    const done = () => { if (++completed === 3 && version === this.selectionVersion) this.sectionLoading = false; };
    this.service.activities(sectionId).subscribe({
      next: activities => { if (version === this.selectionVersion) this.activities = activities; done(); },
      error: () => { if (version === this.selectionVersion) this.error = 'Les activités n’ont pas pu être chargées.'; done(); }
    });
    this.service.chats(sectionId).subscribe({
      next: chats => { if (version === this.selectionVersion) this.chats = chats; done(); },
      error: () => { if (version === this.selectionVersion) this.error = 'Les Chats n’ont pas pu être chargés.'; done(); }
    });
    this.loadMemberships(done, version);
  }

  loadMemberships(done?: () => void, version = this.selectionVersion): void {
    const sectionId = this.sectionId;
    if (sectionId == null) return;
    const pageNumber = this.membershipPage;
    const loadVersion = ++this.membershipLoadVersion;
    this.service.memberships(sectionId, pageNumber, this.membershipSearch, this.membershipStatus).subscribe({
      next: page => {
        if (version === this.selectionVersion && loadVersion === this.membershipLoadVersion) {
          this.memberships = page.content;
          this.membershipTotal = page.totalElements;
          this.membershipTotalPages = page.totalPages;
          this.statusDrafts = Object.fromEntries(page.content.map(item => [item.id, item.statut]));
        }
        done?.();
      },
      error: () => {
        if (version === this.selectionVersion && loadVersion === this.membershipLoadVersion)
          this.error = 'Les adhésions n’ont pas pu être chargées.';
        done?.();
      }
    });
  }

  changeMembershipPage(page: number): void {
    if (page < 0 || page >= this.membershipTotalPages) return;
    this.membershipPage = page;
    this.loadMemberships();
  }

  filterMemberships(): void {
    this.membershipPage = 0;
    this.loadMemberships();
  }

  editActivity(activity: SectionActivity): void {
    this.creationDraft = undefined;
    this.activityDraft = { ...activity };
    this.message = '';
    this.error = '';
  }

  startActivity(): void {
    this.activityDraft = undefined;
    this.creationDraft = { nom: '', tarif: 0, nbPlaces: 20, ageMin: 0, ageMax: 99,
      genre: 'Non genrée', jour: 'MONDAY', horaireDebut: '18:00', duree: 60 };
    this.error = '';
    this.message = '';
  }

  createActivity(): void {
    if (this.sectionId == null || !this.creationDraft || this.saving) return;
    const draft = this.creationDraft;
    if (!draft.nom.trim() || draft.tarif < 0 || draft.nbPlaces < 1 || draft.ageMin < 0
        || draft.ageMax < draft.ageMin || draft.duree < 1 || !draft.horaireDebut) {
      this.error = 'Renseignez un nom, un créneau et des valeurs valides.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.service.createActivity(this.sectionId, draft).subscribe({
      next: saved => {
        this.activities = [...this.activities, saved].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.creationDraft = undefined;
        this.saving = false;
        this.message = 'Activité créée.';
      },
      error: response => {
        this.saving = false;
        this.error = this.apiError(response, 'L’activité n’a pas pu être créée.');
      }
    });
  }

  saveActivity(): void {
    if (this.sectionId == null || !this.activityDraft || this.saving) return;
    const draft = this.activityDraft;
    if (!draft.nom.trim() || draft.tarif == null || draft.tarif < 0 || draft.nbPlaces == null || draft.nbPlaces < 0) {
      this.error = 'Renseignez un nom, un tarif et un nombre de places valides.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.service.updateActivity(this.sectionId, draft).subscribe({
      next: saved => {
        this.activities = this.activities.map(item => item.id === saved.id ? saved : item);
        this.activityDraft = undefined;
        this.saving = false;
        this.message = 'Activité enregistrée.';
      },
      error: response => {
        this.saving = false;
        this.error = this.apiError(response, 'L’activité n’a pas pu être enregistrée.');
      }
    });
  }

  saveStatus(membership: SectionMembership): void {
    if (this.sectionId == null || this.saving) return;
    const statut = this.statusDrafts[membership.id];
    if (!this.statuts.includes(statut)) return;
    this.saving = true;
    this.error = '';
    this.message = '';
    this.service.updateStatus(this.sectionId, membership.id, statut).subscribe({
      next: saved => {
        this.memberships = this.memberships.map(item => item.id === saved.id ? saved : item);
        this.saving = false;
        this.message = 'Statut de l’adhésion enregistré.';
        if (this.membershipStatus && this.membershipStatus !== saved.statut) this.loadMemberships();
      },
      error: response => {
        this.saving = false;
        this.error = this.apiError(response, 'Le statut n’a pas pu être enregistré.');
      }
    });
  }

  startChat(chat?: ChatConfiguration): void {
    this.chatDraft = chat
      ? { id: chat.id, nom: chat.nom, cible: chat.cible as 'SECTION' | 'ACTIVITE', cibleId: chat.cibleId }
      : { nom: '', cible: 'SECTION', cibleId: this.sectionId };
    this.chatPermissions = Object.fromEntries(this.roles.map(role => {
      const permission = chat?.permissions.find(item => item.role === role.code);
      return [role.code, { lecture: !!permission, ecriture: permission?.ecriture ?? false }];
    }));
    this.pendingDeleteChatId = undefined;
    this.error = '';
    this.message = '';
  }

  setChatTarget(cible: 'SECTION' | 'ACTIVITE'): void {
    if (!this.chatDraft) return;
    this.chatDraft.cible = cible;
    this.chatDraft.cibleId = cible === 'SECTION' ? this.sectionId : undefined;
  }

  setPermission(role: string, field: 'lecture' | 'ecriture', value: boolean): void {
    const permission = this.chatPermissions[role];
    if (!permission) return;
    permission[field] = value;
    if (field === 'lecture' && !value) permission.ecriture = false;
    if (field === 'ecriture' && value) permission.lecture = true;
  }

  saveChat(): void {
    if (this.sectionId == null || !this.chatDraft || this.saving) return;
    const draft = this.chatDraft;
    if (!draft.nom.trim() || draft.cibleId == null) {
      this.error = 'Saisissez un nom et choisissez un rattachement.';
      return;
    }
    const chat: ChatConfiguration = {
      id: draft.id, nom: draft.nom.trim(), cible: draft.cible, cibleId: draft.cibleId,
      permissions: this.roles.filter(role => this.chatPermissions[role.code]?.lecture)
        .map(role => ({ role: role.code, ecriture: this.chatPermissions[role.code].ecriture }))
    };
    this.saving = true;
    this.error = '';
    const request = chat.id == null
      ? this.service.createChat(this.sectionId, chat)
      : this.service.updateChat(this.sectionId, chat);
    request.subscribe({
      next: saved => {
        this.chats = [...this.chats.filter(item => item.id !== saved.id), saved]
          .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.chatDraft = undefined;
        this.saving = false;
        this.message = 'Chat enregistré.';
      },
      error: response => {
        this.saving = false;
        this.error = this.apiError(response, 'Le Chat n’a pas pu être enregistré.');
      }
    });
  }

  deleteChat(chatId: number): void {
    if (this.sectionId == null || this.saving) return;
    this.saving = true;
    this.error = '';
    this.service.deleteChat(this.sectionId, chatId).subscribe({
      next: () => {
        this.chats = this.chats.filter(chat => chat.id !== chatId);
        this.pendingDeleteChatId = undefined;
        this.saving = false;
        this.message = 'Chat supprimé.';
      },
      error: response => {
        this.saving = false;
        this.error = this.apiError(response, 'Le Chat n’a pas pu être supprimé.');
      }
    });
  }

  private apiError(response: any, fallback: string): string {
    return response?.error?.detail || response?.error?.message || fallback;
  }
}
