import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { NgbConfig } from '@ng-bootstrap/ng-bootstrap';
import { TeamEventDetails, TeamService } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { TribuService } from '../../_services/tribu.service';
import { Adherent, Tribu, User } from '../../models';
import { EvenementComponent } from './evenement.component';

describe('Page événement', () => {
  let fixture: ComponentFixture<EvenementComponent>;
  let api: jasmine.SpyObj<TeamService>;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParams: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let connectedTribu: Subject<Tribu>;
  const detail = (): TeamEventDetails => ({ activiteId: 7, activiteNom: 'Basket', sectionNom: 'Sports',
    adresse: '1 rue des Sports', causeAnnulation: null, lien: 'example.org/basket', canChat: true,
    evenement: { id: 20, type: 'RENCONTRE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00',
      rendezVous: '3000-01-01T17:00:00', etat: 'PROGRAMMEE', lieu: 'Gymnase', salleId: 4, adversaire: 'Nantes',
      commentaire: 'Maillot blanc', equipes: ['Seniors'], canManage: true, emails: {}, responsabilites: [],
      convocations: [{ id: 30, adherentId: 1, adhesionId: 100, nom: 'Alice Martin', equipes: ['Seniors', 'Loisirs'], selectionnee: true,
        publication: null, reponse: null, dateReponse: null, presence: null, canReply: true }] } });
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['event', 'reply', 'eventMessages']);
    api.event.and.returnValue(of(detail())); api.eventMessages.and.returnValue(of([]));
    params = new BehaviorSubject(convertToParamMap({ evenementId: '20' }));
    queryParams = new BehaviorSubject(convertToParamMap({}));
    connectedTribu = new Subject<Tribu>();
    TestBed.configureTestingModule({ imports: [EvenementComponent], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: params, queryParamMap: queryParams } },
      { provide: TeamService, useValue: api }, { provide: ParamService, useValue: {} },
      { provide: TokenStorageService, useValue: { getUser: () => ({ id: 10 }) } },
      { provide: TribuService, useValue: { getConnected: () => connectedTribu } }
    ] });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(EvenementComponent); fixture.detectChanges();
    const tribu = new Tribu();
    tribu.adherents = [Object.assign(new Adherent(), { id: 1, user: Object.assign(new User(), { id: 10 }) }),
      Object.assign(new Adherent(), { id: 2, user: Object.assign(new User(), { id: 11 }) })];
    connectedTribu.next(tribu); fixture.detectChanges();
  });
  afterEach(() => fixture.destroy());
  it('présente les trois onglets et toutes les informations pratiques sans charger le chat masqué', () => {
    const tabs = fixture.nativeElement.querySelectorAll('[role="tab"]');
    expect(tabs.length).toBe(3);
    expect(tabs[0].getAttribute('aria-selected')).toBe('true');
    for (const value of ['Rencontre', 'Basket', 'Sports', 'Gymnase', 'Nantes', 'Seniors', '17:00', '19:00', 'Maillot blanc', '1 rue des Sports']) {
      expect(fixture.nativeElement.textContent).toContain(value);
    }
    expect(api.eventMessages).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('a[href="https://example.org/basket"]')).not.toBeNull();
  });
  it('ouvre la fiche aux autres adhérents sans action ni accès au chat, même par lien direct', () => {
    const data = detail(); data.canChat = false;
    data.evenement.canManage = false; data.evenement.convocations = [];
    api.event.and.returnValue(of(data));
    queryParams.next(convertToParamMap({ onglet: 'chat' }));
    params.next(convertToParamMap({ evenementId: '20' })); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[role="tab"]').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Basket');
    expect(fixture.nativeElement.querySelector('.event-invitations')).toBeNull();
    expect(fixture.componentInstance.tab()).toBe('informations');
    fixture.componentInstance.selectTab('chat'); fixture.detectChanges();
    expect(fixture.componentInstance.tab()).toBe('informations');
    expect(api.eventMessages).not.toHaveBeenCalled();
    expect(api.reply).not.toHaveBeenCalled();
  });
  for (const [index, value] of [true, false, null].entries()) {
    it(`enregistre la réponse ${value} depuis Informations et actualise les participants`, () => {
      const pending = new Subject<ReturnType<typeof detail>['evenement']>();
      api.reply.and.returnValue(pending);
      const panel = fixture.nativeElement.querySelector('.tab-pane.active');
      const buttons = panel.querySelectorAll('.event-invitations button');
      expect(buttons.length).toBe(3);
      expect(panel.querySelector('.event-invitations').compareDocumentPosition(panel.querySelector('.event-information'))
        & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
      expect(buttons[2].getAttribute('aria-pressed')).toBe('false');
      buttons[index].click(); fixture.detectChanges();
      expect(api.reply).toHaveBeenCalledOnceWith(20, 30, value);
      expect(Array.from<HTMLButtonElement>(buttons).every(button => button.disabled)).toBeTrue();
      const updated = detail().evenement;
      updated.convocations[0] = { ...updated.convocations[0], reponse: value, dateReponse: '2026-11-09T12:00:00' };
      pending.next(updated); pending.complete(); fixture.detectChanges();
      expect(buttons[index].getAttribute('aria-pressed')).toBe('true');
      expect(buttons[index].disabled).toBeFalse();
      fixture.componentInstance.tab.set('participants'); fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.tab-pane.active .invitation-state').getAttribute('aria-label'))
        .toBe(value === true ? 'Présent' : value === false ? 'Absent' : 'Ne sais pas encore');
    });
  }
  it('affiche les erreurs de réponse et permet de réessayer', () => {
    api.reply.and.returnValue(throwError(() => ({ error: { detail: 'Réponse refusée.' } })));
    fixture.nativeElement.querySelector('.event-invitations button').click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.tab-pane.active [role="alert"]').textContent).toContain('Réponse refusée.');
    expect(fixture.nativeElement.querySelector('.event-invitations button').disabled).toBeFalse();
    expect(fixture.componentInstance.detail()?.evenement.convocations[0].dateReponse).toBeNull();
  });
  it('propose uniquement les invitations autorisées, y compris celles des responsabilités', () => {
    const data = detail();
    const invitation = data.evenement.convocations[0];
    data.evenement.convocations = [{ ...invitation, canReply: false }, { ...invitation, id: 31, selectionnee: false },
      { ...invitation, id: 32, adherentId: 2, nom: 'Autre adhérent' }];
    data.evenement.responsabilites = [{ tache: { id: 3, nom: 'Matériel' }, adherent: { id: 1, prenom: 'Alice', nom: 'Martin' },
      invitation: { ...invitation, id: 40 } }];
    fixture.componentInstance.detail.set(data); fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('.event-invitation');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Matériel');
    api.reply.and.returnValue(of(data.evenement));
    rows[0].querySelector('button').click();
    expect(api.reply).toHaveBeenCalledOnceWith(20, 40, true);
    fixture.componentInstance.detail.set({ ...data, evenement: { ...data.evenement, etat: 'ANNULEE' } }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.event-invitations')).toBeNull();
  });
  it('ne présente aucune réponse si l’adhérent connecté ne peut pas être identifié', () => {
    connectedTribu.next(new Tribu()); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.event-invitations')).toBeNull();
    fixture.componentInstance.reply(detail().evenement.convocations[0], true);
    expect(api.reply).not.toHaveBeenCalled();
  });
  it('ignore une réponse reçue après avoir changé d’événement', () => {
    const pending = new Subject<ReturnType<typeof detail>['evenement']>(); api.reply.and.returnValue(pending);
    fixture.nativeElement.querySelector('.event-invitations button').click();
    const next = detail(); next.evenement.id = 21; api.event.and.returnValue(of(next));
    params.next(convertToParamMap({ evenementId: '21' }));
    pending.next(detail().evenement); pending.complete(); fixture.detectChanges();
    expect(fixture.componentInstance.detail()?.evenement.id).toBe(21);
    expect(fixture.componentInstance.replying()).toBeFalse();
  });
  it('affiche les participants en liste sans actions individuelles y compris pour un événement passé', () => {
    const data = detail(); data.evenement.debut = '2020-01-01T18:00:00'; data.evenement.fin = '2020-01-01T19:00:00';
    fixture.componentInstance.detail.set(data); fixture.componentInstance.tab.set('participants'); fixture.detectChanges();
    const panel = fixture.nativeElement.querySelector('.tab-pane.active');
    const list = panel.querySelector('.roster-list');
    expect(list.textContent).toContain('Alice Martin');
    expect(list.querySelector('.roster-teams').textContent).toBe('Seniors · Loisirs');
    expect(list.textContent).toContain('Sans réponse');
    expect(panel.textContent).toContain('Responsabilités');
    expect(list.querySelector('button, a, input, select, [tabindex]')).toBeNull();
    expect(panel.textContent).not.toContain('Présence réelle');
    expect(panel.querySelector('.participant-card')).toBeNull();
    expect(panel.querySelector('input, select')).toBeNull();
    expect(panel.querySelector('[aria-label="Publier les convocations"], [aria-label="Relancer"]')).toBeNull();
    expect(api.reply).not.toHaveBeenCalled();
  });
  for (const state of [
    { reponse: true, dateReponse: '2026-11-09T12:00:00', label: 'Présent', icon: 'circle-check' },
    { reponse: false, dateReponse: '2026-11-09T12:00:00', label: 'Absent', icon: 'circle-xmark' },
    { reponse: null, dateReponse: '2026-11-09T12:00:00', label: 'Ne sais pas encore', icon: 'circle-question' },
    { reponse: null, dateReponse: null, label: 'Sans réponse', icon: 'clock' },
    { reponse: null, dateReponse: null, label: 'Sans réponse', icon: 'clock', noInvitation: true }
  ]) {
    it(`présente les responsabilités comme les participants : ${state.label}${state.noInvitation ? ' sans invitation' : ''}`, () => {
      const data = detail();
      data.evenement.responsabilites = [{
        tache: { id: 3, nom: 'Matériel' }, adherent: { id: 1, prenom: 'Alice', nom: 'Martin' }, equipeNom: 'Bénévoles',
        equipes: ['Bénévoles', 'Loisirs'],
        invitation: state.noInvitation ? null : { ...data.evenement.convocations[0], id: 40, ...state }
      }];
      fixture.componentInstance.detail.set(data); fixture.componentInstance.tab.set('participants'); fixture.detectChanges();
      const panel = fixture.nativeElement.querySelector('.tab-pane.active');
      const row = panel.querySelector('.roster-responsibility');
      expect(row.querySelector('.roster-identity').textContent).toContain('Alice Martin');
      expect(row.querySelector('.roster-teams').textContent).toBe('Bénévoles · Loisirs');
      expect(row.querySelector('.roster-identity').textContent).not.toContain('Matériel');
      expect(row.querySelector('.roster-task').textContent).toContain('Matériel');
      expect(row.querySelector('.invitation-state').getAttribute('aria-label')).toBe(state.label);
      expect(row.querySelector('.invitation-state svg').getAttribute('data-icon')).toBe(state.icon);
      expect(row.querySelector('button, a, input, select, [tabindex]')).toBeNull();
      expect(panel.querySelector('.participant-card')).toBeNull();
      expect(panel.querySelector('.roster-person:not(.roster-responsibility) .invitation-state').getAttribute('aria-label')).toBe('Sans réponse');
      expect(api.reply).not.toHaveBeenCalled();
    });
  }
  it('annule le chargement précédent lors d’un changement d’événement et gère les accès refusés', () => {
    const pending = new Subject<TeamEventDetails>(); api.event.and.returnValue(pending);
    params.next(convertToParamMap({ evenementId: '21' }));
    api.event.and.returnValue(throwError(() => ({ status: 403 })));
    params.next(convertToParamMap({ evenementId: '22' }));
    pending.next(detail()); fixture.detectChanges();
    expect(fixture.componentInstance.detail()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Vous n’avez pas accès');
    params.next(convertToParamMap({ evenementId: 'invalide' })); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Événement introuvable');
    expect(api.event).toHaveBeenCalledTimes(3);
  });
});
