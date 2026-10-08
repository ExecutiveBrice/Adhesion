import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { ParamService } from '../../_services/param.service';
import { SectionManagementService } from '../../_services/section-management.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamBoolean } from '../../models/paramBoolean';
import { chatAuthGuard } from './chat-auth.guard';

describe('chatAuthGuard', () => {
  let storage: jasmine.SpyObj<TokenStorageService>;
  let params: jasmine.SpyObj<ParamService>;
  let sections: jasmine.SpyObj<SectionManagementService>;
  let router: Router;

  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'isTokenExpired', 'getUser']);
    params = jasmine.createSpyObj('ParamService', ['getAllBoolean']);
    sections = jasmine.createSpyObj('SectionManagementService', ['sections']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: TokenStorageService, useValue: storage },
        { provide: ParamService, useValue: params }, { provide: SectionManagementService, useValue: sections }]
    });
    router = TestBed.inject(Router);
    storage.getToken.and.returnValue('token');
    storage.isTokenExpired.and.returnValue(false);
    storage.getUser.and.returnValue({ roles: ['ROLE_USER'] });
    sections.sections.and.returnValue(of([]));
  });

  it('bloque le chat quand il est désactivé', done => {
    params.getAllBoolean.and.returnValue(of([{ id: 1, paramName: 'Show_Chat', paramValue: false }] as ParamBoolean[]));

    const result = TestBed.runInInjectionContext(() => chatAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/chat' } as RouterStateSnapshot));

    (result as unknown as Observable<UrlTree>).subscribe(value => {
      expect(router.serializeUrl(value)).toBe('/profil');
      done();
    });
  });

  it('laisse un administrateur gérer les chats lorsque le chat est désactivé', () => {
    storage.getUser.and.returnValue({ roles: ['ROLE_ADMIN'] });
    const result = TestBed.runInInjectionContext(() => chatAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/chat' } as RouterStateSnapshot));
    expect(result).toBe(true);
    expect(params.getAllBoolean).not.toHaveBeenCalled();
  });

  it('laisse un référent accéder au paramétrage lorsque le chat est désactivé', done => {
    params.getAllBoolean.and.returnValue(of([{ id: 1, paramName: 'Show_Chat', paramValue: false }] as ParamBoolean[]));
    sections.sections.and.returnValue(of([{ id: 4, nom: 'Basket', couleur: '#000000' }]));

    const result = TestBed.runInInjectionContext(() => chatAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/chat' } as RouterStateSnapshot));

    (result as unknown as Observable<boolean>).subscribe(value => {
      expect(value).toBe(true);
      done();
    });
  });
});
