import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamBoolean } from '../../models/paramBoolean';
import { chatAuthGuard } from './chat-auth.guard';

describe('chatAuthGuard', () => {
  let storage: jasmine.SpyObj<TokenStorageService>;
  let params: jasmine.SpyObj<ParamService>;
  let router: Router;

  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'isTokenExpired']);
    params = jasmine.createSpyObj('ParamService', ['getAllBoolean']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: TokenStorageService, useValue: storage }, { provide: ParamService, useValue: params }]
    });
    router = TestBed.inject(Router);
    storage.getToken.and.returnValue('token');
    storage.isTokenExpired.and.returnValue(false);
  });

  it('bloque le chat quand il est désactivé', done => {
    params.getAllBoolean.and.returnValue(of([{ id: 1, paramName: 'Show_Chat', paramValue: false }] as ParamBoolean[]));

    const result = TestBed.runInInjectionContext(() => chatAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/chat' } as RouterStateSnapshot));

    (result as unknown as Observable<UrlTree>).subscribe(value => {
      expect(router.serializeUrl(value)).toBe('/inscription');
      done();
    });
  });
});
