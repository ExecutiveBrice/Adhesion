import { ComponentFixture, TestBed } from '@angular/core/testing';
import { defer, of, Subject, throwError } from 'rxjs';
import { TeamEventsComponent } from './team-events.component';
import { TeamService, Team, TeamEvent } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';

describe('Convocations et présences d’équipe', () => {
  let fixture: ComponentFixture<TeamEventsComponent>;
  let component: TeamEventsComponent;
  let api: jasmine.SpyObj<TeamService>;
  const team: Team = { id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false, canManage: false, encadrants: [], referents: [], effectif: [], membres: 2 };
  const event = (): TeamEvent => ({ id: 20, type: 'SEANCE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00', etat: 'PROGRAMMEE', lieu: 'Studio', salleId: null, rendezVous: '', adversaire: '', commentaire: '', equipes: ['Débutants'], canManage: false, emails: {},
    convocations: [{ id: 30, adherentId: 1, adhesionId: 100, nom: 'Alice Martin', selectionnee: true, publication: '2026-01-01T12:00:00', reponse: null, dateReponse: null, presence: null, canReply: true }] });
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['events', 'reply', 'attendance', 'remind', 'responsibilityTeams', 'responsibilities']); api.events.and.returnValue(of([event()]));
    api.responsibilityTeams.and.returnValue(of([
      { id: 10, nom: 'Débutants', activiteNom: 'Danse', attributaire: { id: 1, prenom: 'Alice', nom: 'Martin' } },
      { id: 11, nom: 'Confirmés', activiteNom: 'Autre activité de la section', attributaire: { id: 2, prenom: 'Bob', nom: 'Martin' } },
      { id: 12, nom: 'Vide', activiteNom: 'Danse', attributaire: null }
    ]));
    TestBed.configureTestingModule({ imports: [TeamEventsComponent], providers: [{ provide: TeamService, useValue: api }, { provide: ParamService, useValue: { getSalles: () => of([]), getTachesSeance: () => of([{ id: 3, nom: 'Matériel' }]) } }] });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(TeamEventsComponent); component = fixture.componentInstance;
    fixture.componentRef.setInput('team', team); fixture.detectChanges();
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); });
  it('affiche la réponse familiale sans proposer de commande de gestion au participant', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Alice Martin'); expect(text).toContain('Accepter'); expect(text).toContain('Refuser');
    expect(text).not.toContain('Publier les convocations'); expect(text).not.toContain('Présence réelle');
  });
  it('sépare la réponse du pointage et bloque les doubles soumissions', () => {
    const response = new Subject<TeamEvent>(); let subscriptions = 0;
    api.reply.and.returnValue(defer(() => { subscriptions++; return response; }));
    component.reply(component.events[0], 30, true); component.reply(component.events[0], 30, true);
    expect(subscriptions).toBe(1);
    expect(api.attendance).not.toHaveBeenCalled(); expect(component.busy).toBeTrue();
    response.next(event()); response.complete(); expect(component.busy).toBeFalse();
  });
  it('réutilise la clé d’une relance après une erreur pour éviter un second envoi', () => {
    const response = new Subject<TeamEvent>(); api.remind.and.returnValue(response);
    component.remind(component.events[0]); const key = api.remind.calls.mostRecent().args[1];
    response.error({ error: { detail: 'Réponse réseau incertaine' } });
    api.remind.and.returnValue(of(event())); component.remind(component.events[0]);
    expect(api.remind.calls.mostRecent().args[1]).toBe(key);
  });
  it('gère les responsabilités dans l’événement et conserve les choix en cas de refus', () => {
    component.editResponsibilities(component.events[0]);
    expect(component.tasks[0].nom).toBe('Matériel');
    component.responsibilityRows = [{ tacheId: 3 }]; component.saveResponsibilities();
    expect(api.responsibilities).not.toHaveBeenCalled();
    component.responsibilityRows = [{ tacheId: 3, equipeId: 11 }];
    api.responsibilities.and.returnValue(throwError(() => ({ error: { detail: 'Équipe hors section' } })));
    component.saveResponsibilities();
    expect(component.error).toBe('Équipe hors section');
    expect(component.responsibilityRows).toEqual([{ tacheId: 3, equipeId: 11 }]);
    expect(component.responsibilityEvent?.id).toBe(20);
    api.responsibilities.and.returnValue(of(event())); component.saveResponsibilities();
    expect(api.responsibilities).toHaveBeenCalledWith(20, [{ tacheId: 3, equipeId: 11 }]);
    expect(component.responsibilityEvent).toBeUndefined();
  });
  it('ouvre une modale avec deux sélecteurs et les équipes des autres activités de la section', () => {
    component.editResponsibilities(component.events[0]); fixture.detectChanges();
    const dialog = document.querySelector('[role="dialog"]')!;
    expect(dialog).not.toBeNull();
    expect(dialog.querySelectorAll('select').length).toBe(2);
    expect(dialog.textContent).toContain('Autre activité de la section · Confirmés');
    const select = dialog.querySelector('[aria-label="Équipe de la section"]') as HTMLSelectElement;
    select.selectedIndex = 2; select.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(component.responsibilityRows[0].equipeId).toBe(11);
    expect(dialog.textContent).toContain('Attribuée à Bob Martin');
    expect(api.responsibilityTeams).toHaveBeenCalledWith(20);
  });
  it('restaure l’équipe enregistrée et refuse les doublons et les équipes vides', () => {
    const e = event(); e.responsabilites = [{ tache: { id: 3, nom: 'Matériel' }, adherent: { id: 1, prenom: 'Alice', nom: 'Martin' }, equipeId: 10 }];
    component.editResponsibilities(e);
    expect(component.responsibilityRows).toEqual([{ tacheId: 3, equipeId: 10 }]);
    component.responsibilityRows.push({ tacheId: 3, equipeId: 10 }); component.saveResponsibilities();
    expect(component.error).toContain('déjà attribuée');
    component.responsibilityRows = [{ tacheId: 3, equipeId: 12 }]; component.saveResponsibilities();
    expect(component.error).toContain('aucun référent ni adhérent');
    expect(api.responsibilities).not.toHaveBeenCalled();
  });
  it('ignore le chargement d’une modale fermée pour un autre événement', () => {
    const pending = new Subject<any>(); api.responsibilityTeams.and.returnValue(pending);
    component.editResponsibilities(component.events[0]); component.closeResponsibilities();
    pending.next([{ id: 99, nom: 'Périmée', activiteNom: 'Danse', attributaire: null }]); pending.complete();
    expect(component.responsibilityEvent).toBeUndefined(); expect(component.responsibilityTeams).toEqual([]);
  });
});
