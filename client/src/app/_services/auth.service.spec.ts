import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { PwaService } from './pwa.service';
import { TokenStorageService } from './token-storage.service';
import { environment } from '../../environments/environment';

describe('Reconnexion PWA', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let pwa: jasmine.SpyObj<PwaService>;
  const api = environment.server + '/auth/';
  const session = { token: 'new-access-token', refreshToken: 'new-secret', roles: ['ROLE_USER'] };

  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', [
      'getRefreshToken', 'isTokenExpired', 'saveSession', 'signOut'
    ]);
    storage.getRefreshToken.and.returnValue('remembered-secret');
    storage.isTokenExpired.and.returnValue(true);
    storage.signOut.and.callFake(() => storage.getRefreshToken.and.returnValue(null));
    pwa = jasmine.createSpyObj('PwaService', ['isMobileStandalone']);
    pwa.isMobileStandalone.and.returnValue(true);
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(), AuthService,
      { provide: TokenStorageService, useValue: storage },
      { provide: PwaService, useValue: pwa }
    ] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('demande une session persistante uniquement en PWA mobile', () => {
    auth.login('member@example.org', 'password').subscribe();
    const first = http.expectOne(api + 'signin');
    expect(first.request.body.rememberSession).toBeTrue();
    first.flush(session);
    expect(storage.saveSession).toHaveBeenCalledWith(session);
    pwa.isMobileStandalone.and.returnValue(false);
    auth.login('member@example.org', 'password').subscribe();
    const second = http.expectOne(api + 'signin');
    expect(second.request.body.rememberSession).toBeFalse();
    second.flush({ token: 'browser-token' });
  });

  it('renouvelle une session expirée avant le démarrage sans transmettre de mot de passe', async () => {
    const restored = auth.restoreSession();
    const request = http.expectOne(api + 'refresh');
    expect(request.request.body).toEqual({ refreshToken: 'remembered-secret' });
    request.flush(session);
    await restored;
    expect(storage.saveSession).toHaveBeenCalledWith(session);
  });

  it('conserve une session encore valide et ne renouvelle pas dans le navigateur', async () => {
    storage.isTokenExpired.and.returnValue(false);
    await auth.restoreSession();
    storage.isTokenExpired.and.returnValue(true);
    pwa.isMobileStandalone.and.returnValue(false);
    await auth.restoreSession();
    http.expectNone(api + 'refresh');
  });

  it('partage le renouvellement entre plusieurs appels simultanés', () => {
    const results: unknown[] = [];
    auth.refreshSession().subscribe(value => results.push(value));
    auth.refreshSession().subscribe(value => results.push(value));
    http.expectOne(api + 'refresh').flush(session);
    expect(results).toEqual([session, session]);
    expect(storage.saveSession).toHaveBeenCalledTimes(1);
  });

  it('efface une session révoquée sans bloquer le démarrage', async () => {
    const restored = auth.restoreSession();
    http.expectOne(api + 'refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    await restored;
    expect(storage.signOut).toHaveBeenCalled();
  });

  it('conserve la session après une coupure réseau et permet une nouvelle tentative', async () => {
    const restored = auth.restoreSession();
    http.expectOne(api + 'refresh').error(new ProgressEvent('error'));
    await restored;
    expect(storage.signOut).not.toHaveBeenCalled();
    auth.refreshSession().subscribe();
    http.expectOne(api + 'refresh').flush(session);
    expect(storage.saveSession).toHaveBeenCalledWith(session);
  });

  it('une réponse tardive ne reconnecte pas après une déconnexion volontaire', () => {
    let rejected = false;
    auth.refreshSession().subscribe({ error: () => rejected = true });
    const pending = http.expectOne(api + 'refresh');
    auth.logout().subscribe();
    const logout = http.expectOne(api + 'signout');
    expect(logout.request.body).toEqual({ refreshToken: 'remembered-secret' });
    logout.flush(null);
    pending.flush(session);
    const revoke = http.expectOne(api + 'signout');
    expect(revoke.request.body).toEqual({ refreshToken: 'new-secret' });
    revoke.flush(null);
    expect(rejected).toBeTrue();
    expect(storage.saveSession).not.toHaveBeenCalled();
  });
});
