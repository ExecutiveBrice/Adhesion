import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { TeamManagementComponent } from './team-management.component';
import { Team, TeamEvent, TeamService, TeamState } from '../../_services/team.service';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { ApiRenderService } from '../../_services/api-render.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TeamMemberModalComponent } from './team-member-modal.component';

describe('Gestion des équipes', () => {
  let fixture: ComponentFixture<TeamManagementComponent>;
  let component: TeamManagementComponent;
  let api: jasmine.SpyObj<TeamService>;
  let modals: jasmine.SpyObj<NgbModal>;
  const state = (): TeamState => ({ equipes: [{ id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false,
    canManage: true, principale: true, encadrants: [], referents: [], effectif: [100], membres: 1 }],
    candidats: [{ id: 1, prenom: 'Alice', nom: 'Martin', adhesionId: 100, statut: 'Validée' }, { id: 2, prenom: 'Bob', nom: 'Dupont', adhesionId: 101, statut: 'Validée' }],
    intervenants: [] });
  beforeEach(() => {
    modals = jasmine.createSpyObj('NgbModal', ['open']);
    api = jasmine.createSpyObj('TeamService', ['state', 'roster', 'save', 'archive', 'events']);
    api.state.and.returnValue(of(state()));
    api.events.and.returnValue(of([]));
    TestBed.configureTestingModule({ imports: [TeamManagementComponent], providers: [provideRouter([]),
      { provide: TeamService, useValue: api }, { provide: ActiviteService, useValue: {} }, { provide: NgbModal, useValue: modals },
      { provide: ParamService, useValue: { getSalles: () => of([]) } }
    ] });
    fixture = TestBed.createComponent(TeamManagementComponent); component = fixture.componentInstance;
  });
  it('masque les listes par défaut et permet de les afficher indépendamment', () => {
    const event: TeamEvent = { id: 20, type: 'ACTIVITE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00',
      etat: 'PROGRAMMEE', lieu: '', salleId: null, rendezVous: '', adversaire: '', commentaire: '',
      equipes: ['Débutants'], canManage: true, convocations: [], emails: {} };
    api.events.and.returnValue(of([event,
      { ...event, id: 21, debut: '2000-01-01T18:00:00', fin: '2000-01-01T19:00:00' },
      { ...event, id: 22, etat: 'ANNULEE' }]));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    expect(api.state).toHaveBeenCalledWith(7);
    expect(component.selectedTeam?.nom).toBe('Débutants');
    expect(fixture.nativeElement.textContent).not.toContain('Catégorie de séance');
    const rosterButton = fixture.nativeElement.querySelector('[aria-controls="team-roster-7"]') as HTMLButtonElement;
    const eventsButton = fixture.nativeElement.querySelector('[aria-controls="team-events-10"]') as HTMLButtonElement;
    const roster = fixture.nativeElement.querySelector('.team-roster') as HTMLElement;
    const events = fixture.nativeElement.querySelector('.team-events') as HTMLElement;
    expect(rosterButton.textContent?.trim()).toBe('Effectif 1 / 2');
    expect(eventsButton.textContent?.trim()).toBe('Événements 1 / 3');
    expect(roster.compareDocumentPosition(eventsButton) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(roster.hidden).toBeTrue(); expect(events.hidden).toBeTrue();
    expect(rosterButton.getAttribute('aria-expanded')).toBe('false');
    expect(eventsButton.getAttribute('aria-expanded')).toBe('false');
    rosterButton.click(); fixture.detectChanges();
    expect(roster.hidden).toBeFalse(); expect(events.hidden).toBeTrue();
    expect(rosterButton.getAttribute('aria-expanded')).toBe('true');
    eventsButton.click(); fixture.detectChanges();
    expect(roster.hidden).toBeFalse(); expect(events.hidden).toBeFalse();
    expect(eventsButton.getAttribute('aria-expanded')).toBe('true');
    rosterButton.click(); fixture.detectChanges();
    expect(roster.hidden).toBeTrue(); expect(events.hidden).toBeFalse();
    eventsButton.click(); fixture.detectChanges();
    expect(events.hidden).toBeTrue();
  });
  it('ajoute et retire un adhérent uniquement par sa case et enregistre immédiatement l’effectif', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const emitted = spyOn(component.stateChanged, 'emit');
    api.roster.and.callFake((_activity, _team, effectif) => of({ ...state().equipes[0], effectif, membres: effectif.length }));
    const cards = () => fixture.nativeElement.querySelectorAll('.roster-member') as NodeListOf<HTMLElement>;
    const checkbox = (index: number) => cards()[index].querySelector('input') as HTMLInputElement;
    checkbox(1).click(); fixture.detectChanges();
    expect(api.roster).toHaveBeenCalledOnceWith(7, 10, [100, 101]);
    expect(checkbox(1).checked).toBeTrue();
    expect(cards()[1].classList).not.toContain('roster-member-unselected');
    expect(component.selectedTeam!.membres).toBe(2);
    expect(fixture.nativeElement.querySelector('[aria-controls="team-roster-7"]').textContent.trim()).toBe('Effectif 2 / 2');
    expect(emitted).toHaveBeenCalledWith(component.state!);
    checkbox(0).click(); fixture.detectChanges();
    expect(api.roster.calls.mostRecent().args).toEqual([7, 10, [101]]);
    expect(checkbox(0).checked).toBeFalse();
    expect(cards()[0].classList).toContain('roster-member-unselected');
    expect(component.selectedTeam!.membres).toBe(1);
    expect(fixture.nativeElement.querySelector('[aria-controls="team-roster-7"]').textContent.trim()).toBe('Effectif 1 / 2');
    expect(cards().length).toBe(2);
    expect(api.state).toHaveBeenCalledTimes(1);
    expect(modals.open).not.toHaveBeenCalled();
  });
  it('ouvre les informations par clic sur la carte sans modifier l’appartenance', () => {
    const ref = { componentInstance: {}, result: Promise.resolve(), dismiss: jasmine.createSpy('dismiss') };
    modals.open.and.returnValue(ref as any);
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const card = fixture.nativeElement.querySelectorAll('.roster-member')[1] as HTMLElement;
    card.querySelector('button')!.click(); fixture.detectChanges();
    expect(modals.open).toHaveBeenCalledWith(TeamMemberModalComponent, jasmine.objectContaining({ ariaLabelledBy: 'team-member-title' }));
    expect(ref.componentInstance).toEqual({ activityId: 7, person: component.state!.candidats[1] });
    expect(api.roster).not.toHaveBeenCalled();
    expect((card.querySelector('input') as HTMLInputElement).checked).toBeFalse();
    fixture.destroy(); expect(ref.dismiss).toHaveBeenCalled();
  });
  it('recherche par nom et prénom sans accents et conserve l’effectif complet lors d’une modification filtrée', () => {
    const data = state(); data.candidats[1].prenom = 'Élodie';
    api.state.and.returnValue(of(data));
    api.roster.and.callFake((_activity, _team, effectif) => of({ ...data.equipes[0], effectif, membres: effectif.length }));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const search = fixture.nativeElement.querySelector('.team-roster input[type="search"]') as HTMLInputElement;
    const searchFor = (value: string) => { search.value = value; search.dispatchEvent(new Event('input')); fixture.detectChanges(); };
    searchFor('  DUPONT   elo ');
    expect(fixture.nativeElement.querySelectorAll('.roster-member').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.roster-member-name').textContent).toBe('Élodie Dupont');
    expect(fixture.nativeElement.querySelector('[aria-controls="team-roster-7"]').textContent.trim()).toBe('Effectif 1 / 2');
    fixture.nativeElement.querySelector('.roster-member-checkbox').click(); fixture.detectChanges();
    expect(api.roster).toHaveBeenCalledOnceWith(7, 10, [100, 101]);
    searchFor('inconnu');
    expect(fixture.nativeElement.querySelectorAll('.roster-member').length).toBe(0);
    expect(fixture.nativeElement.querySelector('.team-roster').textContent).toContain('Aucun adhérent ne correspond à la recherche.');
    searchFor('');
    expect(fixture.nativeElement.querySelectorAll('.roster-member').length).toBe(2);
    expect(api.state).toHaveBeenCalledTimes(1);
  });
  it('ne propose plus les actions d’équipe sous les boutons de sélection', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>);
    expect(buttons.find(b => b.textContent?.includes('Archiver'))).toBeUndefined();
    expect(buttons.find(b => b.textContent?.includes('Modifier l’équipe'))).toBeUndefined();
    expect(fixture.nativeElement.textContent).not.toContain('Activer');
  });
  it('affiche tous les adhérents dans chaque équipe et ne modifie que l’équipe sélectionnée', () => {
    const data = state();
    data.equipes.push({ ...data.equipes[0], id: 11, nom: 'Confirmés', principale: false, effectif: [101] });
    api.state.and.returnValue(of(data));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const cards = () => fixture.nativeElement.querySelectorAll('.roster-member') as NodeListOf<HTMLElement>;
    const checkbox = (index: number) => cards()[index].querySelector('input') as HTMLInputElement;
    expect(cards().length).toBe(2);
    expect(cards()[0].querySelector('.roster-member-name')!.textContent).toBe('Alice Martin');
    expect(checkbox(0).checked).toBeTrue();
    expect(checkbox(1).checked).toBeFalse();
    fixture.componentRef.setInput('selectedTeamId', 11); fixture.detectChanges();
    expect(cards().length).toBe(2);
    expect(checkbox(0).checked).toBeFalse();
    expect(checkbox(1).checked).toBeTrue();
    api.roster.and.returnValue(of({ ...data.equipes[1], effectif: [101, 100], membres: 2 }));
    checkbox(0).click(); fixture.detectChanges();
    expect(api.roster).toHaveBeenCalledOnceWith(7, 11, [101, 100]);
    expect(component.state!.equipes[0].effectif).toEqual([100]);
    fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    expect(checkbox(1).checked).toBeFalse();
  });
  it('grise toutes les cartes d’une équipe vide et les masque pendant la création', () => {
    const empty = state(); empty.equipes[0].effectif = [];
    api.state.and.returnValue(of(empty));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.roster-member-unselected').length).toBe(2);
    fixture.componentRef.setInput('creating', true); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.team-roster')).toBeNull();
  });
  it('affiche sous le nom les pictogrammes de paiement, inscription et signalement selon l’adhésion', () => {
    const data = state();
    data.candidats[0] = { ...data.candidats[0], paiementValide: true, inscriptionValidee: true, flag: false };
    data.candidats[1] = { ...data.candidats[1], paiementValide: false, inscriptionValidee: false, flag: true };
    data.equipes[0].effectif = [100, 101];
    api.state.and.returnValue(of(data));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('.roster-member') as NodeListOf<HTMLElement>;
    const labels = (card: HTMLElement) => Array.from(card.querySelectorAll('.membership-status > [role="img"]'))
      .map(icon => icon.getAttribute('aria-label'));
    expect(cards[0].querySelector('.roster-member-name')!.nextElementSibling!.classList).toContain('membership-status');
    expect(labels(cards[0])).toEqual(['Paiement validé', 'Inscription validée', 'Aucun signalement']);
    expect(labels(cards[1])).toEqual(['Paiement en attente', 'Inscription non validée', 'Adhésion signalée']);
    expect(cards[0].querySelectorAll('.status-valid').length).toBe(2);
    expect(cards[1].querySelectorAll('.status-pending').length).toBe(3);
    expect(cards[0].querySelector('[data-icon="circle-check"]')).not.toBeNull();
    expect(cards[1].querySelector('[data-icon="circle-xmark"]')).not.toBeNull();
    cards.forEach(card => expect(card.querySelectorAll('.membership-status fa-icon').length).toBe(3));
  });
  it('bloque les clics pendant l’enregistrement et conserve l’effectif en cas d’échec', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const pending = new Subject<Team>(); api.roster.and.returnValue(pending);
    const busy = spyOn(component.busyChanged, 'emit');
    const checkboxes = fixture.nativeElement.querySelectorAll('.roster-member-checkbox') as NodeListOf<HTMLInputElement>;
    checkboxes[1].click(); fixture.detectChanges();
    expect(checkboxes[0].disabled).toBeTrue(); expect(checkboxes[1].disabled).toBeTrue();
    expect(checkboxes[1].checked).toBeFalse();
    checkboxes[0].click(); component.toggleRosterMember(100);
    expect(api.roster).toHaveBeenCalledTimes(1);
    pending.error({ error: { detail: 'Enregistrement impossible' } }); TestBed.inject(ApiRenderService).notify(); fixture.detectChanges();
    expect(checkboxes[1].disabled).toBeFalse();
    expect(checkboxes[1].checked).toBeFalse();
    expect(component.selectedTeam!.effectif).toEqual([100]);
    expect(fixture.nativeElement.textContent).toContain('Enregistrement impossible');
    expect(busy.calls.allArgs()).toEqual([[true], [false]]);
    api.roster.and.returnValue(of({ ...state().equipes[0], effectif: [100, 101], membres: 2 }));
    checkboxes[1].click(); fixture.detectChanges();
    expect(checkboxes[1].checked).toBeTrue();
  });
  it('enregistre l’effectif sans option d’actualisation des événements futurs', () => {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    api.roster.and.returnValue(of({ ...state().equipes[0], effectif: [], membres: 0 }));
    expect(fixture.nativeElement.querySelector('.team-roster > label input')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Actualiser aussi');
    fixture.nativeElement.querySelector('.roster-member-checkbox').click(); fixture.detectChanges();
    expect(api.roster).toHaveBeenCalledOnceWith(7, 10, []);
  });
  it('désactive les cases sans droit de gestion et indique une activité sans adhérent', () => {
    const data = state(); data.equipes[0].canManage = false; api.state.and.returnValue(of(data));
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    const checkbox = fixture.nativeElement.querySelector('.roster-member-checkbox') as HTMLInputElement;
    expect(checkbox.disabled).toBeTrue(); checkbox.click(); component.toggleRosterMember(100);
    expect(fixture.nativeElement.querySelector('.roster-member-details').disabled).toBeFalse();
    expect(api.roster).not.toHaveBeenCalled();
    component.state = { ...data, candidats: [] }; TestBed.inject(ApiRenderService).notify(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.team-roster').textContent).toContain('Aucun adhérent dans cette activité.');
  });
});
