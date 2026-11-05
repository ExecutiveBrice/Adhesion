import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from '../../_services/adherent.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { Activite } from '../../models';
import { ActiviteEditorComponent } from './activite-editor.component';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TokenStorageService } from '../../_services/token-storage.service';
import { apiChangeDetectionInterceptor } from '../../_helpers/auth.interceptor';
import { Team, TeamState } from '../../_services/team.service';
import { environment } from '../../../environments/environment';

describe('Fiche activité : section obligatoire', () => {
  let fixture: ComponentFixture<ActiviteEditorComponent>;
  let component: ActiviteEditorComponent;
  let activites: jasmine.SpyObj<ActiviteService>;
  let saved: jasmine.Spy;
  let toasts: jasmine.SpyObj<ToastService>;
  const section = { id: 7, nom: 'Yoga', type: 'NON_COMPETITIVE' as const };

  beforeEach(() => {
    activites = jasmine.createSpyObj('ActiviteService', ['save']);
    saved = jasmine.createSpy('saved');
    toasts = jasmine.createSpyObj('ToastService', ['warning', 'success', 'error']);
    TestBed.configureTestingModule({
      imports: [ActiviteEditorComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: ActiviteService, useValue: activites },
        { provide: ToastService, useValue: toasts },
        { provide: AdherentService, useValue: { getByRole: () => of([]) } },
        { provide: ParamService, useValue: { getSections: () => of([section]), getSalles: () => of([]), getTachesSeance: () => of([]) } }
      ]
    });
    fixture = TestBed.createComponent(ActiviteEditorComponent);
    component = fixture.componentInstance;
    component.activite = new Activite();
    component.saved.subscribe(saved);
    activites.save.and.callFake(activite => of(activite));
  });

  it('bloque le bouton tant qu’aucune section n’est choisie puis enregistre la section sélectionnée', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const bouton = fixture.nativeElement.querySelector('.activity-footer .btn-success') as HTMLButtonElement;
    const select = fixture.nativeElement.querySelector('#sectionActivite') as HTMLSelectElement;
    expect(bouton.disabled).toBeTrue();
    expect(select.required).toBeTrue();
    expect(select.options[0].disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Choisissez une section pour enregistrer l’activité.');
    bouton.click();
    expect(activites.save).not.toHaveBeenCalled();

    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(bouton.disabled).toBeFalse();
    bouton.click();
    expect(activites.save).toHaveBeenCalledOnceWith(component.activite, false);
    expect(component.activite.section).toEqual(section);
    expect(saved).toHaveBeenCalledWith(component.activite);
  });

  it('refuse aussi un appel direct à l’enregistrement pour une création ou une modification sans section', () => {
    for (const id of [undefined, 12]) {
      component.activite.id = id!;
      component.enregistrer();
    }
    expect(activites.save).not.toHaveBeenCalled();
    expect(saved).not.toHaveBeenCalled();
    expect(toasts.warning).toHaveBeenCalledWith('Sélectionnez une section avant d’enregistrer l’activité.', 'Section obligatoire');
  });

  it('limite une création de référent à sa section et utilise l’enregistrement dédié', () => {
    component.scopedToManagedSections = true;
    component.managedSections = [{ id: 7, nom: 'Yoga', couleur: '#000000' }];
    fixture.detectChanges();

    expect(component.activite.section?.id).toBe(7);
    expect(component.sections.map(item => item.id)).toEqual([7]);
    expect(fixture.nativeElement.querySelector('.seances-liste')).toBeNull();
    component.enregistrer();
    expect(activites.save).toHaveBeenCalledOnceWith(component.activite, true);
    expect(saved).toHaveBeenCalledWith(component.activite);

    component.activite.section = { id: 8, nom: 'Autre', type: 'NON_COMPETITIVE' };
    component.enregistrer();
    expect(activites.save).toHaveBeenCalledTimes(1);
  });

  it('ne propose plus de catégorie, créneau ou activation des équipes', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).not.toContain('catégorie');
    expect(text).not.toContain('créneau');
    expect(text).not.toContain('Activer');
  });
});

