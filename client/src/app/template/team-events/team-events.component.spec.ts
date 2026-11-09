import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangeDetectorRef } from '@angular/core';
import { defer, of, Subject, throwError } from 'rxjs';
import { TeamEventsComponent } from './team-events.component';
import { TeamService, Team, TeamEvent } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';

describe('Convocations et présences d’équipe', () => {
  let fixture: ComponentFixture<TeamEventsComponent>;
  let component: TeamEventsComponent;
  let api: jasmine.SpyObj<TeamService>;
  function render() { fixture.detectChanges(); fixture.debugElement.injector.get(ChangeDetectorRef).detectChanges(); }
  const team: Team = { id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false, canManage: false, encadrants: [], referents: [], effectif: [], membres: 2 };
  const event = (): TeamEvent => ({ id: 20, type: 'SEANCE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00', etat: 'PROGRAMMEE', lieu: 'Studio', salleId: null, rendezVous: '', adversaire: '', commentaire: '', equipes: ['Débutants'], canManage: false, emails: {},
    convocations: [{ id: 30, adherentId: 1, adhesionId: 100, nom: 'Alice Martin', selectionnee: true, publication: '2026-01-01T12:00:00', reponse: null, dateReponse: null, presence: null, canReply: true }] });
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['events', 'reply', 'attendance', 'publishInvitation', 'remindInvitation', 'remind', 'responsibilityTeams', 'responsibilities']); api.events.and.returnValue(of([event()]));
    api.responsibilityTeams.and.returnValue(of([
      { id: 10, nom: 'Débutants', activiteNom: 'Danse', attributaire: { id: 1, prenom: 'Alice', nom: 'Martin' } },
      { id: 11, nom: 'Confirmés', activiteNom: 'Autre activité de la section', attributaire: { id: 2, prenom: 'Bob', nom: 'Martin' } },
      { id: 12, nom: 'Vide', activiteNom: 'Danse', attributaire: null }
    ]));
    TestBed.configureTestingModule({ imports: [TeamEventsComponent], providers: [provideRouter([]),{ provide: TeamService, useValue: api }, { provide: ParamService, useValue: { getSalles: () => of([]), getTachesSeance: () => of([{ id: 3, nom: 'Matériel' }]) } }] });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(TeamEventsComponent); component = fixture.componentInstance;
    fixture.componentRef.setInput('team', team); render();
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); });
  it('filtre les participants par nom et prénom indépendamment pour chaque événement et conserve les compteurs', () => {
    const first = event();
    first.convocations.push({ ...first.convocations[0], id: 31, adherentId: 2, adhesionId: 101, nom: 'Élodie Dupont', reponse: true });
    first.convocations.push({ ...first.convocations[0], id: 32, nom: 'Élodie Martin', selectionnee: false });
    component.events = [first, { ...event(), id: 21 }];
    fixture.componentRef.setInput('collapsibleParticipants', true); render();
    const cards = fixture.nativeElement.querySelectorAll('.event-card') as NodeListOf<HTMLElement>;
    const search = cards[0].querySelector('input[type="search"]') as HTMLInputElement;
    const searchFor = (value: string) => { search.value = value; search.dispatchEvent(new Event('input')); render(); };
    searchFor('  DUPONT   elo ');
    expect(cards[0].querySelectorAll('.participant-card').length).toBe(1);
    expect(cards[0].querySelector('.participant-name')!.textContent).toBe('Élodie Dupont');
    expect(cards[1].querySelector('.participant-name')!.textContent).toBe('Alice Martin');
    expect(cards[0].querySelector('.participants-toggle')!.textContent).toContain('Participants (2)');
    expect(cards[0].querySelector('.response-present')!.getAttribute('aria-label')).toBe('1 réponse(s) Présent');
    searchFor('inconnu');
    expect(cards[0].querySelectorAll('.participant-card').length).toBe(0);
    expect(cards[0].textContent).toContain('Aucun participant ne correspond à la recherche.');
    searchFor('');
    expect(cards[0].querySelectorAll('.participant-card').length).toBe(2);
    expect(api.events).toHaveBeenCalledTimes(1);
    expect(api.reply).not.toHaveBeenCalled(); expect(api.attendance).not.toHaveBeenCalled();
  });
  it('recharge les événements après une modification de l’effectif de la même équipe', () => {
    const updated = event(); updated.convocations = [];
    api.events.and.returnValue(of([updated]));
    fixture.componentRef.setInput('team', { ...team, effectif: [101] }); render();
    expect(api.events).toHaveBeenCalledTimes(2);
    expect(component.events[0].convocations).toEqual([]);
    fixture.componentRef.setInput('team', { ...team, effectif: [101], nom: 'Autre nom' }); render();
    expect(api.events).toHaveBeenCalledTimes(2);
  });
  it('affiche la réponse familiale sans proposer de commande de gestion au participant', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Alice Martin'); expect(text).toContain('Présent'); expect(text).toContain('Absent');
    expect(text).toContain('Ne sais pas encore'); expect(text).toContain('Sans réponse');
    expect(text).not.toContain('Publier les convocations'); expect(text).not.toContain('Présence réelle');
    const link = fixture.nativeElement.querySelector('.event-detail-link') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/evenements/20');
    expect(link.getAttribute('aria-label')).toBe('Afficher la page de l’événement');
    expect(link.querySelector('svg')?.getAttribute('data-icon')).toBe('arrow-up-right-from-square');
  });
  it('ne présélectionne ni réponse ni présence et propose trois réponses et deux pointages', () => {
    component.events[0].canManage = true; render();
    const groups = fixture.nativeElement.querySelectorAll('.participant-card [role="group"]');
    expect(groups[0].querySelectorAll('button').length).toBe(3);
    expect(groups[1].querySelectorAll('button').length).toBe(2);
    expect(fixture.nativeElement.querySelectorAll('.participant-card [aria-pressed="true"]').length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Non pointée');
    expect(api.reply).not.toHaveBeenCalled(); expect(api.attendance).not.toHaveBeenCalled();
  });
  it('enregistre l’indécision sans pointer la présence et restaure le choix enregistré', () => {
    const saved = event(); saved.canManage = true; saved.convocations[0].dateReponse = '2026-11-09T12:00:00';
    component.events[0].canManage = true; render();
    api.reply.and.returnValue(of(saved)); api.events.and.returnValue(of([saved]));
    const buttons = fixture.nativeElement.querySelectorAll('.participant-card [role="group"]')[0].querySelectorAll('button');
    buttons[2].click(); render();
    expect(api.reply).toHaveBeenCalledWith(20, 30, null); expect(api.attendance).not.toHaveBeenCalled();
    const groups = fixture.nativeElement.querySelectorAll('.participant-card [role="group"]');
    expect(groups[0].querySelector('[aria-pressed="true"]').textContent).toBe('Ne sais pas encore');
    expect(groups[0].parentElement.textContent).not.toContain('Sans réponse');
    expect(groups[1].querySelector('[aria-pressed="true"]')).toBeNull();
  });
  it('restaure une réponse et une présence différentes dans leurs groupes respectifs', () => {
    component.events[0].canManage = true;
    component.events[0].convocations[0].reponse = true;
    component.events[0].convocations[0].presence = false; render();
    const groups = fixture.nativeElement.querySelectorAll('.participant-card [role="group"]');
    expect(groups[0].querySelector('[aria-pressed="true"]').textContent).toBe('Présent');
    expect(groups[1].querySelector('[aria-pressed="true"]').textContent).toBe('Absent');
  });
  it('sépare la réponse du pointage et bloque les doubles soumissions', () => {
    const response = new Subject<TeamEvent>(); let subscriptions = 0;
    api.reply.and.returnValue(defer(() => { subscriptions++; return response; }));
    component.reply(component.events[0], 30, true); component.reply(component.events[0], 30, true);
    expect(subscriptions).toBe(1);
    expect(api.attendance).not.toHaveBeenCalled(); expect(component.busy).toBeTrue();
    response.next(event()); response.complete(); expect(component.busy).toBeFalse();
  });
  it('convoque uniquement le participant de la carte puis active sa relance', () => {
    const draft = event(); draft.canManage = true; draft.convocations[0].publication = null;
    component.events = [draft]; render();
    const card = fixture.nativeElement.querySelector('.participant-card');
    const publish = card.querySelector('[aria-label="Convoquer Alice Martin"]') as HTMLButtonElement;
    const remind = card.querySelector('[aria-label="Relancer Alice Martin"]') as HTMLButtonElement;
    expect(publish.disabled).toBeFalse(); expect(remind.disabled).toBeTrue();
    const saved = event(); saved.canManage = true;
    api.publishInvitation.and.returnValue(of(saved)); api.events.and.returnValue(of([saved]));
    publish.click(); render();
    expect(api.publishInvitation).toHaveBeenCalledWith(20, 30);
    expect(publish.disabled).toBeTrue(); expect(remind.disabled).toBeFalse();
    api.remindInvitation.and.returnValue(of(saved)); remind.click(); render();
    expect(api.remindInvitation).toHaveBeenCalledWith(20, 30, jasmine.any(String));
    expect(api.remind).not.toHaveBeenCalled();
    saved.convocations[0].dateReponse = '2026-11-09T12:00:00'; render();
    expect(remind.disabled).toBeTrue();
  });
  it('désactive les envois individuels pour un événement passé ou annulé', () => {
    component.events[0].canManage = true; component.events[0].etat = 'ANNULEE'; render();
    const buttons = fixture.nativeElement.querySelectorAll('.participant-invitation button');
    expect(buttons.length).toBe(2);
    expect(Array.from(buttons as NodeListOf<HTMLButtonElement>).every(b => b.disabled)).toBeTrue();
    component.events[0].etat = 'PROGRAMMEE'; component.events[0].debut = '2000-01-01T18:00:00'; render();
    expect(Array.from(buttons as NodeListOf<HTMLButtonElement>).every(b => b.disabled)).toBeTrue();
  });
  it('conserve une clé de relance par participant après une erreur', () => {
    api.remindInvitation.and.returnValue(throwError(() => ({ error: { detail: 'Réponse réseau incertaine' } })));
    component.remindInvitation(component.events[0], 30);
    const key = api.remindInvitation.calls.mostRecent().args[2];
    component.remindInvitation(component.events[0], 31);
    expect(api.remindInvitation.calls.mostRecent().args[2]).not.toBe(key);
    component.remindInvitation(component.events[0], 30);
    expect(api.remindInvitation.calls.mostRecent().args[2]).toBe(key);
  });
  it('réutilise la clé d’une relance après une erreur pour éviter un second envoi', () => {
    const response = new Subject<TeamEvent>(); api.remind.and.returnValue(response);
    component.remind(component.events[0]); const key = api.remind.calls.mostRecent().args[1];
    response.error({ error: { detail: 'Réponse réseau incertaine' } });
    api.remind.and.returnValue(of(event())); component.remind(component.events[0]);
    expect(api.remind.calls.mostRecent().args[1]).toBe(key);
  });
  it('convoque, relance et enregistre une réponse propre à chaque responsabilité', () => {
    const e = event(); e.canManage = true;
    e.responsabilites = [{ tache: { id: 3, nom: 'Matériel' }, adherent: { id: 1, prenom: 'Alice', nom: 'Martin' }, equipeId: 10,
      invitation: { ...e.convocations[0], id: 40, adhesionId: null, publication: null } }];
    component.events = [e]; render();
    const card = () => fixture.nativeElement.querySelector('.responsibility-card') as HTMLElement;
    expect(card().textContent).toContain('Sans réponse');
    expect(card().querySelector('[aria-pressed="true"]')).toBeNull();
    const saved = { ...e, responsabilites: [{ ...e.responsabilites[0], invitation: { ...e.responsabilites[0].invitation!, publication: '2026-11-09T12:00:00' } }] };
    api.publishInvitation.and.returnValue(of(saved)); api.events.and.returnValue(of([saved]));
    (card().querySelector('[aria-label="Convoquer Alice Martin pour Matériel"]') as HTMLButtonElement).click(); render();
    expect(api.publishInvitation).toHaveBeenCalledWith(20, 40);
    expect((card().querySelector('[aria-label="Relancer Alice Martin pour Matériel"]') as HTMLButtonElement).disabled).toBeFalse();
    api.remindInvitation.and.returnValue(of(saved));
    (card().querySelector('[aria-label="Relancer Alice Martin pour Matériel"]') as HTMLButtonElement).click(); render();
    expect(api.remindInvitation).toHaveBeenCalledWith(20, 40, jasmine.any(String));
    const answered = { ...saved, responsabilites: [{ ...saved.responsabilites[0], invitation: { ...saved.responsabilites[0].invitation, dateReponse: '2026-11-09T13:00:00' } }] };
    api.reply.and.returnValue(of(answered)); api.events.and.returnValue(of([answered]));
    (card().querySelectorAll('[role="group"] button')[2] as HTMLButtonElement).click(); render();
    expect(api.reply).toHaveBeenCalledWith(20, 40, null);
    expect(card().querySelector('[aria-pressed="true"]')?.textContent).toBe('Ne sais pas encore');
    expect((card().querySelector('[aria-label="Relancer Alice Martin pour Matériel"]') as HTMLButtonElement).disabled).toBeTrue();
    expect(component.events[0].convocations[0].dateReponse).toBeNull();
    expect(card().textContent).not.toContain('Présence réelle'); expect(api.attendance).not.toHaveBeenCalled();
  });
  it('réserve les commandes de réponse aux personnes autorisées sur la responsabilité', () => {
    const e = event();
    e.responsabilites = [{ tache: { id: 3, nom: 'Matériel' }, adherent: { id: 2, prenom: 'Bob', nom: 'Martin' },
      invitation: { ...e.convocations[0], id: 40, canReply: false, reponse: false } }];
    component.events = [e]; render();
    const card = fixture.nativeElement.querySelector('.responsibility-card');
    expect(card.textContent).toContain('Absent'); expect(card.querySelector('button')).toBeNull();
    expect(api.reply).not.toHaveBeenCalled();
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
    component.editResponsibilities(component.events[0]); render();
    const dialog = document.querySelector('[role="dialog"]')!;
    expect(dialog).not.toBeNull();
    expect(dialog.querySelectorAll('select').length).toBe(2);
    expect(dialog.textContent).toContain('Autre activité de la section · Confirmés');
    const select = dialog.querySelector('[aria-label="Équipe de la section"]') as HTMLSelectElement;
    select.selectedIndex = 2; select.dispatchEvent(new Event('change')); render();
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
