import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatComponent } from './chat.component';
import { TokenStorageService } from '../../_services/token-storage.service';
import { environment } from '../../../environments/environment';
import { ChatAdministrationComponent } from './chat-administration.component';
import { By } from '@angular/platform-browser';
import { SectionManagementService } from '../../_services/section-management.service';
import { of } from 'rxjs';

describe('Chat des activités', () => {
  let component: ChatComponent;
  let http: HttpTestingController;
  let roles: string[];
  let sections: jasmine.SpyObj<SectionManagementService>;
  const url = environment.server + '/chat';
  const pilates = { id: 7, nom: 'Pilates', groupeFiltre: 'Adultes', categories: [{ descriptif: 'Débutants', jour: 'Mardi', horaire: '18:00' }], unreadCount: 3, canWrite: true };
  const yoga = { id: 8, nom: 'Yoga', groupeFiltre: 'Adultes', categories: [{ descriptif: 'Confirmés', jour: 'Jeudi', horaire: '19:00' }], unreadCount: 1, canWrite: true };
  const message = (id: number) => ({ id, senderUserId: 4, senderName: 'Alice', content: 'Bonjour', createdAt: '2026-10-02T10:00:00Z' });

  beforeEach(() => {
    roles = [];
    sections = jasmine.createSpyObj('SectionManagementService', ['sections', 'chats', 'createChat', 'updateChat', 'deleteChat']);
    sections.sections.and.returnValue(of([]));
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: TokenStorageService, useValue: { getUser: () => ({ id: 4, roles }) } },
      { provide: SectionManagementService, useValue: sections }] });
    component = TestBed.runInInjectionContext(() => new ChatComponent());
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { component.ngOnDestroy(); http.verify({ ignoreCancelled: true }); });

  it('masque le paramétrage sans administration ni section confiée', () => {
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#chat-administration-button')).toBeNull();
    fixture.componentInstance.openAdministration();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chat-admin-modal')).toBeNull();
    fixture.destroy();
  });

  it('ouvre l’administration depuis Chat et actualise les salons après création', () => {
    roles.push('ROLE_ADMIN');
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('#chat-administration-button').click();
    fixture.detectChanges();
    http.expectOne(environment.server + '/param/chats').flush([]);
    http.expectOne(environment.server + '/param/sections').flush([]);
    http.expectOne(environment.server + '/activite/all').flush([]);
    const admin = fixture.debugElement.query(By.directive(ChatAdministrationComponent)).componentInstance as ChatAdministrationComponent;
    expect(fixture.nativeElement.querySelector('[role="dialog"][aria-labelledby="chat-admin-title"]')).not.toBeNull();
    admin.afficherCreationChat();
    admin.nouveauChatNom = 'Informations';
    admin.basculerLectureChat(admin.rolesChat.find(role => role.code === 'ROLE_USER')!.code);
    admin.creerChat();
    const created = { id: 10, nom: 'Informations', cible: 'ASSOCIATION', permissions: [{ role: 'ROLE_USER', ecriture: false }] };
    const request = http.expectOne(environment.server + '/param/chats');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ nom: 'Informations', cible: 'ASSOCIATION', cibleId: undefined, permissions: created.permissions });
    request.flush(created);
    http.expectOne(url).flush([{ id: 10, nom: 'Informations', groupeFiltre: '', categories: [], unreadCount: 0, canWrite: false }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chat créé.');
    expect(fixture.componentInstance.rooms().map(room => room.nom)).toEqual(['Informations']);
    admin.close();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chat-admin-modal')).toBeNull();
    fixture.destroy();
  });

  it('limite le paramétrage du référent aux chats de sa section', () => {
    sections.sections.and.returnValue(of([{ id: 4, nom: 'Basket', couleur: '#000000' }]));
    sections.chats.and.returnValue(of([{ id: 10, nom: 'Informations', cible: 'SECTION', cibleId: 4,
      permissions: [] }]));
    const fixture = TestBed.createComponent(ChatComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('#chat-administration-button').click();
    fixture.detectChanges();

    const admin = fixture.debugElement.query(By.directive(ChatAdministrationComponent)).componentInstance as ChatAdministrationComponent;
    expect(sections.chats).toHaveBeenCalledOnceWith(4);
    expect(admin.chats.map(chat => chat.nom)).toEqual(['Informations']);
    admin.afficherCreationChat();
    expect(admin.nouveauChatCible).toBe('SECTION');
    expect(admin.nouveauChatCibleId).toBe(4);
    admin.nouveauChatNom = 'Équipe';
    sections.createChat.and.returnValue(of({ id: 11, nom: 'Équipe', cible: 'SECTION', cibleId: 4, permissions: [] }));
    admin.creerChat();
    expect(sections.createChat).toHaveBeenCalledOnceWith(4,
      { nom: 'Équipe', cible: 'SECTION', cibleId: 4, permissions: [] });
    http.expectOne(url).flush([]);
    fixture.destroy();
  });

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
