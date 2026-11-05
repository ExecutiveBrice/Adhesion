import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleXmark } from '@fortawesome/free-solid-svg-icons';
import { Activite, SectionConfiguration } from '../../models';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ToastService } from '../../_services/toast.service';
import { ManagedSection } from '../../_services/section-management.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { TeamManagementComponent } from '../../template/team-management/team-management.component';
import { Team, TeamState } from '../../_services/team.service';

type ActivityTab = 'options' | 'add-team' | number;

@Component({ selector: 'app-activite-editor', templateUrl: './activite-editor.component.html', styleUrls: ['./activite-editor.component.css'],
  imports: [FormsModule, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, FaIconComponent, TeamManagementComponent] })
export class ActiviteEditorComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private activities = inject(ActiviteService);
  private params = inject(ParamService);
  private tokens = inject(TokenStorageService);
  private toasts = inject(ToastService);
  @Input() activite!: Activite;
  @Input() scopedToManagedSections = false;
  @Input() managedSections: ManagedSection[] = [];
  @Output() saved = new EventEmitter<Activite>();
  sections: (SectionConfiguration | ManagedSection)[] = [];
  activeTab: ActivityTab = 'options';
  selectedTeamId?: number;
  teams: Team[] = [];
  teamsBusy = false;
  saving = false;
  private tabSelected = false;
  faCircleCheck = faCircleCheck; faCircleXmark = faCircleXmark;
  get canConfigureTeams() { return this.scopedToManagedSections || !!this.tokens.getUser().roles?.includes('ROLE_ADMIN'); }
  get sectionSelectionnee() { return !!this.activite.section?.id && (!this.scopedToManagedSections || this.managedSections.some(s => s.id === this.activite.section?.id)); }
  ngOnInit() {
    if (this.scopedToManagedSections) {
      this.sections = this.managedSections;
      if (!this.activite.id && this.sections.length === 1) this.activite.section = this.sections[0] as SectionConfiguration;
    } else this.params.getSections().subscribe({ next: s => this.sections = s, error: e => this.toasts.error(e.message) });
  }
  comparerSections(a?: SectionConfiguration | ManagedSection, b?: SectionConfiguration | ManagedSection) { return a?.id === b?.id; }
  selectTab(tab: ActivityTab) {
    if (this.teamsBusy) return;
    this.tabSelected = true;
    this.activeTab = tab;
    if (typeof tab === 'number') this.selectedTeamId = tab;
  }
  updateTeams(state: TeamState) {
    this.teams = state.equipes.filter(team => !team.archivee);
    if (!this.tabSelected || (typeof this.activeTab === 'number' && !this.teams.some(team => team.id === this.activeTab))) {
      const team = this.teams.find(team => team.principale) ?? this.teams[0];
      this.activeTab = team?.id ?? 'options'; this.selectedTeamId = team?.id;
      this.tabSelected = true;
    }
  }
  teamCreated(team: Team) { this.selectTab(team.id); }
  navigateTabs(event: KeyboardEvent) {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const tabs = Array.from((event.currentTarget as HTMLElement).querySelectorAll<HTMLButtonElement>('[role="tab"]:not(:disabled)'));
    const index = tabs.indexOf(event.target as HTMLButtonElement);
    if (index < 0) return;
    event.preventDefault();
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1
      : (index + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;
    tabs[next].click(); tabs[next].focus();
    tabs[next].scrollIntoView({ block: 'nearest', inline: 'nearest' });
  }
  enregistrer() {
    if (this.saving) return;
    if (!this.sectionSelectionnee) { this.toasts.warning('Sélectionnez une section avant d’enregistrer l’activité.', 'Section obligatoire'); return; }
    this.saving = true;
    this.activities.save(this.activite, this.scopedToManagedSections).subscribe({
      next: activity => { this.saving = false; this.toasts.success('Activité enregistrée.'); this.saved.emit(activity); },
      error: e => { this.saving = false; this.toasts.error(e.error?.detail || e.error?.message || 'L’activité n’a pas pu être enregistrée.'); }
    });
  }
}
