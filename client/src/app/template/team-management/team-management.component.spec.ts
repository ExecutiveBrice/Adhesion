import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TeamManagementComponent } from './team-management.component';
import { TeamService, TeamState } from '../../_services/team.service';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';

describe('Gestion des équipes', () => {
  let fixture: ComponentFixture<TeamManagementComponent>;
  let component: TeamManagementComponent;
  let api: jasmine.SpyObj<TeamService>;
  const state = (): TeamState => ({ equipes: [{ id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false,
    canManage: true, principale: true, encadrants: [], referents: [], effectif: [100], membres: 1 }],
    candidats: [{ id: 1, prenom: 'Alice', nom: 'Martin', adhesionId: 100, statut: 'Validée' }, { id: 2, prenom: 'Bob', nom: 'Dupont', adhesionId: 101, statut: 'Validée' }],
    intervenants: [] });
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['state', 'roster', 'save', 'archive', 'events']);
    api.state.and.returnValue(of(state()));
    api.events.and.returnValue(of([]));
    TestBed.configureTestingModule({ imports: [TeamManagementComponent], providers: [
      { provide: TeamService, useValue: api }, { provide: ActiviteService, useValue: {} },
      { provide: ParamService, useValue: { getSalles: () => of([]) } }
    ] });
    fixture = TestBed.createComponent(TeamManagementComponent); component = fixture.componentInstance;
  });
  it('propose les équipes pour une activité sans dépendre de sa section', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    expect(api.state).toHaveBeenCalledWith(7);
    expect(fixture.nativeElement.textContent).toContain('Débutants');
    expect(fixture.nativeElement.textContent).not.toContain('Catégorie de séance');
  });
  it('permet une sélection multiple sans imposer une équipe à tous les inscrits', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    component.openRoster(component.state!.equipes[0]); component.togglePerson(101, true); component.togglePerson(100, false);
    api.roster.and.returnValue(of(component.state!.equipes[0])); component.saveRoster();
    expect(api.roster).toHaveBeenCalledOnceWith(7, 10, [101], false);
  });
  it('affiche immédiatement la première équipe et empêche son archivage', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Première équipe');
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>);
    expect(buttons.find(b => b.textContent?.includes('Archiver'))?.disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('Activer');
  });
});