describe('Onglets de la fiche activité', () => {
  let fixture: ComponentFixture<ActiviteEditorComponent>;
  let http: HttpTestingController;
  const base = environment.server;
  const team = (id: number, nom: string): Team => ({ id, nom, activiteId: 7, activiteNom: 'Danse', description: '',
    archivee: false, canManage: true, principale: id === 10, encadrants: [], referents: [], effectif: [], membres: 0 });
  const state = (equipes = [team(10, 'Débutants'), team(11, 'Confirmés')]): TeamState => ({ equipes, candidats: [], intervenants: [] });

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ActiviteEditorComponent], providers: [
      provideHttpClient(withInterceptors([apiChangeDetectionInterceptor])), provideHttpClientTesting(),
      { provide: TokenStorageService, useValue: { getUser: () => ({ roles: ['ROLE_ADMIN'] }) } },
      { provide: ToastService, useValue: jasmine.createSpyObj('toast', ['warning', 'success', 'error']) },
      { provide: ParamService, useValue: { getSections: () => of([]), getSalles: () => of([]) } }
    ] });
    TestBed.inject(NgbConfig).animation = false;
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ActiviteEditorComponent);
    fixture.componentInstance.activite = Object.assign(new Activite(), { id: 7, nom: 'Danse', section: { id: 3, nom: 'Culture' } });
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); http.verify(); });
  const tabs = () => Array.from(fixture.nativeElement.querySelectorAll('[role="tab"]') as NodeListOf<HTMLButtonElement>);
  function clickTab(text: string) {
    tabs().find(tab => tab.textContent?.trim().replace(/^＋\s*/, '') === text)!.click();
    tick(); fixture.detectChanges();
  }
  function open(equipes = state().equipes) {
    fixture.detectChanges(); http.expectOne(`${base}/teams/activities/7`).flush(state(equipes)); tick(); fixture.detectChanges();
    const primary = equipes.find(team => team.principale) ?? equipes[0];
    if (primary) { http.expectOne(`${base}/teams/equipes/${primary.id}/events`).flush([]); tick(); fixture.detectChanges(); }
  }
  function fill(input: HTMLInputElement, value: string) {
    input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); tick(); fixture.detectChanges();
  }
  function button(text: string) {
    return Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(button => button.textContent?.trim() === text)!;
  }

  it('place Options, les équipes puis Ajouter une équipe dans l’entête et ouvre l’équipe principale', fakeAsync(() => {
    open();
    expect(tabs().map(tab => tab.textContent?.trim().replace(/^＋\s*/, ''))).toEqual(['Options', 'Débutants', 'Confirmés', 'Ajouter une équipe']);
    expect(fixture.nativeElement.querySelector('.activity-header').textContent).not.toContain("Choix de l'activité");
    expect(tabs()[1].getAttribute('aria-selected')).toBe('true');
    expect(fixture.nativeElement.querySelector('app-team-events').textContent).toContain('Événements · Débutants');
    http.expectNone(request => request.url.endsWith('/events'));
    clickTab('Confirmés'); http.expectOne(`${base}/teams/equipes/11/events`).flush([]); tick(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#activity-panel-options').hidden).toBeTrue();
    expect(fixture.nativeElement.querySelector('#activity-panel-team-11')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-team-events').textContent).toContain('Événements · Confirmés');
    expect(fixture.nativeElement.querySelector('.activity-footer').hidden).toBeTrue();
  }));

  it('conserve les paramètres et le brouillon d’événement en passant par Options sans recharger les équipes', fakeAsync(() => {
    open(); fill(fixture.nativeElement.querySelector('input[name="nom"]'), 'Danse adultes');
    clickTab('Débutants');
    button('Ajouter un événement').click(); tick(); fixture.detectChanges();
    const date = document.querySelector<HTMLInputElement>('app-team-event-modal input[type="datetime-local"]')!;
    fill(date, '2027-01-10T17:45');
    clickTab('Options');
    expect((fixture.nativeElement.querySelector('input[name="nom"]') as HTMLInputElement).value).toBe('Danse adultes');
    clickTab('Débutants');
    expect(document.querySelector<HTMLInputElement>('app-team-event-modal input[type="datetime-local"]')!.value).toBe('2027-01-10T17:45');
    http.expectNone(request => request.url.endsWith('/events') || request.url.endsWith('/teams/activities/7'));
  }));

  it('conserve le brouillon d’équipe et ouvre son onglet après création', fakeAsync(() => {
    open(); clickTab('Ajouter une équipe');
    fill(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"] input'), 'Juniors');
    clickTab('Options'); clickTab('Ajouter une équipe');
    expect(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"] input').value).toBe('Juniors');
    button('Enregistrer l’équipe').click(); fixture.detectChanges();
    expect(tabs().every(tab => tab.disabled)).toBeTrue();
    const request = http.expectOne(`${base}/teams/activities/7/equipes`);
    expect(request.request.body.nom).toBe('Juniors');
    const created = team(12, 'Juniors'); request.flush(created);
    http.expectOne(`${base}/teams/activities/7`).flush(state([...state().equipes, created])); tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/12/events`).flush([]); tick(); fixture.detectChanges();
    expect(tabs().map(tab => tab.textContent?.trim().replace(/^＋\s*/, ''))).toEqual(['Options', 'Débutants', 'Confirmés', 'Juniors', 'Ajouter une équipe']);
    expect(tabs().find(tab => tab.textContent?.trim() === 'Juniors')?.getAttribute('aria-selected')).toBe('true');
  }));

  it('met à jour le nom de l’onglet et revient à l’équipe principale après archivage', fakeAsync(() => {
    open(); clickTab('Confirmés'); http.expectOne(`${base}/teams/equipes/11/events`).flush([]); tick(); fixture.detectChanges();
    button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    fill(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"] input'), 'Avancés');
    button('Enregistrer l’équipe').click(); http.expectOne(`${base}/teams/activities/7/equipes/11`).flush(team(11, 'Avancés'));
    http.expectOne(`${base}/teams/activities/7`).flush(state([team(10, 'Débutants'), team(11, 'Avancés')])); tick(); fixture.detectChanges();
    expect(tabs()[2].textContent?.trim()).toBe('Avancés');
    http.expectNone(request => request.url.endsWith('/events'));
    spyOn(window, 'confirm').and.returnValue(true); button('Archiver').click();
    http.expectOne(`${base}/teams/activities/7/equipes/11`).flush(null);
    http.expectOne(`${base}/teams/activities/7`).flush(state([team(10, 'Débutants')])); tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([]); tick(); fixture.detectChanges();
    expect(tabs()[1].getAttribute('aria-selected')).toBe('true');
    expect(tabs().length).toBe(3);
  }));

  it('sélectionne l’équipe principale même si elle n’est pas la première de la liste', fakeAsync(() => {
    open([team(11, 'Confirmés'), team(10, 'Débutants')]);
    expect(tabs()[2].getAttribute('aria-selected')).toBe('true');
    expect(fixture.componentInstance.selectedTeamId).toBe(10);
  }));

  it('conserve Options si aucune équipe n’existe', fakeAsync(() => {
    open([]);
    expect(tabs()[0].getAttribute('aria-selected')).toBe('true');
    http.expectNone(request => request.url.endsWith('/events'));
  }));
});
