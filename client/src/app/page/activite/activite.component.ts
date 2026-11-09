import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { ManagedSection, SectionManagementService } from '../../_services/section-management.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Activite } from '../../models';
import { ActiviteEditorComponent } from './activite-editor.component';

@Component({
  selector: 'app-activite',
  imports: [RouterLink, ActiviteEditorComponent],
  template: `
    <main class="container-fluid py-4 px-3 px-md-4">
      @if (!activite()) {
        <header class="d-flex align-items-center gap-3 mb-4">
          <h1 class="h3 mb-0 me-auto">Activité</h1>
          <a routerLink="/activites" class="btn btn-outline-secondary flex-shrink-0 text-nowrap" aria-label="Retour aux activités" title="Retour aux activités"><span aria-hidden="true">←</span><span class="d-none d-sm-inline"> Retour aux activités</span></a>
        </header>
      }
      @if (loading()) { <p role="status">Chargement de l’activité…</p> }
      @if (error()) { <div class="alert alert-danger" role="alert">{{error()}}</div> }
      @if (activite(); as activity) {
        <app-activite-editor [activite]="activity" [scopedToManagedSections]="scopedToManagedSections"
          [managedSections]="managedSections" (saved)="onSaved($event)" />
      }
    </main>`
})
export class ActiviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ActiviteService);
  private readonly sections = inject(SectionManagementService);
  private readonly tokens = inject(TokenStorageService);
  private readonly destroyRef = inject(DestroyRef);
  readonly activite = signal<Activite | undefined>(undefined);
  managedSections: ManagedSection[] = [];
  scopedToManagedSections = false;
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit() {
    const roles: string[] = this.tokens.getUser().roles ?? [];
    this.scopedToManagedSections = !roles.includes('ROLE_ADMIN') && !roles.includes('ROLE_SECRETAIRE');
    this.route.paramMap.pipe(
      switchMap(params => {
        this.activite.set(undefined); this.error.set(''); this.loading.set(true);
        const id = params.get('activiteId');
        return forkJoin({
          activity: id ? this.api.getById(Number(id), this.scopedToManagedSections) : of(new Activite()),
          sections: this.scopedToManagedSections ? this.sections.sections() : of([] as ManagedSection[])
        }).pipe(catchError(e => {
          this.error.set(e.error?.detail || e.error?.message || 'L’activité n’a pas pu être chargée.');
          return of(undefined);
        }));
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => {
      this.loading.set(false);
      if (result) { this.activite.set(result.activity); this.managedSections = result.sections; }
    });
  }

  onSaved(activity: Activite) {
    if (!this.activite()?.id) this.router.navigate(['/activites', activity.id], { replaceUrl: true });
  }
}
