import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of } from 'rxjs';
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
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(ActiviteEditorComponent);
    component = fixture.componentInstance;
    component.activite = new Activite();
    component.saved.subscribe(saved);
    activites.save.and.callFake(activite => of(activite));
  });

  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); });

  it('bloque le bouton tant qu’aucune section n’est choisie puis enregistre la section sélectionnée', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const bouton = document.querySelector('.activity-footer .btn-success') as HTMLButtonElement;
    const select = document.querySelector('#sectionActivite') as HTMLSelectElement;
    expect(bouton.disabled).toBeTrue();
    expect(select.required).toBeTrue();
    expect(select.options[0].disabled).toBeTrue();
    expect(document.body.textContent).toContain('Choisissez une section pour enregistrer l’activité.');
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

describe('Équipes et options de la fiche activité', () => {
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
      { provide: ParamService, useValue: { getSections: () => of([{ id: 3, nom: 'Culture' }]), getSalles: () => of([]) } }
    ] });
    TestBed.inject(NgbConfig).animation = false;
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ActiviteEditorComponent);
    fixture.componentInstance.activite = Object.assign(new Activite(), { id: 7, nom: 'Danse', section: { id: 3, nom: 'Culture' } });
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); http.verify(); });
  const teamButtons = () => Array.from(fixture.nativeElement.querySelectorAll('.activity-team-button') as NodeListOf<HTMLButtonElement>);
  function selectTeam(text: string) {
    teamButtons().find(button => button.textContent?.trim() === text)!.click();
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
    return Array.from(document.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(button => button.textContent?.trim() === text)!;
  }
  function openOptions() { buttonBySelector('.activity-settings').click(); tick(); fixture.detectChanges(); }
  function closeOptions() { document.querySelector<HTMLButtonElement>('ngb-modal-window .btn-close')!.click(); tick(); fixture.detectChanges(); }
  function buttonBySelector(selector: string): HTMLButtonElement { return fixture.nativeElement.querySelector(selector); }

  it('remplace les onglets par des boutons et affiche l’équipe principale puis l’équipe choisie', fakeAsync(() => {
    open();
    expect(fixture.nativeElement.querySelector('[role="tablist"], [role="tab"], [role="tabpanel"]')).toBeNull();
    expect(teamButtons().map(button => button.textContent?.trim())).toEqual(['Débutants', 'Confirmés']);
    expect(teamButtons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(button('Modifier l’équipe').parentElement).toBe(button('＋ Ajouter une équipe').parentElement);
    expect(button('Modifier l’équipe').parentElement!.classList).toContain('activity-teams');
    expect(button('Archiver')).toBeUndefined();
    expect(fixture.nativeElement.querySelector('app-team-events h5').textContent).toBe('Événements');
    expect(document.querySelector('ngb-modal-window')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name="nom"]')).toBeNull();
    selectTeam('Confirmés'); http.expectOne(`${base}/teams/equipes/11/events`).flush([]); tick(); fixture.detectChanges();
    expect(teamButtons()[1].getAttribute('aria-pressed')).toBe('true');
    expect(fixture.nativeElement.querySelector('app-team-events h5').textContent).toBe('Événements');
  }));

  it('ouvre les options à côté du titre et conserve le brouillon à la fermeture sans recharger les équipes', fakeAsync(() => {
    open();
    expect(buttonBySelector('.activity-settings').parentElement!.querySelector('h1')!.textContent).toBe('Danse');
    expect(buttonBySelector('.activity-settings').getAttribute('aria-haspopup')).toBe('dialog');
    openOptions();
    expect(document.querySelector('ngb-modal-window')!.getAttribute('aria-labelledby')).toBe('activity-options-title');
    fill(document.querySelector<HTMLInputElement>('ngb-modal-window input[name="nom"]')!, 'Danse adultes');
    closeOptions();
    expect(document.querySelector('ngb-modal-window')).toBeNull();
    expect(teamButtons()[0].getAttribute('aria-pressed')).toBe('true');
    openOptions();
    expect(document.querySelector<HTMLInputElement>('ngb-modal-window input[name="nom"]')!.value).toBe('Danse adultes');
    document.querySelector<HTMLButtonElement>('ngb-modal-window .btn-success')!.click();
    const save = http.expectOne(`${base}/activite/save`);
    expect(save.request.body.nom).toBe('Danse adultes');
    save.flush(fixture.componentInstance.activite); tick(); fixture.detectChanges();
    expect(document.querySelector('ngb-modal-window')).toBeNull();
    http.expectNone(request => request.url.endsWith('/events') || request.url.endsWith('/teams/activities/7'));
  }));

  it('garde les options ouvertes si l’enregistrement échoue', fakeAsync(() => {
    open(); openOptions();
    document.querySelector<HTMLButtonElement>('ngb-modal-window .btn-success')!.click();
    http.expectOne(`${base}/activite/save`).flush({ detail: 'Erreur' }, { status: 500, statusText: 'Server Error' });
    tick(); fixture.detectChanges();
    expect(document.querySelector('ngb-modal-window')).not.toBeNull();
    expect(document.querySelector<HTMLButtonElement>('ngb-modal-window .btn-success')!.disabled).toBeFalse();
  }));

  it('conserve le brouillon d’équipe après consultation des options et sélectionne l’équipe créée', fakeAsync(() => {
    open(); button('＋ Ajouter une équipe').click(); tick(); fixture.detectChanges();
    fill(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"] input'), 'Juniors');
    openOptions(); closeOptions();
    expect(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"] input').value).toBe('Juniors');
    button('Enregistrer l’équipe').click(); fixture.detectChanges();
    expect(teamButtons().every(button => button.disabled)).toBeTrue();
    const request = http.expectOne(`${base}/teams/activities/7/equipes`);
    expect(request.request.body.nom).toBe('Juniors');
    const created = team(12, 'Juniors'); request.flush(created);
    http.expectOne(`${base}/teams/activities/7`).flush(state([...state().equipes, created])); tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/12/events`).flush([]); tick(); fixture.detectChanges();
    expect(teamButtons().map(button => button.textContent?.trim())).toEqual(['Débutants', 'Confirmés', 'Juniors']);
    expect(teamButtons()[2].getAttribute('aria-pressed')).toBe('true');
  }));

  it('modifie dans une modale et revient à l’équipe principale après suppression depuis cette modale', fakeAsync(() => {
    open(); selectTeam('Confirmés'); http.expectOne(`${base}/teams/equipes/11/events`).flush([]); tick(); fixture.detectChanges();
    button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    expect(document.querySelector('ngb-modal-window')!.getAttribute('aria-labelledby')).toBe('team-edit-title');
    expect(fixture.nativeElement.querySelector('[aria-label="Édition d’équipe"]')).toBeNull();
    fill(document.querySelector('[aria-label="Édition d’équipe"] input')!, 'Avancés');
    button('Enregistrer l’équipe').click(); http.expectOne(`${base}/teams/activities/7/equipes/11`).flush(team(11, 'Avancés')); tick();
    http.expectOne(`${base}/teams/activities/7`).flush(state([team(10, 'Débutants'), team(11, 'Avancés')])); tick(); fixture.detectChanges();
    expect(teamButtons()[1].textContent?.trim()).toBe('Avancés');
    expect(document.querySelector('app-team-edit-modal')).toBeNull();
    http.expectNone(request => request.url.endsWith('/events'));
    button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    expect(document.querySelector('app-team-edit-modal')!.contains(button('Supprimer l’équipe'))).toBeTrue();
    spyOn(window, 'confirm').and.returnValue(true); button('Supprimer l’équipe').click();
    const deletion = http.expectOne(`${base}/teams/activities/7/equipes/11`);
    expect(deletion.request.method).toBe('DELETE'); deletion.flush(null); tick();
    http.expectOne(`${base}/teams/activities/7`).flush(state([team(10, 'Débutants')])); tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([]); tick(); fixture.detectChanges();
    expect(teamButtons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(teamButtons().length).toBe(1);
    expect(document.querySelector('app-team-edit-modal')).toBeNull();
  }));

  it('protège la première équipe de la suppression et ferme la modale sans sauvegarder le brouillon', fakeAsync(() => {
    open(); button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    expect(button('Supprimer l’équipe').disabled).toBeTrue();
    button('Supprimer l’équipe').click();
    fill(document.querySelector('[aria-label="Édition d’équipe"] input')!, 'Brouillon');
    button('Fermer').click(); tick(); fixture.detectChanges();
    expect(teamButtons()[0].textContent?.trim()).toBe('Débutants');
    expect(document.querySelector('app-team-edit-modal')).toBeNull();
    http.expectNone(r => r.method === 'PUT' || r.method === 'DELETE');
    button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    expect(document.querySelector<HTMLInputElement>('[aria-label="Édition d’équipe"] input')!.value).toBe('Débutants');
  }));

  it('affiche les erreurs de modification et de suppression dans la modale et permet de réessayer', fakeAsync(() => {
    open(); selectTeam('Confirmés'); http.expectOne(`${base}/teams/equipes/11/events`).flush([]); tick(); fixture.detectChanges();
    button('Modifier l’équipe').click(); tick(); fixture.detectChanges();
    const input = document.querySelector<HTMLInputElement>('[aria-label="Édition d’équipe"] input')!;
    fill(input, 'Avancés'); button('Enregistrer l’équipe').click(); fixture.detectChanges();
    expect(button('Supprimer l’équipe').disabled).toBeTrue();
    expect(document.querySelector<HTMLButtonElement>('app-team-edit-modal .btn-close')!.disabled).toBeTrue();
    http.expectOne(`${base}/teams/activities/7/equipes/11`).flush({ detail: 'Nom déjà utilisé' }, { status: 409, statusText: 'Conflict' }); tick(); fixture.detectChanges();
    expect(document.querySelector('app-team-edit-modal')!.textContent).toContain('Nom déjà utilisé');
    expect(input.value).toBe('Avancés');
    const confirmation = spyOn(window, 'confirm').and.returnValue(false);
    button('Supprimer l’équipe').click(); http.expectNone(r => r.method === 'DELETE');
    confirmation.and.returnValue(true); button('Supprimer l’équipe').click();
    http.expectOne(`${base}/teams/activities/7/equipes/11`).flush({ detail: 'Cette équipe a des événements à venir' }, { status: 409, statusText: 'Conflict' }); tick(); fixture.detectChanges();
    expect(document.querySelector('app-team-edit-modal')!.textContent).toContain('Cette équipe a des événements à venir');
    expect(button('Supprimer l’équipe').disabled).toBeFalse();
    button('Enregistrer l’équipe').click();
    http.expectOne(`${base}/teams/activities/7/equipes/11`).flush(team(11, 'Avancés')); tick();
    http.expectOne(`${base}/teams/activities/7`).flush(state([team(10, 'Débutants'), team(11, 'Avancés')])); tick(); fixture.detectChanges();
    expect(document.querySelector('app-team-edit-modal')).toBeNull();
    expect(teamButtons()[1].textContent?.trim()).toBe('Avancés');
  }));

  it('sélectionne l’équipe principale même si elle n’est pas la première', fakeAsync(() => {
    open([team(11, 'Confirmés'), team(10, 'Débutants')]);
    expect(teamButtons()[1].getAttribute('aria-pressed')).toBe('true');
  }));

  it('propose l’ajout d’une équipe quand aucune équipe n’existe', fakeAsync(() => {
    open([]);
    expect(teamButtons().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Aucune équipe.');
    expect(button('＋ Ajouter une équipe')).toBeDefined();
    http.expectNone(request => request.url.endsWith('/events'));
  }));
});
