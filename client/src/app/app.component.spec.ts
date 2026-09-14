import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { AppComponent } from './app.component';
import { AuthService } from './_services/auth.service';
import { ParamService } from './_services/param.service';
import { ParamTransmissionService } from './_services/transmission.service';
import { PwaService } from './_services/pwa.service';
import { ToastService } from './_services/toast.service';
import { TokenStorageService } from './_services/token-storage.service';

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
    storage.getUser.and.returnValue({ username: 'admin@example.org', roles: ['ROLE_ADMIN', 'ROLE_PROF'] });
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
