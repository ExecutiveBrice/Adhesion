import { provideRouter } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { of, Subject } from 'rxjs';
import { Team, TeamEvent, TeamService } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { EquipesComponent } from './equipes.component';
import { TeamEventsComponent } from '../../template/team-events/team-events.component';

describe('Page équipes', () => {
  let fixture: ComponentFixture<EquipesComponent>;
  let api: jasmine.SpyObj<TeamService>;
  const teams: Team[] = [
    { id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false,
      canManage: false, encadrants: [], referents: [], effectif: [], membres: 2 },
    { id: 11, activiteId: 7, activiteNom: 'Danse', nom: 'Confirmés', description: '', archivee: true,
      canManage: false, encadrants: [], referents: [], effectif: [], membres: 1 }
  ];
  const event = (id: number, lieu: string): TeamEvent => ({ id, type: 'ENTRAINEMENT',
    debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00', rendezVous: '3000-01-01T17:45:00',
    etat: 'PROGRAMMEE', lieu, salleId: null, adversaire: '', commentaire: '', equipes: [],
    canManage: false, convocations: [], emails: {} });
  const render = () => { fixture.detectChanges(); fixture.debugElement.injector.get(ChangeDetectorRef).detectChanges(); };
  const buttons = (): NodeListOf<HTMLButtonElement> => fixture.nativeElement.querySelectorAll('.team-button');

  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['mine', 'events']);
    api.mine.and.returnValue(of(teams));
    api.events.and.returnValue(of([event(20, 'Studio A')]));
    TestBed.configureTestingModule({ imports: [EquipesComponent], providers: [provideRouter([]),
      { provide: TeamService, useValue: api }, { provide: ParamService, useValue: {} }
    ] });
    fixture = TestBed.createComponent(EquipesComponent); render();
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); });

  it('propose les équipes en boutons et charge les cartes au clic sur une équipe', () => {
    expect(buttons().length).toBe(2);
    expect(buttons()[1].textContent).toContain('Archivée');
    expect(fixture.nativeElement.querySelector('select')).toBeNull();
    expect(api.events).not.toHaveBeenCalled();
    buttons()[0].click(); render();
    expect(api.events).toHaveBeenCalledOnceWith(10);
    expect(buttons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(buttons()[1].getAttribute('aria-pressed')).toBe('false');
    const card = fixture.nativeElement.querySelector('.events-grid .event-card') as HTMLElement;
    expect(card.textContent).toContain('Studio A');
    expect(card.querySelector('.event-summary')!.textContent).toContain('Débutants');
    expect(card.querySelector('.event-summary')!.textContent).toContain('17:45');
    const toggle = card.querySelector('.participants-toggle') as HTMLButtonElement;
    const details = card.querySelector('.event-participants [hidden]') as HTMLElement;
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(details.hidden).toBeTrue();
    toggle.click(); render();
    expect(details.hidden).toBeFalse();
  });

  it('remplace les événements au changement d’équipe et ignore une réponse tardive de l’ancienne équipe', () => {
    const first = new Subject<TeamEvent[]>();
    api.events.and.callFake(id => id === 10 ? first : of([event(21, 'Studio B')]));
    buttons()[0].click(); render();
    buttons()[1].click(); render();
    first.next([event(20, 'Studio A')]); first.complete(); render();
    expect(buttons()[0].getAttribute('aria-pressed')).toBe('false');
    expect(buttons()[1].getAttribute('aria-pressed')).toBe('true');
    const cards = fixture.nativeElement.querySelectorAll('.event-card') as NodeListOf<HTMLElement>;
    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain('Studio B');
    expect(cards[0].textContent).not.toContain('Studio A');
  });

  it('retire la création et la modification même pour une équipe gérée et conserve les convocations', () => {
    const managedTeam = { ...teams[0], canManage: true };
    const managedEvent = { ...event(20, 'Studio A'), canManage: true };
    api.mine.and.returnValue(of([managedTeam]));
    api.events.and.returnValue(of([managedEvent]));
    fixture.componentInstance.load(); render();
    buttons()[0].click(); render();
    expect(fixture.nativeElement.querySelector('[aria-label="Ajouter un événement"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Modifier l’événement"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Publier les convocations"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Relancer"]')).not.toBeNull();
    const open = spyOn(TestBed.inject(NgbModal), 'open');
    const events = fixture.debugElement.query(By.directive(TeamEventsComponent)).componentInstance as TeamEventsComponent;
    events.open(); events.open(managedEvent);
    expect(open).not.toHaveBeenCalled();
  });

  it('conserve la sélection à l’actualisation et la retire si l’équipe n’est plus disponible', () => {
    buttons()[0].click(); render();
    api.mine.and.returnValue(of(teams.map(t => ({ ...t }))));
    fixture.componentInstance.load(); render();
    expect(buttons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(api.events).toHaveBeenCalledTimes(1);
    api.mine.and.returnValue(of([teams[1]]));
    fixture.componentInstance.load(); render();
    expect(fixture.nativeElement.querySelector('app-team-events')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Choisissez une équipe');
  });

  it('indique l’absence d’équipe et l’absence d’événement pour une équipe sélectionnée', () => {
    api.events.and.returnValue(of([]));
    buttons()[0].click(); render();
    expect(fixture.nativeElement.textContent).toContain('Aucun événement sur cette période.');
    api.mine.and.returnValue(of([]));
    fixture.componentInstance.load(); render();
    expect(buttons().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Vous n’êtes référent ou encadrant d’aucune équipe');
  });
});
