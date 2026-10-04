import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatService } from './chat.service';
import { environment } from '../../environments/environment';

describe('Compteurs de messages non lus', () => {
  let chat: ChatService;
  let http: HttpTestingController;
  const url = environment.server + '/chat';
  const room = (unreadCount: number) => ({ id: 7, nom: 'Pilates', groupe: 'Adultes', horaire: '', unreadCount });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    chat = TestBed.inject(ChatService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { chat.monitorUnread(undefined); http.verify({ ignoreCancelled: true }); });

  it('actualise hors de la page Chat et efface les compteurs à la déconnexion', fakeAsync(() => {
    chat.monitorUnread(4);
    tick(0);
    http.expectOne(url).flush([room(3)]);
    expect(chat.totalUnread()).toBe(3);
    tick(5000);
    http.expectOne(url).flush([room(5)]);
    expect(chat.totalUnread()).toBe(5);
    chat.monitorUnread(undefined);
    expect(chat.totalUnread()).toBe(0);
    tick(5000);
    http.expectNone(url);
  }));

  it('ignore un ancien compteur reçu après un accusé de lecture', () => {
    chat.rooms().subscribe();
    http.expectOne(url).flush([room(3)]);
    chat.rooms().subscribe();
    const stale = http.expectOne(url);
    chat.markRead(7, 10).subscribe();
    http.expectOne(url + '/7/read').flush({ unreadCount: 1 });
    stale.flush([room(3)]);
    expect(chat.totalUnread()).toBe(1);
  });
});
