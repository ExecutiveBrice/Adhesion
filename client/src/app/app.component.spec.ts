import { TribuService } from './_services/tribu.service';
import { Component, signal } from '@angular/core';
import { ChatService } from './_services/chat.service';
import { ApiRenderService } from './_services/api-render.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthService } from './_services/auth.service';
import { ParamService } from './_services/param.service';
import { ParamTransmissionService } from './_services/transmission.service';
import { PwaService } from './_services/pwa.service';
import { ToastService } from './_services/toast.service';
import { TokenStorageService } from './_services/token-storage.service';
import { LoginPageService } from './_services/login-page.service';

@Component({ template: '' })
class HeaderTestPage {}

describe('Navigation du bandeau', () => {
  let fixture: ComponentFixture<AppComponent>;
  let router: Router;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let sessionChanges: Subject<StorageEvent>;

  beforeEach(async () => {
    sessionChanges = new Subject();
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'getUser'], {
      sessionChanges$: sessionChanges
    });
    storage.getToken.and.returnValue('access-token');
    storage.getUser.and.returnValue({ username: 'admin@example.org', roles: ['ROLE_ADMIN', 'ROLE_ENCADRANT'] });
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([
          { path: 'login', component: HeaderTestPage, title: 'Connexion' },
          { path: 'reporting', component: HeaderTestPage, title: 'Reporting' },
          { path: 'seances', component: HeaderTestPage, title: 'Séances' }
        ]),
        { provide: TokenStorageService, useValue: storage },
        { provide: AuthService, useValue: { logout: () => of(null) } },
        { provide: ParamService, useValue: { isClose: () => of(false), getAllBoolean: () => of([{ paramName: 'Show_Boutique', paramValue: true }, { paramName: 'Show_Chat', paramValue: true }]) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: TribuService, useValue: { getConnected: () => of({ adherents: [] }) } },
        { provide: PwaService, useValue: { canInstall: () => false, showIosInstallHint: () => false } },
        { provide: ToastService, useValue: { toasts: [] } },
        { provide: ChatService, useValue: { monitorUnread: jasmine.createSpy('monitorUnread'), totalUnread: signal(0) } }
      ]
    });
    router = TestBed.inject(Router);
    await router.navigateByUrl('/reporting');
    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('garde la navigation visible après un changement de page', async () => {
    expect(fixture.nativeElement.querySelector('.navbar-page-title')).toBeNull();
    expect(fixture.nativeElement.querySelector('.navbar-toggler')).toBeNull();
    expect(fixture.nativeElement.querySelector('#navbarNavigation')).not.toBeNull();
    await router.navigateByUrl('/seances');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.navbar-page-title')).toBeNull();
    expect(fixture.nativeElement.querySelector('#navbarNavigation')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.navbar-backdrop')).toBeNull();
  });

  it('affiche le sélecteur de tribu à côté de la navigation', () => {
    const active = { id: 1, prenom: 'Paul', nom: 'Martin', genre: 'Masculin', mineur: false, user: { id: 10 } } as any;
    const child = { id: 2, prenom: 'Alice', nom: 'Martin', genre: 'Féminin', mineur: true, user: undefined } as any;
    spyOn(TestBed.inject(TribuService), 'getConnected').and.returnValue(of({ adherents: [active, child] } as any));
    storage.getUser.and.returnValue({ id: 10, username: 'paul@example.org', roles: ['ROLE_USER'] });
    fixture.componentInstance.loadTribeMembers();
    fixture.detectChanges();
    const toggle = fixture.nativeElement.querySelector('#member-selector-toggle');
    expect(toggle.closest('#navbarNavigation')).toBeNull();
    expect(toggle.querySelector('img').getAttribute('src')).toBe('assets/homme.png');
    toggle.click();
    fixture.detectChanges();
    const options = fixture.nativeElement.querySelectorAll('.member-option');
    expect(options.length).toBe(2);
    expect(options[0].getAttribute('aria-current')).toBe('true');
    expect(options[1].disabled).toBeTrue();
    expect(options[1].textContent).toContain('Sans compte de connexion');
  });

  it('affiche une alerte Chat seulement en présence de messages non lus', () => {
    const unread = TestBed.inject(ChatService).totalUnread as ReturnType<typeof signal<number>>;
    expect(fixture.nativeElement.querySelector('.chat-alert')).toBeNull();
    unread.set(3);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chat-alert').getAttribute('aria-label')).toBe('3 messages non lus');
    unread.set(0);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chat-alert')).toBeNull();
  });

  it('propose une icône et un libellé accessible pour chaque lien du menu', () => {
    fixture.componentInstance.showSecretaire = true;
    fixture.componentInstance.showShopManager = true;
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('.navbar-nav .nav-link');
    expect(links.length).toBeGreaterThan(4);
    links.forEach(link => {
      expect(link.querySelector('fa-icon')).not.toBeNull();
      expect(link.querySelector('.nav-label')?.textContent?.trim()).toBeTruthy();
      expect(link.getAttribute('aria-label')).toBeTruthy();
      expect(link.getAttribute('title')).toBeTruthy();
    });
    fixture.nativeElement.querySelector('nav').dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#navbarNavigation')).not.toBeNull();
  });

  it('permet de revenir à la connexion depuis la récupération du mot de passe', async () => {
    await router.navigateByUrl('/login?sessionExpiree=1');
    TestBed.inject(LoginPageService).view.set('recuperation');
    fixture.detectChanges();
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('.navbar-login-actions button');
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent).toContain('Connexion');
    expect(fixture.nativeElement.querySelector('.navbar-toggler')).toBeNull();
    buttons[0].click();
    expect(TestBed.inject(LoginPageService).view()).toBe('connexion');
  });

  it('efface les droits du bandeau lorsque la session expire', async () => {
    storage.getToken.and.returnValue(null);
    await router.navigateByUrl('/login?sessionExpiree=1');
    fixture.detectChanges();
    expect(fixture.componentInstance.isLoggedIn).toBeFalse();
    expect(fixture.componentInstance.showAdmin).toBeFalse();
    expect(fixture.componentInstance.showSeances).toBeFalse();
    expect(fixture.componentInstance.username).toBeUndefined();
  });

  it('masque le lien vers la boutique lorsque son paramètre est désactivé', () => {
    expect(fixture.nativeElement.querySelector('app-shop-cart-link')).not.toBeNull();
    fixture.componentInstance.showShop = false;
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-shop-cart-link')).toBeNull();
  });

  it('masque le lien Chat lorsque son paramètre est désactivé', () => {
    expect(fixture.nativeElement.querySelector('[routerLink="chat"]')).not.toBeNull();
    fixture.componentInstance.showChat = false;
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[routerLink="chat"]')).toBeNull();
  });
});

