import { Component, ElementRef, EventEmitter, HostListener, inject, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faPencilSquare, faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { ManagedSection, SectionManagementService } from '../../_services/section-management.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Activite, ChatConfiguration, ERole } from '../../models';
import { forkJoin, map, of } from 'rxjs';

@Component({
  selector: 'app-chat-administration',
  standalone: true,
  imports: [FormsModule, FaIconComponent],
  templateUrl: './chat-administration.component.html',
  styleUrls: ['./chat-administration.component.css']
})
export class ChatAdministrationComponent implements OnInit {
  private readonly paramService = inject(ParamService);
  private readonly activiteService = inject(ActiviteService);
  private readonly sectionManagement = inject(SectionManagementService);
  private readonly storage = inject(TokenStorageService);
  readonly isGlobalAdmin = this.storage.getUser().roles?.includes(ERole.ROLE_ADMIN) ?? false;
  @Input() managedSections: ManagedSection[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() chatsChanged = new EventEmitter<void>();
  @ViewChild('closeButton') closeButton?: ElementRef<HTMLButtonElement>;
  readonly faPlus = faPlus;
  readonly faPencilSquare = faPencilSquare;
  readonly faTrash = faTrash;
  chats: ChatConfiguration[] = [];
  sections: { id?: number; nom: string }[] = [];
  activites: Activite[] = [];
  chatCreationVisible = false;
  nouveauChatNom = '';
  nouveauChatCible: ChatConfiguration['cible'] = 'ASSOCIATION';
  nouveauChatCibleId?: number;
  chatEnregistrement = false;
  chatMessage = '';
  chatErreur = '';
  chatEnEdition?: number;
  chatEnEditionNom = '';
  chatEnEditionCible: ChatConfiguration['cible'] = 'SECTION';
  chatEnEditionCibleId?: number;
  readonly rolesChat = [
    { code: ERole.ROLE_USER, libelle: 'Adhérents' },
    { code: ERole.ROLE_SECRETAIRE, libelle: 'Secrétaires' },
    { code: ERole.ROLE_BUREAU, libelle: 'Bureau' },
    { code: ERole.ROLE_MEMBRECA, libelle: 'Membres du CA' },
    { code: ERole.ROLE_ADMIN, libelle: 'Administrateurs' },
    { code: ERole.ROLE_COMPTABLE, libelle: 'Comptables' },
    { code: ERole.ROLE_ENCADRANT, libelle: 'Encadrants' },
    { code: ERole.ROLE_COMMUNICATION_GLOBAL, libelle: 'Communication globale' },
    { code: ERole.ROLE_RESPONSABLE_BOUTIQUE, libelle: 'Responsables de la boutique' }
  ];
  droitsNouveauChat: Record<string, { lecture: boolean; ecriture: boolean }> = {};
  droitsChatEnEdition: Record<string, { lecture: boolean; ecriture: boolean }> = {};

  ngOnInit(): void {
    this.getChats();
    if (this.isGlobalAdmin) {
      this.paramService.getSections().subscribe({
        next: sections => this.sections = sections.sort((a, b) => a.nom.localeCompare(b.nom, 'fr')),
        error: () => this.chatErreur = 'La liste des sections n’a pas pu être chargée.'
      });
      this.getActivites();
    } else {
      this.sections = [...this.managedSections].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
    }
    queueMicrotask(() => this.closeButton?.nativeElement.focus());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.close(); }

  close(): void {
    if (!this.chatEnregistrement) this.closed.emit();
  }

  getChats(): void {
    const request = this.isGlobalAdmin ? this.paramService.getChats()
      : this.managedSections.length
        ? forkJoin(this.managedSections.map(section => this.sectionManagement.chats(section.id))).pipe(map(lists => lists.flat()))
        : of([] as ChatConfiguration[]);
    request.subscribe({
      next: chats => this.chats = chats,
      error: () => this.chatErreur = 'La liste des chats n’a pas pu être chargée.'
    });
  }

  getActivites(): void {
    this.activiteService.getAll().subscribe({
      next: activites => this.activites = activites.sort((a, b) => a.nom.localeCompare(b.nom, 'fr')),
      error: () => this.chatErreur = 'La liste des activités n’a pas pu être chargée.'
    });
  }

  afficherCreationChat(): void {
    if (this.chatEnregistrement) {
      return;
    }
    this.chatEnEdition = undefined;
    this.chatCreationVisible = true;
    this.nouveauChatNom = '';
    this.nouveauChatCible = this.isGlobalAdmin ? 'ASSOCIATION' : 'SECTION';
    this.nouveauChatCibleId = !this.isGlobalAdmin && this.sections.length === 1 ? this.sections[0].id : undefined;
    this.droitsNouveauChat = Object.fromEntries(this.rolesChat.map(role => [role.code, { lecture: false, ecriture: false }]));
    this.chatErreur = '';
    this.chatMessage = '';
  }

  basculerLectureChat(role: ERole): void {
    const droit = this.droitsNouveauChat[role];
    droit.lecture = !droit.lecture;
    if (!droit.lecture) {
      droit.ecriture = false;
    }
  }

  basculerEcritureChat(role: ERole): void {
    const droit = this.droitsNouveauChat[role];
    droit.ecriture = !droit.ecriture;
    if (droit.ecriture) {
      droit.lecture = true;
    }
  }

  annulerCreationChat(): void {
    if (!this.chatEnregistrement) {
      this.chatCreationVisible = false;
    }
  }

  creerChat(): void {
    const nom = this.nouveauChatNom.trim();
    const permissions = this.rolesChat.filter(role => this.droitsNouveauChat[role.code]?.lecture).map(role => ({
      role: role.code,
      ecriture: this.droitsNouveauChat[role.code].ecriture
    }));
    this.chatErreur = '';
    this.chatMessage = '';
    if (!nom || (this.nouveauChatCible === 'ASSOCIATION' && permissions.length === 0) || (this.nouveauChatCible !== 'ASSOCIATION' && this.nouveauChatCibleId == null)) {
      this.chatErreur = 'Saisissez un nom et un rattachement. Pour un chat global, sélectionnez au moins un rôle en lecture.';
      return;
    }
    if (!this.isGlobalAdmin && (this.nouveauChatCible !== 'SECTION'
      || !this.managedSections.some(section => section.id === this.nouveauChatCibleId))) {
      this.chatErreur = 'Choisissez une section qui vous est confiée.';
      return;
    }
    this.chatEnregistrement = true;
    const request = this.isGlobalAdmin
      ? this.paramService.createChat({ nom, cible: this.nouveauChatCible, cibleId: this.nouveauChatCibleId, permissions })
      : this.sectionManagement.createChat(this.nouveauChatCibleId!, { nom, cible: 'SECTION', cibleId: this.nouveauChatCibleId, permissions });
    request.subscribe({
      next: chat => {
        this.chats = [...this.chats, chat].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.chatCreationVisible = false;
        this.chatEnregistrement = false;
        this.chatMessage = 'Chat créé.';
        this.chatsChanged.emit();
      },
      error: response => {
        this.chatEnregistrement = false;
        this.chatErreur = response?.error?.message || response?.error?.detail || 'Le chat n’a pas pu être créé.';
      }
    });
  }

  modifierChat(chat: ChatConfiguration): void {
    if (this.chatEnregistrement || chat.id == null) {
      return;
    }
    if (!this.isGlobalAdmin && (chat.cible !== 'SECTION'
      || !this.managedSections.some(section => section.id === chat.cibleId))) return;
    this.chatCreationVisible = false;
    this.chatEnEdition = chat.id;
    this.chatEnEditionNom = chat.nom;
    this.chatEnEditionCible = chat.cible;
    this.chatEnEditionCibleId = chat.cibleId;
    this.droitsChatEnEdition = Object.fromEntries(this.rolesChat.map(role => {
      const permission = chat.permissions.find(item => item.role === role.code);
      return [role.code, { lecture: permission != null, ecriture: permission?.ecriture ?? false }];
    }));
    this.chatErreur = '';
    this.chatMessage = '';
  }

  annulerModificationChat(): void {
    if (!this.chatEnregistrement) {
      this.chatEnEdition = undefined;
    }
  }

  basculerLectureChatEnEdition(role: ERole): void {
    const droit = this.droitsChatEnEdition[role];
    droit.lecture = !droit.lecture;
    if (!droit.lecture) {
      droit.ecriture = false;
    }
  }

  basculerEcritureChatEnEdition(role: ERole): void {
    const droit = this.droitsChatEnEdition[role];
    droit.ecriture = !droit.ecriture;
    if (droit.ecriture) {
      droit.lecture = true;
    }
  }

  enregistrerChat(chat: ChatConfiguration): void {
    const nom = this.chatEnEditionNom.trim();
    const permissions = this.rolesChat.filter(role => this.droitsChatEnEdition[role.code]?.lecture).map(role => ({
      role: role.code,
      ecriture: this.droitsChatEnEdition[role.code].ecriture
    }));
    if (this.chatEnregistrement || chat.id == null) {
      return;
    }
    this.chatErreur = '';
    this.chatMessage = '';
    if (!nom || (this.chatEnEditionCible === 'ASSOCIATION' && permissions.length === 0) || (this.chatEnEditionCible !== 'ASSOCIATION' && this.chatEnEditionCibleId == null)) {
      this.chatErreur = 'Saisissez un nom et un rattachement. Pour un chat global, sélectionnez au moins un rôle en lecture.';
      return;
    }
    if (!this.isGlobalAdmin && (chat.cible !== 'SECTION' || this.chatEnEditionCible !== 'SECTION'
      || chat.cibleId !== this.chatEnEditionCibleId
      || !this.managedSections.some(section => section.id === chat.cibleId))) {
      this.chatErreur = 'Ce chat doit rester dans sa section.';
      return;
    }
    this.chatEnregistrement = true;
    const changes = { id: chat.id, nom, cible: this.chatEnEditionCible, cibleId: this.chatEnEditionCibleId, permissions };
    const request = this.isGlobalAdmin ? this.paramService.updateChat(changes)
      : this.sectionManagement.updateChat(chat.cibleId!, changes);
    request.subscribe({
      next: chatEnregistre => {
        this.chats = this.chats.map(item =>
          item.id === chatEnregistre.id ? chatEnregistre : item);
        this.chatEnEdition = undefined;
        this.chatEnregistrement = false;
        this.chatMessage = 'Chat enregistré.';
        this.chatsChanged.emit();
      },
      error: response => {
        this.chatEnregistrement = false;
        this.chatErreur = response?.error?.message || response?.error?.detail || 'Le chat n’a pas pu être enregistré.';
      }
    });
  }

  supprimerChat(chat: ChatConfiguration): void {
    if (this.chatEnregistrement || chat.id == null) {
      return;
    }
    if (!this.isGlobalAdmin && (chat.cible !== 'SECTION'
      || !this.managedSections.some(section => section.id === chat.cibleId))) return;
    this.chatEnregistrement = true;
    this.chatErreur = '';
    this.chatMessage = '';
    const request = this.isGlobalAdmin ? this.paramService.deleteChat(chat.id)
      : this.sectionManagement.deleteChat(chat.cibleId!, chat.id);
    request.subscribe({
      next: () => {
        this.chats = this.chats.filter(item => item.id !== chat.id);
        if (this.chatEnEdition === chat.id) {
          this.chatEnEdition = undefined;
        }
        this.chatEnregistrement = false;
        this.chatMessage = 'Chat supprimé.';
        this.chatsChanged.emit();
      },
      error: response => {
        this.chatEnregistrement = false;
        this.chatErreur = response?.error?.message || response?.error?.detail || 'Le chat n’a pas pu être supprimé.';
      }
    });
  }

  resumePermissionsChat(chat: ChatConfiguration): string {
    return chat.permissions.map(permission => {
      const role = this.rolesChat.find(item => item.code === permission.role)?.libelle ?? permission.role;
      return `${role} : ${permission.ecriture ? 'lecture et écriture' : 'lecture'}`;
    }).join(' · ');
  }

  cibleChat(chat: ChatConfiguration): string {
    return chat.cible === 'ASSOCIATION' ? 'Globale' : chat.cibleNom || (chat.cible === 'SECTION' ? 'Section' : 'Activité');
  }
}
