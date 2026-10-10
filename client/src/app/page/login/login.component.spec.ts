import { TestBed } from '@angular/core/testing';
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

describe('Accueil après connexion', () => {
  let component: LoginComponent;
  let session: Subject<unknown>;
  let reloadAccueil: jasmine.Spy;
  let creation: Subject<unknown>;
  let register: jasmine.Spy;
  let login: jasmine.Spy;

  beforeEach(() => {
    session = new Subject();
    creation = new Subject();
    register = jasmine.createSpy('register').and.returnValue(creation);
    login = jasmine.createSpy('login').and.returnValue(session);
    reloadAccueil = jasmine.createSpy('reloadAccueil');
    TestBed.configureTestingModule({ providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { login, register } },
      { provide: TokenStorageService, useValue: { getUser: () => ({ roles: ['ROLE_ADMIN'] }) } },
      { provide: ParamTransmissionService, useValue: {} },
      { provide: ParamService, useValue: {} },
      { provide: ToastService, useValue: { warning: () => {} } },
      { provide: ActiviteService, useValue: {} },
      { provide: SessionNavigationService, useValue: { reloadAccueil } }
    ] });
    TestBed.overrideComponent(LoginComponent, { set: { template: '', imports: [] } });
    component = TestBed.createComponent(LoginComponent).componentInstance;
    component.form = { username: 'nouveau@example.test', password: 'secret1' };
    TestBed.inject(LoginPageService).inscriptionOpen.set(true);
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

  it('attend la création et la connexion sans accepter de double soumission', () => {
    component.onSubmitInscription();
    component.onSubmitInscription();
    expect(register).toHaveBeenCalledOnceWith('nouveau@example.test', 'secret1');
    expect(login).not.toHaveBeenCalled();
    creation.next({});
    creation.complete();
    expect(login).toHaveBeenCalledOnceWith('nouveau@example.test', 'secret1');
    expect(reloadAccueil).not.toHaveBeenCalled();
    session.next({});
    session.complete();
    expect(reloadAccueil).toHaveBeenCalledTimes(1);
    expect(component.inscriptionSubmitting).toBeFalse();
  });

  it('ne crée pas de compte lorsque l’inscription est fermée', () => {
    TestBed.inject(LoginPageService).inscriptionOpen.set(false);
    component.onSubmitInscription();
    expect(register).not.toHaveBeenCalled();
    expect(login).not.toHaveBeenCalled();
  });

  it('conserve le formulaire après un refus de création et permet de réessayer', () => {
    component.onSubmitInscription();
    creation.error({ error: { message: 'Adresse déjà utilisée' } });
    expect(component.inscriptionError).toBe('Adresse déjà utilisée');
    expect(component.inscriptionSubmitting).toBeFalse();
    expect(component.isSuccessful).toBeFalse();
    expect(component.form.username).toBe('nouveau@example.test');
    expect(login).not.toHaveBeenCalled();
    register.and.returnValue(new Subject());
    component.onSubmitInscription();
    expect(register).toHaveBeenCalledTimes(2);
  });

  it('invite à se connecter sans recréer le compte si la connexion après création échoue', () => {
    component.onSubmitInscription();
    creation.next({});
    session.error({ error: { message: 'Connexion indisponible' } });
    expect(component.inscriptionError).toContain('Votre compte a été créé');
    expect(component.inscriptionSubmitting).toBeFalse();
    expect(reloadAccueil).not.toHaveBeenCalled();
    component.onSubmitInscription();
    expect(register).toHaveBeenCalledTimes(1);
  });

});
