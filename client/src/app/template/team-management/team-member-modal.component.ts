import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TeamMemberDetails, TeamPerson, TeamService } from '../../_services/team.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({ selector: 'app-team-member-modal', imports: [DatePipe], templateUrl: './team-member-modal.component.html' })
export class TeamMemberModalComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  private destroyRef = inject(DestroyRef);
  readonly modal = inject(NgbActiveModal);
  activityId!: number;
  person!: TeamPerson;
  details?: TeamMemberDetails;
  loading = true;
  error = '';

  ngOnInit() { this.load(); }
  load() {
    this.loading = true; this.error = '';
    this.api.memberDetails(this.activityId, this.person.adhesionId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: details => { this.details = details; this.loading = false; },
      error: error => {
        this.loading = false;
        this.error = error.error?.detail || error.error?.message || 'Les informations de l’adhérent n’ont pas pu être chargées.';
      }
    });
  }
}
