import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { EventChatMessage, TeamService } from '../../_services/team.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { EventChatComponent } from './event-chat.component';

describe('Chat de l’événement', () => {
  let fixture: ComponentFixture<EventChatComponent>;
  let api: jasmine.SpyObj<TeamService>;
  const message = (id: number): EventChatMessage => ({ id, senderUserId: 1, senderName: 'Alice Martin',
    content: `Message ${id}`, createdAt: '2026-11-09T15:00:00Z' });
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['eventMessages', 'sendEventMessage']);
    api.eventMessages.and.returnValue(of([message(1)]));
    TestBed.configureTestingModule({ imports: [EventChatComponent], providers: [
      { provide: TeamService, useValue: api }, { provide: TokenStorageService, useValue: { getUser: () => ({ id: 1 }) } }
    ] });
    fixture = TestBed.createComponent(EventChatComponent); fixture.componentRef.setInput('eventId', 20);
  });
  afterEach(() => fixture.destroy());
  it('interrompt l’actualisation dans un onglet masqué et conserve le brouillon', fakeAsync(() => {
    fixture.componentRef.setInput('active', false); fixture.detectChanges(); tick(5000);
    expect(api.eventMessages).not.toHaveBeenCalled();
    fixture.componentRef.setInput('active', true); fixture.detectChanges(); tick(0);
    expect(api.eventMessages).toHaveBeenCalledWith(20, undefined);
    fixture.componentInstance.draft.set('À demain');
    fixture.componentRef.setInput('active', false); fixture.detectChanges(); tick(10000);
    expect(api.eventMessages).toHaveBeenCalledTimes(1);
    fixture.componentRef.setInput('active', true); fixture.detectChanges(); tick(0);
    expect(api.eventMessages).toHaveBeenCalledWith(20, { afterId: 1 });
    expect(fixture.componentInstance.draft()).toBe('À demain');
    fixture.destroy();
  }));
  it('conserve le texte en cas d’erreur, refuse les doublons et ne saute aucun message après un envoi', fakeAsync(() => {
    fixture.detectChanges(); tick(0);
    const chat = fixture.componentInstance;
    chat.draft.set('  Bonjour  ');
    api.sendEventMessage.and.returnValue(throwError(() => ({ status: 500 })));
    chat.send();
    expect(chat.draft()).toBe('  Bonjour  '); expect(chat.sendError()).toContain('conservé');
    const sent = new Subject<EventChatMessage>(); api.sendEventMessage.and.returnValue(sent);
    chat.send(); chat.send();
    expect(api.sendEventMessage).toHaveBeenCalledTimes(2);
    sent.next(message(3)); sent.complete();
    expect(chat.draft()).toBe('');
    api.eventMessages.and.returnValue(of([message(2), message(3)])); tick(5000);
    expect(api.eventMessages).toHaveBeenCalledWith(20, { afterId: 1 });
    expect(chat.messages().map(m => m.id)).toEqual([1, 2, 3]);
    fixture.destroy();
  }));
  it('charge l’historique, isole les événements et affiche les messages comme du texte', fakeAsync(() => {
    api.eventMessages.and.returnValue(of(Array.from({ length: 50 }, (_, index) => message(index + 2))));
    fixture.detectChanges(); tick(0); expect(fixture.componentInstance.hasOlder()).toBe(true);
    api.eventMessages.and.returnValue(of([{ ...message(1), content: '<img src=x onerror=alert(1)>' }]));
    fixture.componentInstance.older(); fixture.detectChanges();
    expect(api.eventMessages).toHaveBeenCalledWith(20, { beforeId: 2 });
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('<img src=x onerror=alert(1)>');
    api.eventMessages.and.returnValue(of([])); fixture.componentRef.setInput('eventId', 21); fixture.detectChanges(); tick(0);
    expect(api.eventMessages).toHaveBeenCalledWith(21, undefined);
    expect(fixture.componentInstance.messages()).toEqual([]);
    fixture.destroy();
  }));
  it('bloque les messages vides ou trop longs et arrête l’actualisation après une perte d’accès', fakeAsync(() => {
    fixture.detectChanges(); tick(0); const chat = fixture.componentInstance;
    for (const draft of ['   ', 'a'.repeat(2001)]) { chat.draft.set(draft); chat.send(); }
    expect(api.sendEventMessage).not.toHaveBeenCalled();
    api.eventMessages.and.returnValue(throwError(() => ({ status: 403 }))); tick(5000);
    expect(chat.denied()).toBe(true);
    tick(10000); expect(api.eventMessages).toHaveBeenCalledTimes(2);
    fixture.destroy();
  }));
});
