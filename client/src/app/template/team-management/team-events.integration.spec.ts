import { provideRouter } from '@angular/router';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { apiChangeDetectionInterceptor } from '../../_helpers/auth.interceptor';
import { TeamManagementComponent } from './team-management.component';
import { TeamEvent, TeamState } from '../../_services/team.service';
import { ToastService } from '../../_services/toast.service';
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
    TestBed.configureTestingModule({ imports: [TeamManagementComponent], providers: [provideRouter([]),
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
  function openTeam(data = state(), events: TeamEvent[] = []) {
    fixture.componentRef.setInput('activityId', 7); fixture.componentRef.setInput('selectedTeamId', 10); fixture.detectChanges();
    http.expectOne(`${base}/teams/activities/7`).flush(data);
    tick(); fixture.detectChanges();
    http.expectOne(`${base}/teams/equipes/10/events`).flush(events);
    tick(); fixture.detectChanges();
    fixture.nativeElement.querySelector('[aria-controls="team-roster-7"]').click();
    fixture.nativeElement.querySelector('[aria-controls="team-events-10"]').click();
    fixture.detectChanges();
  }

  it('enregistre seulement les cases d’appartenance et rafraîchit leur état à la réponse du serveur', fakeAsync(() => {
    const data = state();
    data.candidats = [{ id: 1, prenom: 'Alice', nom: 'Martin', adhesionId: 100, statut: 'Validée' }];
    openTeam(data);
    const card = fixture.nativeElement.querySelector('.roster-member') as HTMLElement;
    const checkbox = card.querySelector('input') as HTMLInputElement;
    expect(checkbox.checked).toBeFalse();
    checkbox.click(); fixture.detectChanges();
    const add = http.expectOne(`${base}/teams/activities/7/equipes/10/effectif`);
    expect(add.request.method).toBe('PUT');
    expect(add.request.body).toEqual({ adhesions: [100] });
    expect(checkbox.disabled).toBeTrue();
    expect(document.querySelector('app-team-member-modal')).toBeNull();
    add.flush({ ...data.equipes[0], effectif: [100], membres: 1 }); tick();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([]); tick();
    expect(checkbox.checked).toBeTrue();
    expect(card.classList).not.toContain('roster-member-unselected');
    expect(checkbox.disabled).toBeFalse();
    expect(fixture.componentInstance.selectedTeam!.membres).toBe(1);
    checkbox.click(); fixture.detectChanges();
    const remove = http.expectOne(`${base}/teams/activities/7/equipes/10/effectif`);
    expect(remove.request.body.adhesions).toEqual([]);
    remove.flush({ detail: 'Enregistrement impossible' }, { status: 400, statusText: 'Bad Request' }); tick();
    expect(checkbox.checked).toBeTrue();
    expect(checkbox.disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement impossible');
    http.expectNone(`${base}/teams/activities/7`);
  }));

  it('charge les principales informations dans une modale sans modifier l’effectif', fakeAsync(() => {
    const data = state();
    data.candidats = [{ id: 1, prenom: 'Alice', nom: 'Martin', adhesionId: 100, statut: 'Validée' }];
    openTeam(data);
    fixture.nativeElement.querySelector('.roster-member-details').click(); fixture.detectChanges(); tick();
    const modal = document.querySelector('app-team-member-modal')!;
    expect(modal.textContent).toContain('Alice Martin');
    expect(modal.textContent).toContain('Chargement des informations');
    http.expectOne(`${base}/teams/activities/7/adhesions/100`).flush({
      personne: { ...data.candidats[0], paiementValide: true, inscriptionValidee: true, flag: false },
      naissance: '2010-05-12', email: 'parent@example.org', telephone: '0612345678', adresse: '12 rue des Sports',
      codePostal: '44000', ville: 'Nantes', representant: { prenom: 'Marie', nom: 'Martin', email: 'parent@example.org', telephone: '0612345678' }
    }); tick();
    expect(modal.textContent).toContain('12/05/2010');
    expect(modal.textContent).toContain('12 rue des Sports');
    expect(modal.textContent).toContain('Nantes');
    expect(modal.textContent).toContain('Marie Martin');
    expect(modal.querySelector('a[href="mailto:parent@example.org"]')).not.toBeNull();
    expect(modal.querySelector('a[href="tel:0612345678"]')).not.toBeNull();
    http.expectNone(r => r.method === 'PUT');
    expect((fixture.nativeElement.querySelector('.roster-member-checkbox') as HTMLInputElement).checked).toBeFalse();
    click('Fermer'); tick();
    expect(document.querySelector('app-team-member-modal')).toBeNull();
  }));

  it('permet de réessayer le chargement de la fiche et indique les informations manquantes', fakeAsync(() => {
    const data = state();
    data.candidats = [{ id: 1, prenom: 'Alice', nom: 'Martin', adhesionId: 100, statut: '' }];
    openTeam(data);
    fixture.nativeElement.querySelector('.roster-member-details').click(); fixture.detectChanges(); tick();
    http.expectOne(`${base}/teams/activities/7/adhesions/100`).flush({ detail: 'Fiche indisponible' }, { status: 503, statusText: 'Unavailable' }); tick();
    const modal = document.querySelector('app-team-member-modal')!;
    expect(modal.textContent).toContain('Fiche indisponible');
    click('Réessayer');
    http.expectOne(`${base}/teams/activities/7/adhesions/100`).flush({ personne: data.candidats[0], naissance: null,
      email: null, telephone: null, adresse: null, codePostal: null, ville: null, representant: null }); tick();
    expect(modal.textContent).toContain('Non renseignée');
    expect(modal.textContent).toContain('Non renseigné');
    expect(modal.textContent).not.toContain('Fiche indisponible');
    click('Fermer'); tick();
  }));

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

  it('permet au gestionnaire de saisir les trois réponses depuis les participants sans modifier la présence réelle', fakeAsync(() => {
    const event: TeamEvent = { id: 50, type: 'ACTIVITE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00',
      rendezVous: '', salleId: null, lieu: 'Studio', adversaire: '', commentaire: '',
      etat: 'PROGRAMMEE', equipes: ['Débutants'], canManage: true, emails: {},
      convocations: [{ id: 30, adherentId: 1, adhesionId: 100, nom: 'Alice Martin', selectionnee: true,
        publication: null, reponse: null, dateReponse: null, presence: false, canReply: true }] };
    openTeam(state(), [event]);
    fixture.nativeElement.querySelector('.participants-toggle').click(); fixture.detectChanges();
    for (const [index, value] of [true, false, null].entries()) {
      const group = fixture.nativeElement.querySelector('[aria-label="Réponse à l’invitation de Alice Martin"]');
      const buttons = group.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
      expect(buttons.length).toBe(3);
      buttons[index].click(); fixture.detectChanges();
      const request = http.expectOne(`${base}/teams/events/50/invitations/30/reply`);
      expect(request.request.method).toBe('PATCH');
      expect(request.request.body).toEqual({ value });
      expect(buttons[index].disabled).toBeTrue();
      event.convocations[0] = { ...event.convocations[0], reponse: value, dateReponse: '2026-11-09T12:00:00' };
      request.flush(event); tick();
      http.expectOne(`${base}/teams/equipes/10/events`).flush([event]); tick(); fixture.detectChanges();
      const selected = fixture.nativeElement.querySelector('[aria-label="Réponse à l’invitation de Alice Martin"] [aria-pressed="true"]');
      expect(selected.textContent.trim()).toBe(['Présent', 'Absent', 'Ne sais pas encore'][index]);
      expect(fixture.nativeElement.querySelector('[aria-label="Présence réelle de Alice Martin"] [aria-pressed="true"]').textContent.trim()).toBe('Absent');
      http.expectNone(r => r.url.endsWith('/attendance'));
    }
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
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ type: 'success', message: 'Événement enregistré.' }));
    expect(fixture.nativeElement.querySelector('.alert-success')).toBeNull();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).toBeNull();
    click('Ajouter un événement'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).not.toBeNull();
  }));

  it('modifie un événement existant et actualise les informations affichées', fakeAsync(() => {
    const event: TeamEvent = { id: 50, type: 'ACTIVITE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00',
      rendezVous: '3000-01-01T17:00:00', salleId: null, lieu: 'Studio', adversaire: '', commentaire: 'Avant',
      etat: 'PROGRAMMEE', equipes: ['Débutants'], canManage: true, convocations: [], emails: {} };
    openTeam(state(), [event]);
    click('Modifier'); http.expectOne(`${base}/param/salles`).flush([]); tick(); fixture.detectChanges();
    const form = document.querySelector('[aria-label="Édition d’événement"]')!;
    expect((form.querySelector('[name="debut"]') as HTMLInputElement).value).toBe('3000-01-01T18:00');
    const comment = form.querySelector('textarea') as HTMLTextAreaElement;
    expect(comment.value).toBe('Avant');
    comment.value = 'Après'; comment.dispatchEvent(new Event('input', { bubbles: true })); tick(); fixture.detectChanges();
    click('Enregistrer');
    const request = http.expectOne(`${base}/teams/events/50`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.commentaire).toBe('Après');
    const updated = { ...event, commentaire: 'Après', etat: 'MODIFIEE' };
    request.flush(updated); tick();
    http.expectOne(`${base}/teams/equipes/10/events`).flush([updated]); tick(); fixture.detectChanges();
    expect(document.querySelector('[aria-label="Édition d’événement"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('.event-card').textContent).toContain('Après');
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
