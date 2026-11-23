import { Component, signal } from '@angular/core';
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
import { ShopApiService } from './shop/services/shop-api.service';
import { ShopOrderDto } from './shop/models/shop.models';

@Component({ template: '' })
class HeaderTestPage {}

describe('Navigation du bandeau', () => {
  let fixture: ComponentFixture<AppComponent>;
  let router: Router;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let sessionChanges: Subject<StorageEvent>;
  let shopApi: jasmine.SpyObj<ShopApiService>;

  beforeEach(async () => {
    sessionChanges = new Subject();
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'getUser', 'isTokenExpired'], {
      sessionChanges$: sessionChanges
    });
    storage.getToken.and.returnValue('access-token');
    storage.isTokenExpired.and.returnValue(false);
    storage.getUser.and.returnValue({ username: 'admin@example.org', roles: ['ROLE_ADMIN', 'ROLE_ENCADRANT'] });
    shopApi = jasmine.createSpyObj('ShopApiService', ['orders']);
    shopApi.orders.and.returnValue(of([]));
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
        { provide: ParamService, useValue: { isClose: () => of(false), getAllBoolean: () => of([]), showBoutique: signal(true) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: PwaService, useValue: { canInstall: () => false, showIosInstallHint: () => false } },
        { provide: ToastService, useValue: { toasts: [] } },
        { provide: ShopApiService, useValue: shopApi }
      ]
    });
    router = TestBed.inject(Router);
    await router.navigateByUrl('/reporting');
    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('affiche directement les raccourcis avec leur picto et indique la page active', async () => {
    const nav: HTMLElement = fixture.nativeElement.querySelector('nav');
    const reporting: HTMLAnchorElement = nav.querySelector('a[href="/reporting"]')!;
    const seances: HTMLAnchorElement = nav.querySelector('a[href="/seances"]')!;
    expect(nav.querySelector('.navbar-toggler, .navbar-collapse, .navbar-backdrop')).toBeNull();
    expect(reporting.querySelector('fa-icon svg')).not.toBeNull();
    expect(reporting.getAttribute('aria-label')).toBe('Reporting');
    expect(reporting.getAttribute('aria-current')).toBe('page');
    expect(seances.querySelector('fa-icon svg')).not.toBeNull();
    expect(seances.getAttribute('aria-label')).toBe('Séances');

    await router.navigateByUrl('/seances');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(reporting.hasAttribute('aria-current')).toBeFalse();
    expect(seances.getAttribute('aria-current')).toBe('page');
  });

  it('masque Mes commandes sans historique et l’affiche après la première commande', async () => {
    expect(fixture.nativeElement.querySelector('a[href="/boutique/commandes"]')).toBeNull();

    shopApi.orders.and.returnValue(of([{ status: 'PENDING_PAYMENT' } as ShopOrderDto]));
    await router.navigateByUrl('/seances');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/boutique/commandes"] fa-icon')).not.toBeNull();

    storage.getToken.and.returnValue(null);
    await router.navigateByUrl('/login');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/boutique/commandes"]')).toBeNull();
  });

  it('partage les boutons connexion et inscription avec la page de connexion', async () => {
    await router.navigateByUrl('/login?sessionExpiree=1');
    fixture.detectChanges();
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('.navbar-login-actions button');
    expect(buttons.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.navbar-nav')).toBeNull();
    buttons[1].click();
    fixture.detectChanges();
    expect(TestBed.inject(LoginPageService).view()).toBe('inscription');
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');
    buttons[0].click();
    expect(TestBed.inject(LoginPageService).view()).toBe('connexion');
  });

  it('affiche la boutique et le panier à côté de la connexion sur l’accueil public', async () => {
    storage.getToken.and.returnValue(null);
    await router.navigateByUrl('/login');
    fixture.detectChanges();
    const actions: HTMLElement = fixture.nativeElement.querySelector('.navbar-login-actions');

    expect(actions.querySelector('a[href="/boutique"] fa-icon')).not.toBeNull();
    expect(actions.querySelector('a[href="/boutique/panier"] fa-icon')).not.toBeNull();
    expect(actions.querySelectorAll('button').length).toBe(2);
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

  it('affiche le lien Matériel pour la Logistique', async () => {
    storage.getUser.and.returnValue({ username: 'logistique@example.org', roles: ['ROLE_LOGISTIQUE'] });
    await router.navigateByUrl('/seances');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/materiel"]')).not.toBeNull();
    expect(fixture.componentInstance.showLogistique).toBeTrue();
    expect(fixture.componentInstance.showAdmin).toBeFalse();
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
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'getUser', 'signOut', 'isTokenExpired'], {
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
        { provide: ParamService, useValue: { isClose: () => of(false), getAllBoolean: () => of([]), showBoutique: signal(false) } },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: PwaService, useValue: {} },
        { provide: ShopApiService, useValue: { orders: () => of([]) } },
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

  it('affiche la connexion sans attendre le serveur et efface les droits de la session', () => {
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
