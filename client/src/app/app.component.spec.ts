import { Component } from '@angular/core';
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
        { provide: ParamService, useValue: { isClose: () => of(false) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: PwaService, useValue: { canInstall: () => false, showIosInstallHint: () => false } },
        { provide: ToastService, useValue: { toasts: [] } }
      ]
    });
    router = TestBed.inject(Router);
    await router.navigateByUrl('/reporting');
    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('affiche le titre de la route et ferme le menu à la navigation', async () => {
    expect(fixture.nativeElement.querySelector('.navbar-page-title').textContent).toBe('Reporting');
    fixture.nativeElement.querySelector('.navbar-toggler').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.isCollapsed).toBeFalse();
    await router.navigateByUrl('/seances');
    fixture.detectChanges();
    expect(fixture.componentInstance.isCollapsed).toBeTrue();
    expect(fixture.nativeElement.querySelector('.navbar-page-title').textContent).toBe('Séances');
    expect(fixture.nativeElement.querySelector('.navbar-backdrop')).toBeNull();
  });

  it('ferme le menu avec Échap et rend le focus au bouton Menu', () => {
    const toggle: HTMLButtonElement = fixture.nativeElement.querySelector('.navbar-toggler');
    const focus = spyOn(toggle, 'focus');
    toggle.click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('nav').dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.isCollapsed).toBeTrue();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(focus).toHaveBeenCalled();
  });

  it('ferme le menu en cliquant sur le fond', () => {
    fixture.nativeElement.querySelector('.navbar-toggler').click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.navbar-backdrop').click();
    fixture.detectChanges();
    expect(fixture.componentInstance.isCollapsed).toBeTrue();
    expect(fixture.nativeElement.querySelector('.navbar-backdrop')).toBeNull();
  });

  it('partage les boutons connexion et inscription avec la page de connexion', async () => {
    await router.navigateByUrl('/login?sessionExpiree=1');
    fixture.detectChanges();
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('.navbar-login-actions button');
    expect(buttons.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.navbar-toggler')).toBeNull();
    buttons[1].click();
    fixture.detectChanges();
    expect(TestBed.inject(LoginPageService).view()).toBe('inscription');
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');
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
        { provide: ParamService, useValue: { isClose: () => of(false) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: PwaService, useValue: {} },
        { provide: ToastService, useValue: {} }
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
    component.isCollapsed = false;
    component.logout();
    expect(logout).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/login'], { replaceUrl: true });
    expect(component.isLoggedIn).toBeFalse();
    expect(component.showAdmin).toBeFalse();
    expect(component.showProf).toBeFalse();
    expect(component.showSeances).toBeFalse();
    expect(component.username).toBeUndefined();
    expect(component.isCollapsed).toBeTrue();
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
