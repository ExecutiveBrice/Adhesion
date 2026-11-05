import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { apiChangeDetectionInterceptor } from '../../_helpers/auth.interceptor';
import { TeamManagementComponent } from './team-management.component';
import { TeamEvent, TeamState } from '../../_services/team.service';
import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { environment } from '../../../environments/environment';

describe('Ajout d’événements depuis la fiche activité', () => {
  let fixture: ComponentFixture<TeamManagementComponent>;
  let http: HttpTestingController;
  const base = environment.server;
  const state = (): TeamState => ({ 
    equipes: [{ id: 10, activiteId: 7, activiteNom: 'Danse', nom: 'Débutants', description: '', archivee: false,
      canManage: true, encadrants: [], referents: [], effectif: [], membres: 0 }],
    candidats: [], intervenants: [] });

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TeamManagementComponent], providers: [
      provideHttpClient(withInterceptors([apiChangeDetectionInterceptor])), provideHttpClientTesting()
    ] });
    TestBed.inject(NgbConfig).animation = false;
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TeamManagementComponent);
  });
  afterEach(() => { TestBed.inject(NgbModal).dismissAll(); fixture.destroy(); http.verify(); });

  function click(text: string) {
    const button = Array.from(document.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(b => b.textContent?.trim() === text);
    expect(button).withContext(`Bouton « ${text} » disponible`).toBeDefined();
    button!.click(); fixture.detectChanges();
  }
  function openTeam() {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    http.expectOne(`${base}/teams/activities/7`).flush(state());
    tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([]);
    tick(); fixture.detectChanges();
  }

  it('ne relance pas les chargements à chaque rafraîchissement et conserve le formulaire ouvert', fakeAsync(() => {
    openTeam();
    http.expectNone(`${base}/teams/equipes/10/events`);
    http.expectNone(`${base}/param/salles`);
    click('Ajouter un événement'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).not.toBeNull();
    http.expectNone(`${base}/teams/equipes/10/events`);
    click('Fermer'); tick(); fixture.detectChanges();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).toBeNull();
  }));

  it('enregistre un événement puis permet d’en ajouter un autre sans bloquer les boutons', fakeAsync(() => {
    openTeam(); click('Ajouter un événement'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    click('Enregistrer');
    const request = http.expectOne(`${base}/teams/equipes/10/events`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.equipes).toEqual([10]);
    const event: TeamEvent = { id: 50, ...request.request.body, etat: 'PROGRAMMEE', equipes: ['Débutants'],
      canManage: true, convocations: [], emails: {} };
    request.flush(event); tick();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([event]);
    tick(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Événement enregistré.');
    expect(document.querySelector('[aria-label="Édition d’événement"]')).toBeNull();
    click('Ajouter un événement'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).not.toBeNull();
  }));

  it('explique un rendez-vous après le début et permet de corriger avant l’envoi', fakeAsync(() => {
    openTeam(); click('Ajouter un événement'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    const dateInputs = document.querySelectorAll('input[type="datetime-local"]') as NodeListOf<HTMLInputElement>;
    function fill(input: HTMLInputElement, value: string) { input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); tick(); fixture.detectChanges(); }
    fill(dateInputs[0], '2026-11-04T18:00'); fill(dateInputs[1], '2026-11-04T19:00'); fill(dateInputs[2], '2026-11-04T18:23');
    const save = Array.from(document.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(b => b.textContent?.trim() === 'Enregistrer')!;
    expect(document.body.textContent).toContain('Le rendez-vous ne peut pas être postérieur au début de l’événement.');
    expect(save.disabled).toBeTrue();
    http.expectNone(r => r.method === 'POST');
    fill(dateInputs[2], '2026-11-04T17:45');
    expect(save.disabled).toBeFalse(); click('Enregistrer');
    const request = http.expectOne(`${base}/teams/equipes/10/events`);
    expect(request.request.body.rendezVous).toBe('2026-11-04T17:45');
    request.flush({ detail: 'Salle introuvable' }, { status: 400, statusText: 'Bad Request' });
    tick(); fixture.detectChanges();
    expect(document.body.textContent).toContain('Salle introuvable');
    expect(document.querySelector('[aria-label="Édition d’événement"]')).not.toBeNull();
    expect(save.disabled).toBeFalse();
  }));
});
