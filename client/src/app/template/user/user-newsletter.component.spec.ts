import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { UserComponent } from './user.component';
import { Adherent, User } from '../../models';
import { AdherentNewsletterSubscription, MailService } from '../../_services/mail.service';
import { AdherentService } from '../../_services/adherent.service';
import { UserService } from '../../_services/user.service';
import { AdhesionService } from '../../_services/adhesion.service';
import { ActiviteService } from '../../_services/activite.service';
import { UtilService } from '../../_services/util.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamService } from '../../_services/param.service';
import { FileService } from '../../_services/file.service';
import { ToastService } from '../../_services/toast.service';

describe('Adherent modal newsletter', () => {
  let fixture: ComponentFixture<UserComponent>;
  let component: UserComponent;
  let mail: jasmine.SpyObj<MailService>;
  const subscription = (status: AdherentNewsletterSubscription['status'], canModify = true): AdherentNewsletterSubscription =>
    ({ email: 'owner@example.org', status, canModify, canSubscribe: canModify, representativeEmail: false });

  beforeEach(() => {
    mail = jasmine.createSpyObj('MailService', ['getAdherentNewsletter', 'updateAdherentNewsletter']);
    TestBed.configureTestingModule({ imports: [UserComponent], providers: [provideRouter([]),
      { provide: MailService, useValue: mail },
      ...[AdherentService, UserService, AdhesionService, ActiviteService, UtilService, TokenStorageService,
        ParamService, FileService, ToastService, NgbActiveModal].map(provide => ({ provide, useValue: {} }))
    ] });
    spyOn(UserComponent.prototype, 'ngOnInit').and.stub();
    fixture = TestBed.createComponent(UserComponent);
    component = fixture.componentInstance;
    component.adherent = Object.assign(new Adherent(), { id: 7, completAdhesion: true, activitesNm1: [], user: new User() });
  });
  afterEach(() => fixture.destroy());

  it('hides the frame and prevents requests when the representative email is used', () => {
    component.newsletter = subscription('SUBSCRIBED');
    component.adherent.representant = Object.assign(new Adherent(), { user: new User() });
    component.adherent.emailRepresentant = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-labelledby="adherent-newsletter-title"]')).toBeNull();
    component.modifierNewsletter(true);
    component.modifierNewsletter(false);
    component.chargerNewsletter();
    expect(mail.getAdherentNewsletter).not.toHaveBeenCalled();
    expect(mail.updateAdherentNewsletter).not.toHaveBeenCalled();
  });

  it('allows represented members to subscribe and unsubscribe with their own email', () => {
    component.adherent.representant = Object.assign(new Adherent(), { user: new User() });
    component.adherent.emailRepresentant = false;
    mail.getAdherentNewsletter.and.returnValue(of(subscription('SUBSCRIBED')));
    mail.updateAdherentNewsletter.and.returnValues(of(subscription('NOT_SUBSCRIBED')), of(subscription('SUBSCRIBED')));
    component.chargerNewsletter(); fixture.detectChanges();
    const section: HTMLElement = fixture.nativeElement.querySelector('[aria-labelledby="adherent-newsletter-title"]');
    expect(section).not.toBeNull();
    section.querySelector('button')!.click(); fixture.detectChanges();
    expect(mail.updateAdherentNewsletter).toHaveBeenCalledWith(7, false, 'owner@example.org');
    expect(section.textContent).toContain('S’abonner');
    section.querySelector('button')!.click(); fixture.detectChanges();
    expect(mail.updateAdherentNewsletter).toHaveBeenCalledWith(7, true, 'owner@example.org');
    expect(section.textContent).toContain('Se désabonner');
  });

  it('discards an in-flight response when the saved member acquires a representative', () => {
    const pending = new Subject<AdherentNewsletterSubscription>();
    mail.getAdherentNewsletter.and.returnValue(pending);
    component.chargerNewsletter();
    component.adherent.representant = new Adherent();
    component.adherent.emailRepresentant = true;
    component.chargerNewsletter();
    pending.next(subscription('SUBSCRIBED'));
    expect(component.newsletter).toBeUndefined();
    expect(component.newsletterBusy).toBeFalse();
  });

  it('shows Brevo state and unsubscribes using the saved email', () => {
    mail.getAdherentNewsletter.and.returnValue(of(subscription('SUBSCRIBED')));
    mail.updateAdherentNewsletter.and.returnValue(of(subscription('NOT_SUBSCRIBED')));
    component.chargerNewsletter(); fixture.detectChanges();
    const section: HTMLElement = fixture.nativeElement.querySelector('[aria-labelledby="adherent-newsletter-title"]');
    expect(section.textContent).toContain('Abonné');
    const unsubscribe = Array.from(section.querySelectorAll('button')).find(button => button.textContent?.includes('Se désabonner'))!;
    unsubscribe.click(); fixture.detectChanges();
    expect(mail.updateAdherentNewsletter).toHaveBeenCalledWith(7, false, 'owner@example.org');
    expect(section.textContent).toContain('Non abonné');
  });

  it('keeps the button visible but disabled for a reader without modification rights', () => {
    component.newsletter = subscription('SUBSCRIBED', false); fixture.detectChanges();
    const section: HTMLElement = fixture.nativeElement.querySelector('[aria-labelledby="adherent-newsletter-title"]');
    const button = section.querySelector('button')!;
    expect(button.textContent).toContain('Se désabonner');
    expect(button.disabled).toBeTrue();
    component.modifierNewsletter(false);
    expect(mail.updateAdherentNewsletter).not.toHaveBeenCalled();
  });

  it('confirms global unblocking and preserves cancellation', () => {
    component.newsletter = subscription('BLOCKED');
    const confirm = spyOn(window, 'confirm').and.returnValue(false);
    component.modifierNewsletter(true);
    expect(confirm.calls.mostRecent().args[0]).toContain('autres listes');
    expect(mail.updateAdherentNewsletter).not.toHaveBeenCalled();
  });

  it('requires a fresh status after uncertain mutation and never replays it', () => {
    component.newsletter = subscription('SUBSCRIBED');
    mail.updateAdherentNewsletter.and.returnValue(throwError(() => new Error('network')));
    component.modifierNewsletter(false); component.modifierNewsletter(false);
    expect(component.newsletterNeedsRefresh).toBeTrue();
    expect(mail.updateAdherentNewsletter).toHaveBeenCalledTimes(1);
    mail.getAdherentNewsletter.and.returnValue(of(subscription('NOT_SUBSCRIBED')));
    component.chargerNewsletter();
    expect(component.newsletterNeedsRefresh).toBeFalse();
  });

  it('ignores old responses after saving a changed email and does not query unsaved members', () => {
    const old = new Subject<AdherentNewsletterSubscription>();
    mail.getAdherentNewsletter.and.returnValues(old, of({ ...subscription('NOT_SUBSCRIBED'), email: 'new@example.org' }));
    component.chargerNewsletter(); component.chargerNewsletter();
    old.next(subscription('SUBSCRIBED'));
    expect(component.newsletter?.email).toBe('new@example.org');
    component.adherent.id = 0; component.chargerNewsletter();
    expect(mail.getAdherentNewsletter).toHaveBeenCalledTimes(2);
  });
});