describe('Déconnexion depuis une page protégée', () => {
  let component: AppComponent;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let logout: jasmine.Spy;
  let navigate: jasmine.Spy;
  let revocation: Subject<unknown>;
  let sessionChanges: Subject<StorageEvent>;

  beforeEach(() => {
    revocation = new Subject();
    sessionChanges = new Subject();
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'getUser', 'signOut'], {
      sessionChanges$: sessionChanges
    });
    storage.getToken.and.returnValue('access-token');
    storage.getUser.and.returnValue({ username: 'admin@example.org', roles: ['ROLE_ADMIN', 'ROLE_ENCADRANT'] });
    storage.signOut.and.callFake(() => storage.getToken.and.returnValue(null));
    logout = jasmine.createSpy('logout').and.callFake(() => {
      storage.signOut();
      return revocation;
    });
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: TokenStorageService, useValue: storage },
        { provide: AuthService, useValue: { logout } },
        { provide: ParamService, useValue: { isClose: () => of(false), getAllBoolean: () => of([]) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: TribuService, useValue: { getConnected: () => of({ adherents: [] }) } },
        { provide: PwaService, useValue: {} },
        { provide: ToastService, useValue: {} },
        { provide: ChatService, useValue: { monitorUnread: jasmine.createSpy('monitorUnread'), totalUnread: signal(0) } }
      ]
    });
    TestBed.overrideComponent(AppComponent, { set: { template: '', imports: [] } });
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.isLoggedIn).toBeTrue();
    expect(component.showAdmin).toBeTrue();
  });

  it('affiche la connexion sans attendre le serveur et efface le menu connecté', () => {
    component.logout();
    expect(logout).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login'], { replaceUrl: true });
    expect(component.isLoggedIn).toBeFalse();
    expect(component.showAdmin).toBeFalse();
    expect(component.showProf).toBeFalse();
    expect(component.showSeances).toBeFalse();
    expect(component.username).toBeUndefined();
    revocation.next(null);
    revocation.complete();
    expect(navigate).toHaveBeenCalledTimes(1);
  });

  it('revient à la connexion après une déconnexion dans un autre onglet', () => {
    storage.signOut();
    sessionChanges.next({} as StorageEvent);
    expect(navigate).toHaveBeenCalledWith(['/login'], { replaceUrl: true });
    expect(component.isLoggedIn).toBeFalse();
    expect(component.showAdmin).toBeFalse();
  });
});

