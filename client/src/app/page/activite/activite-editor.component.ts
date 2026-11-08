import { AfterViewInit, Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output, TemplateRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleXmark, faGear } from '@fortawesome/free-solid-svg-icons';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { Activite, SectionConfiguration } from '../../models';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ToastService } from '../../_services/toast.service';
import { ManagedSection } from '../../_services/section-management.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { TeamManagementComponent } from '../../template/team-management/team-management.component';
import { Team, TeamState } from '../../_services/team.service';

@Component({ selector: 'app-activite-editor', templateUrl: './activite-editor.component.html', styleUrls: ['./activite-editor.component.css'],
  imports: [FormsModule, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, FaIconComponent, TeamManagementComponent] })
export class ActiviteEditorComponent implements OnInit, AfterViewInit, OnDestroy {
  private refresh = registerApiViewRefresh();
  private activities = inject(ActiviteService);
  private params = inject(ParamService);
  private tokens = inject(TokenStorageService);
  private toasts = inject(ToastService);
  private modal = inject(NgbModal);
  private optionsModal?: NgbModalRef;
  @ViewChild('options', { static: true }) options!: TemplateRef<unknown>;
  @ViewChild(TeamManagementComponent) teamManagement?: TeamManagementComponent;
  @Input() activite!: Activite;
  @Input() scopedToManagedSections = false;
  @Input() managedSections: ManagedSection[] = [];
  @Output() saved = new EventEmitter<Activite>();
  sections: (SectionConfiguration | ManagedSection)[] = [];
  creatingTeam = false;
  selectedTeamId?: number;
  teams: Team[] = [];
  teamsBusy = false;
  saving = false;
  faCircleCheck = faCircleCheck; faCircleXmark = faCircleXmark; faGear = faGear;
  get canConfigureTeams() { return this.scopedToManagedSections || !!this.tokens.getUser().roles?.includes('ROLE_ADMIN'); }
  get canEditSelectedTeam() { return !!this.teams.find(team => team.id === this.selectedTeamId)?.canManage; }
  get sectionSelectionnee() { return !!this.activite.section?.id && (!this.scopedToManagedSections || this.managedSections.some(s => s.id === this.activite.section?.id)); }
  ngOnInit() {
    if (this.scopedToManagedSections) {
      this.sections = this.managedSections;
      if (!this.activite.id && this.sections.length === 1) this.activite.section = this.sections[0] as SectionConfiguration;
    } else this.params.getSections().subscribe({ next: s => this.sections = s, error: e => this.toasts.error(e.message) });
  }
  ngAfterViewInit() { if (!this.activite.id) this.openOptions(); }
  ngOnDestroy() { this.optionsModal?.dismiss(); }
  openOptions() {
    if (this.optionsModal) return;
    const ref = this.modal.open(this.options, { size: 'lg', scrollable: true, ariaLabelledBy: 'activity-options-title' });
    this.optionsModal = ref;
    ref.result.then(() => this.optionsModal = undefined, () => this.optionsModal = undefined);
  }
  comparerSections(a?: SectionConfiguration | ManagedSection, b?: SectionConfiguration | ManagedSection) { return a?.id === b?.id; }
  selectTeam(id: number) {
    if (this.teamsBusy) return;
    this.creatingTeam = false;
    this.selectedTeamId = id;
  }
  addTeam() { if (!this.teamsBusy) this.creatingTeam = true; }
  editTeam() { if (!this.teamsBusy && !this.creatingTeam && this.canEditSelectedTeam) this.teamManagement?.editSelectedTeam(); }
  updateTeams(state: TeamState) {
    this.teams = state.equipes.filter(team => !team.archivee);
    if (!this.teams.some(team => team.id === this.selectedTeamId)) {
      const team = this.teams.find(team => team.principale) ?? this.teams[0];
      this.selectedTeamId = team?.id;
    }
  }
  teamCreated(team: Team) { this.selectTeam(team.id); }
  enregistrer() {
    if (this.saving) return;
    if (!this.sectionSelectionnee) { this.toasts.warning('Sélectionnez une section avant d’enregistrer l’activité.', 'Section obligatoire'); return; }
    this.saving = true;
    this.activities.save(this.activite, this.scopedToManagedSections).subscribe({
      next: activity => { this.saving = false; this.optionsModal?.close(); this.toasts.success('Activité enregistrée.'); this.saved.emit(activity); },
      error: e => { this.saving = false; this.toasts.error(e.error?.detail || e.error?.message || 'L’activité n’a pas pu être enregistrée.'); }
    });
  }
}
