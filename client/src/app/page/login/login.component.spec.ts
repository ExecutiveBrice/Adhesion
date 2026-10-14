import { TestBed } from '@angular/core/testing';
import { convertToParamMap, ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { LoginComponent } from './login.component';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamTransmissionService } from '../../_services/transmission.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { ActiviteService } from '../../_services/activite.service';
import { SessionNavigationService } from '../../_services/session-navigation.service';
import { LoginComponent } from './login.component';
import { LoginPageService } from '../../_services/login-page.service';

describe('Expiration sur la page de connexion déjà ouverte', () => {
  it('affiche le message et efface l’état connecté sans recréer la page', () => {
    const params = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap: params } },
        { provide: Router, useValue: {} },
        { provide: AuthService, useValue: {} },
        { provide: TokenStorageService, useValue: {} },
        { provide: ParamTransmissionService, useValue: {} },
        { provide: ParamService, useValue: {} },
        { provide: ToastService, useValue: {} },
        { provide: ActiviteService, useValue: {} }
      ]
    });
    TestBed.overrideComponent(LoginComponent, { set: { template: '' } });
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    component.isLoggedIn = true;
    component.roles = ['ROLE_ADMIN'];
    const page = TestBed.inject(LoginPageService);
    page.view.set('inscription');
    params.next(convertToParamMap({ sessionExpiree: '1' }));
    expect(component.sessionExpiree).toBeTrue();
    expect(component.isLoggedIn).toBeFalse();
    expect(component.roles).toEqual([]);
    expect(page.view()).toBe('connexion');
    fixture.destroy();
describe('Accueil après connexion', () => {
  let component: LoginComponent;
  let session: Subject<unknown>;
  let reloadAccueil: jasmine.Spy;

  beforeEach(() => {
    session = new Subject();
    reloadAccueil = jasmine.createSpy('reloadAccueil');
    TestBed.configureTestingModule({ providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { login: () => session } },
      { provide: TokenStorageService, useValue: { getUser: () => ({ roles: ['ROLE_ADMIN'] }) } },
      { provide: ParamTransmissionService, useValue: {} },
      { provide: ParamService, useValue: {} },
      { provide: ToastService, useValue: { warning: () => {} } },
      { provide: ActiviteService, useValue: {} },
      { provide: SessionNavigationService, useValue: { reloadAccueil } }
    ] });
    TestBed.overrideComponent(LoginComponent, { set: { template: '', imports: [] } });
    component = TestBed.createComponent(LoginComponent).componentInstance;
  });

  it('attend la connexion réussie avant de recharger l’accueil', () => {
    component.onSubmit();
    expect(reloadAccueil).not.toHaveBeenCalled();
    session.next({});
    expect(reloadAccueil).toHaveBeenCalledTimes(1);
  });

  it('reste sur la connexion en cas d’échec', () => {
    component.onSubmit();
    session.error({ error: { message: 'Refus' } });
    expect(reloadAccueil).not.toHaveBeenCalled();
  });

});
