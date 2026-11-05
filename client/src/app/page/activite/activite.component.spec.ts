import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { apiChangeDetectionInterceptor } from '../../_helpers/auth.interceptor';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ToastService } from '../../_services/toast.service';
import { Activite } from '../../models';
import { environment } from '../../../environments/environment';
import { ActiviteComponent } from './activite.component';
import { ActivitesComponent } from '../activites/activites.component';
import { EvenementModalService } from '../../_services/evenement-modal.service';

describe('Navigation vers la page d’activité', () => {
  beforeAll(() => registerLocaleData(localeFr));
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let roles: string[];
  const base = environment.server;
  const activity = () => Object.assign(new Activite(), { id: 7, nom: 'Danse', section: { id: 3, nom: 'Culture' } });
  const primary = { id: 10, nom: 'Débutants', activiteId: 7, activiteNom: 'Danse', principale: true,
    archivee: false, canManage: true, membres: 0, description: '', encadrants: [], referents: [], effectif: [] };

  beforeEach(async () => {
    roles = ['ROLE_ADMIN'];
    TestBed.configureTestingModule({ providers: [
      provideRouter([
        { path: 'activites/nouvelle', component: ActiviteComponent },
        { path: 'activites/:activiteId', component: ActiviteComponent },
        { path: 'activites', component: ActivitesComponent }
      ]),
      provideHttpClient(withInterceptors([apiChangeDetectionInterceptor])), provideHttpClientTesting(),
      { provide: TokenStorageService, useValue: { getUser: () => ({ roles }) } },
      { provide: ToastService, useValue: jasmine.createSpyObj('toast', ['warning', 'success', 'error']) },
      { provide: ParamService, useValue: { getSections: () => of([]), getSalles: () => of([]) } }
    ] });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());

  async function flushActivity() {
    http.expectOne(`${base}/activite/7`).flush(activity());
    await harness.fixture.whenStable();
    harness.detectChanges();
    http.expectOne(`${base}/teams/activities/7`).flush({ equipes: [primary], candidats: [], intervenants: [] });
    await harness.fixture.whenStable();
    harness.detectChanges();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([]);
    harness.detectChanges();
    await harness.fixture.whenStable();
  }

  it('ouvre la page depuis une carte et active l’équipe principale', async () => {
    await harness.navigateByUrl('/activites', ActivitesComponent);
    http.expectOne(request => request.url === `${base}/activite/page`).flush({ content: [activity()], totalElements: 1, totalPages: 1, number: 0, size: 20 });
    harness.detectChanges();
    const card = harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.activity-card-link')!;
    expect(card.getAttribute('href')).toBe('/activites/7');
    card.click();
    await harness.fixture.whenStable();
    await flushActivity();
    expect(harness.routeNativeElement!.querySelector('h1')!.textContent).toBe('Danse');
    expect(harness.routeNativeElement!.querySelector('#activity-tab-team-10')!.getAttribute('aria-selected')).toBe('true');
    expect(document.querySelector('ngb-modal-window')).toBeNull();
  });

  it('charge une activité directement depuis son URL', async () => {
    await harness.navigateByUrl('/activites/7', ActiviteComponent);
    await flushActivity();
    expect(harness.routeNativeElement!.textContent).toContain('Événements · Débutants');
  });

  it('affiche les événements de la semaine prochaine et ouvre leur détail sans quitter le listing', async () => {
    const open = spyOn(TestBed.inject(EvenementModalService), 'ouvrir');
    const event = { id: 42, activiteId: 7, activiteNom: 'Danse – Débutants', activiteNomCourt: 'Danse',
      descriptif: 'Débutants / Confirmés', nomEvenement: 'Championnat', typeEvenement: 'RENCONTRE', adversaire: 'Club voisin',
      debut: '2026-11-09T18:30:00', fin: '2026-11-09T20:00:00', etatSeance: 'PROGRAMMEE' };
    await harness.navigateByUrl('/activites', ActivitesComponent);
    http.expectOne(request => request.url === `${base}/activite/page`).flush({
      content: [{ ...activity(), evenementsSemaineProchaine: [event] }], totalElements: 1, totalPages: 1, number: 0, size: 20
    });
    harness.detectChanges();
    const button = harness.routeNativeElement!.querySelector<HTMLButtonElement>('.activity-event')!;
    expect(button.textContent).toContain('Débutants / Confirmés');
    expect(button.textContent).toContain('09/11/2026 à 18:30');
    expect(button.textContent).toContain('Championnat contre Club voisin');
    expect(button.closest('a')).toBeNull();
    button.click();
    expect(open).toHaveBeenCalledWith(jasmine.objectContaining({
      seanceId: 42, titre: 'Championnat contre Club voisin', debut: event.debut, fin: event.fin
    }));
    expect(harness.routeNativeElement!.querySelector('h2')!.textContent).toBe('Activités');
  });

  it('indique l’absence d’événement la semaine prochaine', async () => {
    await harness.navigateByUrl('/activites', ActivitesComponent);
    http.expectOne(request => request.url === `${base}/activite/page`).flush({
      content: [activity()], totalElements: 1, totalPages: 1, number: 0, size: 20
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Aucun événement la semaine prochaine.');
    expect(harness.routeNativeElement!.querySelector('.activity-event')).toBeNull();
  });

  it('charge la liste filtrée pour un encadrant et masque les actions de configuration', async () => {
    roles = ['ROLE_ENCADRANT'];
    await harness.navigateByUrl('/activites', ActivitesComponent);
    http.expectOne(`${base}/section-management/sections`).flush([]);
    http.expectOne(request => request.url === `${base}/activite/managed/page`).flush({
      content: [activity()], totalElements: 1, totalPages: 1, number: 0, size: 20
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Activités dont vous encadrez une équipe');
    expect(harness.routeNativeElement!.querySelector('h3')!.textContent).toBe('Danse');
    expect(harness.routeNativeElement!.querySelector('a[href="/activites/nouvelle"]')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.activity-card-link')).toBeNull();
    const tariffInput = harness.routeNativeElement!.querySelector<HTMLInputElement>('#tarif-filter')!;
    tariffInput.value = '200';
    tariffInput.dispatchEvent(new Event('input'));
    http.expectOne(request => request.url === `${base}/activite/managed/page`
      && request.params.get('tarif') === '200').flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Aucune activité trouvée');
  });

  it('conserve les actions de gestion uniquement pour les activités des sections confiées', async () => {
    roles = ['ROLE_ENCADRANT'];
    await harness.navigateByUrl('/activites', ActivitesComponent);
    http.expectOne(`${base}/section-management/sections`).flush([{ id: 3, nom: 'Culture', couleur: '#000000' }]);
    http.expectOne(request => request.url === `${base}/activite/managed/page`).flush({
      content: [activity(), { ...activity(), id: 8, section: { id: 4, nom: 'Sports' } }],
      totalElements: 2, totalPages: 1, number: 0, size: 20
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('a[href="/activites/nouvelle"]')).not.toBeNull();
    expect(harness.routeNativeElement!.querySelectorAll('.activity-card-link').length).toBe(1);
  });

  it('affiche une erreur sans éditeur quand l’URL désigne une activité absente', async () => {
    await harness.navigateByUrl('/activites/99', ActiviteComponent);
    http.expectOne(`${base}/activite/99`).flush({ detail: 'Activité introuvable' }, { status: 404, statusText: 'Not Found' });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain('Activité introuvable');
    expect(harness.routeNativeElement!.querySelector('app-activite-editor')).toBeNull();
  });

  it('utilise l’accès limité aux sections confiées et ne rend pas une activité refusée', async () => {
    roles = ['ROLE_REFERENT'];
    await harness.navigateByUrl('/activites/7', ActiviteComponent);
    http.expectOne(`${base}/section-management/sections`).flush([{ id: 3, nom: 'Culture', couleur: '#000000' }]);
    http.expectOne(`${base}/activite/managed/7`).flush({ detail: 'Cette activité ne vous est pas confiée' }, { status: 403, statusText: 'Forbidden' });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('app-activite-editor')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain('Cette activité ne vous est pas confiée');
  });

  it('ouvre Options pour la création puis redirige vers l’activité enregistrée', async () => {
    const component = await harness.navigateByUrl('/activites/nouvelle', ActiviteComponent);
    expect(harness.routeNativeElement!.querySelector('#activity-tab-options')!.getAttribute('aria-selected')).toBe('true');
    component.onSaved(activity());
    await harness.fixture.whenStable();
    await flushActivity();
    expect(harness.routeNativeElement!.querySelector('#activity-tab-team-10')!.getAttribute('aria-selected')).toBe('true');
  });
});
