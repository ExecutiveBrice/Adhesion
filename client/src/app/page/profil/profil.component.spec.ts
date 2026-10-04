import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../../app.routes';
import { AuthService } from '../../_services/auth.service';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { TribuService } from '../../_services/tribu.service';
import { ProfilComponent } from './profil.component';

@Component({ template: '' })
class EmptyPage {}

describe('Navigation vers le profil adhérent', () => {
  let tribuService: { getConnected: jasmine.Spy; getTribuByUuid: jasmine.Spy };
  let user: { id: number; roles: string[] };
  let storage: jasmine.SpyObj<TokenStorageService>;
  let auth: jasmine.SpyObj<AuthService>;
  const connected = { uuid: 'ma-tribu', adherents: [
    { id: 11, user: { id: 1 }, prenom: 'Parent' },
    { id: 12, prenom: 'Enfant' }
  ] };
  const other = { uuid: 'autre-tribu', adherents: [
    { id: 21, prenom: 'Premier' }, { id: 22, prenom: 'Sélectionné', user: { id: 2, roles: ['ROLE_USER'] } }
  ] };

  beforeEach(() => {
    user = { id: 1, roles: ['ROLE_USER'] };
    storage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', [
      'getToken', 'getUser', 'isTokenExpired', 'saveToken', 'saveUser', 'saveSession', 'signOut'
    ]);
    storage.getToken.and.returnValue('jwt-origine');
    storage.isTokenExpired.and.returnValue(false);
    storage.getUser.and.callFake(() => user);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['switchMember', 'impersonate', 'login']);
    tribuService = {
      getConnected: jasmine.createSpy().and.returnValue(of(connected)),
      getTribuByUuid: jasmine.createSpy().and.returnValue(of(other))
    };
    TestBed.configureTestingModule({ providers: [
      provideRouter(routes.filter(route => ['profil', 'profil/nouveau', 'profil/:tribuUuid', 'inscription', 'inscription/:tribuUuid'].includes(route.path ?? '')).concat({ path: 'login', component: EmptyPage })),
      { provide: TokenStorageService, useValue: storage },
      { provide: AuthService, useValue: auth },
      { provide: TribuService, useValue: tribuService },
      { provide: ParamService, useValue: {} }
    ] });
    TestBed.overrideComponent(ProfilComponent, { set: { template: '', imports: [] } });
  });

  it('redirige l’ancienne page vers le profil du membre actif', async () => {
    const harness = await RouterTestingHarness.create();
    const profile = await harness.navigateByUrl('/inscription/', ProfilComponent);
    expect(TestBed.inject(Router).url).toMatch(/^\/profil\/?$/);
    expect(profile.adherent?.id).toBe(11);
  });

  it('ouvre précisément l’adhérent sélectionné dans une autre tribu', async () => {
    user.roles = ['ROLE_ADMIN'];
    const harness = await RouterTestingHarness.create();
    const profile = await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(tribuService.getTribuByUuid).toHaveBeenCalledWith('autre-tribu');
    expect(profile.adherent?.id).toBe(22);
    expect(tribuService.getConnected).not.toHaveBeenCalled();
  });

  it('conserve la tribu et l’adhérent des anciennes URL', async () => {
    user.roles = ['ROLE_SECRETAIRE'];
    const harness = await RouterTestingHarness.create();
    const profile = await harness.navigateByUrl('/inscription/autre-tribu?adherentId=22', ProfilComponent);
    expect(TestBed.inject(Router).url).toBe('/profil/autre-tribu?adherentId=22');
    expect(profile.adherent?.id).toBe(22);
  });

  for (const role of ['ROLE_ADMIN', 'ROLE_SECRETAIRE']) {
    it('conserve la session et les droits ' + role + ' lors de la redirection vers un autre adhérent', async () => {
      user.roles = ['ROLE_USER', role];
      const originalUser = { ...user, roles: [...user.roles] };
      const harness = await RouterTestingHarness.create();
      const profile = await harness.navigateByUrl('/inscription/autre-tribu?adherentId=22', ProfilComponent);
      expect(profile.adherent?.user?.id).toBe(2);
      expect(storage.getUser()).toEqual(originalUser);
      expect(storage.getToken()).toBe('jwt-origine');
      expect(storage.saveToken).not.toHaveBeenCalled();
      expect(storage.saveUser).not.toHaveBeenCalled();
      expect(storage.saveSession).not.toHaveBeenCalled();
      expect(storage.signOut).not.toHaveBeenCalled();
      expect(auth.switchMember).not.toHaveBeenCalled();
      expect(auth.impersonate).not.toHaveBeenCalled();
      expect(auth.login).not.toHaveBeenCalled();
      await harness.navigateByUrl('/profil', ProfilComponent);
      expect(storage.getUser()).toEqual(originalUser);
    });
  }

  it('permet de consulter un enfant sans compte et de changer de sélection', async () => {
    const harness = await RouterTestingHarness.create();
    let profile = await harness.navigateByUrl('/profil/ma-tribu?adherentId=12', ProfilComponent);
    expect(profile.adherent?.id).toBe(12);
    profile = await harness.navigateByUrl('/profil/ma-tribu?adherentId=11', ProfilComponent);
    expect(profile.adherent?.id).toBe(11);
  });

  it('ne remplace pas un adhérent introuvable par un autre profil', async () => {
    const harness = await RouterTestingHarness.create();
    const profile = await harness.navigateByUrl('/profil/ma-tribu?adherentId=99', ProfilComponent);
    expect(profile.adherent).toBeUndefined();
    expect(profile.erreur).toBeTruthy();
  });

  it('refuse une autre tribu pour un membre sans droits de secrétariat', async () => {
    const harness = await RouterTestingHarness.create();
    const profile = await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(profile.adherent).toBeUndefined();
    expect(profile.erreur).toBeTruthy();
    expect(tribuService.getTribuByUuid).not.toHaveBeenCalled();
  });
});

