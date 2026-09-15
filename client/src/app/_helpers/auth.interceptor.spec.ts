import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptorProviders } from './auth.interceptor';
import { PwaService } from '../_services/pwa.service';
import { TokenStorageService } from '../_services/token-storage.service';
import { environment } from '../../environments/environment';

describe('Renouvellement des requêtes PWA', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let router: jasmine.SpyObj<Router>;
  const api = environment.server;
  const session = { token: 'new-access', refreshToken: 'new-secret', roles: ['ROLE_USER'] };

  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', [
      'getToken', 'getRefreshToken', 'isTokenExpired', 'saveSession', 'signOut'
    ]);
    storage.getToken.and.returnValue('old-access');
    storage.getRefreshToken.and.returnValue('remembered-secret');
    storage.isTokenExpired.and.returnValue(false);
    storage.saveSession.and.callFake(value => {
      storage.getToken.and.returnValue(value.token);
      storage.getRefreshToken.and.returnValue(value.refreshToken);
      storage.isTokenExpired.and.returnValue(false);
    });
    storage.signOut.and.callFake(() => {
      storage.getToken.and.returnValue(null);
      storage.getRefreshToken.and.returnValue(null);
    });
    router = jasmine.createSpyObj('Router', ['navigate']);
    router.navigate.and.resolveTo(true);
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting(), authInterceptorProviders,
      { provide: TokenStorageService, useValue: storage },
      { provide: PwaService, useValue: { isMobileStandalone: () => true } },
      { provide: Router, useValue: router }
    ] });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renouvelle avant la requête après expiration en arrière-plan', () => {
    storage.isTokenExpired.and.returnValue(true);
    client.get(api + '/user/profile').subscribe();
    http.expectNone(api + '/user/profile');
    http.expectOne(api + '/auth/refresh').flush(session);
    const request = http.expectOne(api + '/user/profile');
    expect(request.request.headers.get('Authorization')).toBe('Bearer new-access');
    request.flush({});
  });

  it('un seul renouvellement reprend deux requêtes rejetées simultanément', () => {
    client.get(api + '/user/first').subscribe();
    client.get(api + '/user/second').subscribe();
    http.expectOne(api + '/user/first').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(api + '/user/second').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(api + '/auth/refresh').flush(session);
    for (const path of ['first', 'second']) {
      const retry = http.expectOne(api + '/user/' + path);
      expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access');
      retry.flush({});
    }
    expect(storage.signOut).not.toHaveBeenCalled();
  });

  it('ne déconnecte pas pour une erreur de validation après renouvellement', () => {
    storage.isTokenExpired.and.returnValue(true);
    client.post(api + '/user/profile', {}).subscribe({ error: () => {} });
    http.expectOne(api + '/auth/refresh').flush(session);
    http.expectOne(api + '/user/profile').flush({}, { status: 400, statusText: 'Bad Request' });
    expect(storage.signOut).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('revient à la connexion si le jeton mémorisé est révoqué', () => {
    storage.isTokenExpired.and.returnValue(true);
    client.get(api + '/user/profile').subscribe({ error: () => {} });
    http.expectOne(api + '/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectNone(api + '/user/profile');
    expect(storage.signOut).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { sessionExpiree: '1' } });
  });

  it('un mauvais mot de passe ne déclenche pas de renouvellement', () => {
    client.post(api + '/auth/signin', {}).subscribe({ error: () => {} });
    http.expectOne(api + '/auth/signin').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectNone(api + '/auth/refresh');
    expect(storage.signOut).not.toHaveBeenCalled();
  });

  it('ne transmet pas le jeton à un service externe', () => {
    client.get('https://example.org/calendar').subscribe();
    const request = http.expectOne('https://example.org/calendar');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });
});