describe('Changement d’adhérent', () => {
  let component: AppComponent;
  let switchRequest: Subject<unknown>;
  let auth: { switchMember: jasmine.Spy };
  let toast: { error: jasmine.Spy };
  const member = { id: 2, prenom: 'Alice', nom: 'Martin', genre: 'Féminin', mineur: true, user: { id: 20 } } as any;

  beforeEach(() => {
    switchRequest = new Subject();
    auth = { switchMember: jasmine.createSpy().and.returnValue(switchRequest) };
    toast = { error: jasmine.createSpy() };
    TestBed.configureTestingModule({ providers: [
      provideRouter([]),
      { provide: TokenStorageService, useValue: { getToken: () => 'jwt', getUser: () => ({ id: 10 }), sessionChanges$: new Subject() } },
      { provide: AuthService, useValue: auth },
      { provide: TribuService, useValue: { getConnected: () => of({ adherents: [member] }) } },
      { provide: ParamService, useValue: { isClose: () => of(false), getAllBoolean: () => of([]) } },
      { provide: ParamTransmissionService, useValue: {} },
      { provide: PwaService, useValue: {} },
      { provide: ToastService, useValue: toast },
      { provide: ChatService, useValue: { monitorUnread: () => {} } }
    ] });
    TestBed.overrideComponent(AppComponent, { set: { template: '', imports: [] } });
    component = TestBed.createComponent(AppComponent).componentInstance;
  });

  it('choisit les quatre avatars selon le genre et le statut mineur', () => {
    expect(component.memberAvatar(member)).toBe('assets/fille.png');
    expect(component.memberAvatar({ ...member, mineur: false })).toBe('assets/femme.png');
    expect(component.memberAvatar({ ...member, genre: 'Masculin' })).toBe('assets/garcon.png');
    expect(component.memberAvatar({ ...member, genre: 'Masculin', mineur: false })).toBe('assets/homme.png');
  });

  it('attend la nouvelle session et empêche les doubles clics avant le rechargement', () => {
    const reload = spyOn(component, 'reloadMemberSession');
    component.switchMember(member);
    component.switchMember(member);
    expect(auth.switchMember).toHaveBeenCalledOnceWith(2);
    expect(reload).not.toHaveBeenCalled();
    switchRequest.next({ token: 'new-jwt' });
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('conserve la page et permet de réessayer après une erreur', () => {
    const reload = spyOn(component, 'reloadMemberSession');
    component.switchMember(member);
    switchRequest.error(new Error('refus'));
    expect(component.switchingMember()).toBeFalse();
    expect(reload).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalled();
  });

  it('ne change pas vers le compte actif ou un adhérent sans compte', () => {
    component.activeMember.set(member);
    component.switchMember(member);
    component.switchMember({ ...member, id: 3, user: undefined });
    expect(auth.switchMember).not.toHaveBeenCalled();
  });

  it('actualise le sélecteur pour proposer un adhérent créé depuis la page de profil', () => {
    component.isLoggedIn = true;
    const load = spyOn(TestBed.inject(TribuService), 'getConnected');
    load.and.returnValue(of({ adherents: [member] } as any));
    component.loadTribeMembers();
    const newMember = { ...member, id: 3, user: { id: 30, username: 'adherent-123@sans-email.invalid' } };
    load.and.returnValue(of({ adherents: [member, newMember] } as any));
    component.loadTribeMembers();
    expect(component.tribeMembers().length).toBe(1);
    component.loadTribeMembers(true);
    expect(component.tribeMembers()).toContain(newMember);
  });
});
