import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatComponent } from './chat.component';
import { TokenStorageService } from '../../_services/token-storage.service';
import { environment } from '../../../environments/environment';

describe('Chat des activités', () => {
  let component: ChatComponent;
  let http: HttpTestingController;
  const url = environment.server + '/chat';
  const pilates = { id: 7, nom: 'Pilates', groupeFiltre: 'Adultes', categories: [{ descriptif: 'Débutants', jour: 'Mardi', horaire: '18:00' }], unreadCount: 3, canWrite: true };
  const yoga = { id: 8, nom: 'Yoga', groupeFiltre: 'Adultes', categories: [{ descriptif: 'Confirmés', jour: 'Jeudi', horaire: '19:00' }], unreadCount: 1, canWrite: true };
  const message = (id: number) => ({ id, senderUserId: 4, senderName: 'Alice', content: 'Bonjour', createdAt: '2026-10-02T10:00:00Z' });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: TokenStorageService, useValue: { getUser: () => ({ id: 4 }) } }] });
    component = TestBed.runInInjectionContext(() => new ChatComponent());
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { component.ngOnDestroy(); http.verify({ ignoreCancelled: true }); });

  it('affiche les salons dans l’ordre alphabétique sans ouvrir de modale', () => {
    component.ngOnInit();
    http.expectOne(url).flush([yoga, pilates]);
    expect(component.rooms().map(room => room.nom)).toEqual(['Pilates', 'Yoga']);
    expect(component.activeRoom()).toBeUndefined();
  });

  it('place les salons ayant des messages non lus avant les autres', () => {
    component.ngOnInit();
    http.expectOne(url).flush([
      { ...pilates, id: 9, nom: 'Zumba', unreadCount: 0 },
      { ...yoga, id: 8, nom: 'Yoga', unreadCount: 2 },
      { ...pilates, id: 7, nom: 'Badminton', unreadCount: 1 },
      { ...yoga, id: 10, nom: 'Aquagym', unreadCount: 0 }
    ]);
    expect(component.filteredRooms().map(room => room.nom)).toEqual(['Badminton', 'Yoga', 'Aquagym', 'Zumba']);
  });

  it('filtre les salons par activité, catégorie ou horaire sans tenir compte des accents', () => {
    component.ngOnInit();
    http.expectOne(url).flush([yoga, pilates]);
    component.filter.set('pilates');
    expect(component.filteredRooms().map(room => room.id)).toEqual([7]);
    component.filter.set('confirmes');
    expect(component.filteredRooms().map(room => room.id)).toEqual([8]);
    component.filter.set('18:00');
    expect(component.filteredRooms().map(room => room.id)).toEqual([7]);
  });

  it('affiche un chat global en lecture seule et autorise le formulaire des rédacteurs', fakeAsync(() => {
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    const communication = { id: 10, nom: 'Informations', groupeFiltre: '', categories: [], unreadCount: 0, canWrite: false };
    http.expectOne(url).flush([communication]);
    fixture.componentInstance.open(communication);
    tick(0);
    http.expectOne(url + '/10/messages').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('textarea')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Canal en lecture seule');
    fixture.componentInstance.rooms.set([{ ...communication, canWrite: true }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('textarea')).not.toBeNull();
    fixture.componentInstance.updateDraft(10, 'Information');
    fixture.componentInstance.send({ ...communication, canWrite: true });
    http.expectOne(url + '/10/messages').flush(message(10));
    fixture.destroy();
  }));

  it('affiche le rôle le plus élevé avec sa couleur à côté de l’auteur', fakeAsync(() => {
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([pilates]);
    fixture.componentInstance.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages').flush([
      { ...message(10), senderRole: 'REFERENT_ACTIVITE' },
      { ...message(11), senderRole: 'ENCADRANT' },
      { ...message(12), senderRole: 'MEMBRECA' },
      { ...message(13), senderRole: 'SECRETAIRE' }
    ]);
    http.expectOne(url + '/7/read').flush({ unreadCount: 0 });
    fixture.detectChanges();
    const roles = fixture.nativeElement.querySelectorAll('.chat-role');
    expect([...roles].map((item: any) => item.textContent.trim())).toEqual(['Référent d’activité', 'Encadrant', 'Membre du CA', 'Secrétariat']);
    expect(roles[0].classList).toContain('role-referent_activite');
    expect(roles[1].classList).toContain('role-encadrant');
    expect(roles[2].classList).toContain('role-membreca');
    expect(roles[3].classList).toContain('role-secretaire');
    fixture.destroy();
  }));

  it('charge la discussion ouverte dans la modale et conserve les messages déjà chargés', fakeAsync(() => {
    component.ngOnInit();
    http.expectOne(url).flush([pilates, yoga]);
    component.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages').flush([message(10)]);
    http.expectOne(url + '/7/read').flush({ unreadCount: 0 });
    component.open(yoga);
    tick(0);
    http.expectOne(url + '/8/messages').flush([message(20)]);
    http.expectOne(url + '/8/read').flush({ unreadCount: 0 });
    expect(component.activeRoom()?.id).toBe(8);
    expect(component.state(7).messages.map(item => item.id)).toEqual([10]);
    expect(component.state(8).messages.map(item => item.id)).toEqual([20]);
  }));

  it('arrête l’actualisation à la fermeture de la modale et conserve le brouillon au retour', fakeAsync(() => {
    component.ngOnInit();
    http.expectOne(url).flush([pilates]);
    component.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages').flush([message(10)]);
    http.expectOne(url + '/7/read').flush({ unreadCount: 0 });
    component.updateDraft(7, 'Brouillon');
    component.close();
    expect(component.activeRoom()).toBeUndefined();
    tick(5000);
    http.expectNone(url + '/7/messages?afterId=10');
    component.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages?afterId=10').flush([]);
    expect(component.state(7).draft).toBe('Brouillon');
  }));

  it('conserve les non lus des salons fermés et réessaie un accusé de lecture en échec', fakeAsync(() => {
    component.ngOnInit();
    http.expectOne(url).flush([pilates, yoga]);
    expect(component.unreadCounts().get(7)).toBe(3);
    component.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages').flush([message(10)]);
    http.expectOne(url + '/7/read').flush({}, { status: 500, statusText: 'Erreur' });
    expect(component.unreadCounts().get(7)).toBe(3);
    tick(5000);
    http.expectOne(url + '/7/messages?afterId=10').flush([]);
    const read = http.expectOne(url + '/7/read');
    expect(read.request.body).toEqual({ lastMessageId: 10 });
    read.flush({ unreadCount: 0 });
    expect(component.unreadCounts().get(7)).toBe(0);
    expect(component.unreadCounts().get(8)).toBe(1);
  }));

  it('affiche le compteur dans le bandeau de chaque activité', () => {
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([pilates, yoga]);
    fixture.detectChanges();
    const counts = fixture.nativeElement.querySelectorAll('.chat-unread-count');
    expect([...counts].map((item: any) => item.textContent.trim())).toEqual(['3', '1']);
    expect(counts[0].getAttribute('aria-label')).toBe('3 messages non lus');
    expect(fixture.nativeElement.querySelector('.chat-title span').textContent).toContain('AdultesDébutants · Mardi · 18:00');
    fixture.destroy();
  });

  it('ne saute pas de messages entrants après un envoi', fakeAsync(() => {
    component.ngOnInit();
    http.expectOne(url).flush([pilates]);
    component.open(pilates);
    tick(0);
    http.expectOne(url + '/7/messages').flush([message(10)]);
    http.expectOne(url + '/7/read').flush({ unreadCount: 0 });
    component.updateDraft(7, 'Bonjour');
    component.send(pilates);
    http.expectOne(url + '/7/messages').flush(message(70));
    tick(5000);
    http.expectOne(url + '/7/messages?afterId=10').flush([message(11)]);
    const read = http.expectOne(url + '/7/read');
    expect(read.request.body.lastMessageId).toBe(11);
    read.flush({ unreadCount: 2 });
    expect(component.unreadCounts().get(7)).toBe(2);
  }));
});
