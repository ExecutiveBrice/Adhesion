import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { routes } from '../../app.routes';
import { Adherent, ERole, Tribu, User } from '../../models';
import { ApiRenderService } from '../../_services/api-render.service';
import { AdherentService } from '../../_services/adherent.service';
import { ActiviteService } from '../../_services/activite.service';
import { AdhesionService } from '../../_services/adhesion.service';
import { FileService } from '../../_services/file.service';
import { ParamService } from '../../_services/param.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ToastService } from '../../_services/toast.service';
import { TribuService } from '../../_services/tribu.service';
import { UserService } from '../../_services/user.service';
import { UtilService } from '../../_services/util.service';
import { UserComponent } from '../../template/user/user.component';
import { ProfilComponent } from './profil.component';

describe('Actions administrateur sur un profil consulté', () => {
  let actorRoles: string[];
  let target: Adherent;

  beforeEach(() => {
    actorRoles = ['ROLE_USER', 'ROLE_ADMIN'];
    target = Object.assign(new Adherent(), {
      id: 22, prenom: 'Autre', nom: 'Adhérent', mineur: false, completAdhesion: true, activitesNm1: [],
      user: Object.assign(new User(), { id: 2, username: 'autre@example.org', roles: [ERole.ROLE_USER] })
    });
    const child = Object.assign(new Adherent(), {
      id: 23, prenom: 'Enfant', nom: 'Adhérent', mineur: true, completAdhesion: true, activitesNm1: []
    });
    const tribe = Object.assign(new Tribu(), { uuid: 'autre-tribu', adherents: [target, child] });
    TestBed.configureTestingModule({ providers: [
      provideRouter(routes.filter(route => ['profil/:tribuUuid', 'inscription/:tribuUuid'].includes(route.path ?? ''))),
      { provide: TokenStorageService, useValue: { getToken: () => 'jwt-admin', isTokenExpired: () => false, getUser: () => ({ id: 1, roles: actorRoles }) } },
      { provide: TribuService, useValue: { getTribuByUuid: () => of(tribe), getConnected: () => of(tribe) } },
      { provide: ParamService, useValue: { getAllBoolean: () => of([{ paramName: 'Inscription', paramValue: true }, { paramName: 'Ouvert', paramValue: true }]) } },
      { provide: AdherentService, useValue: {} },
      { provide: ActiviteService, useValue: { fillObjects: () => {} } },
      { provide: AdhesionService, useValue: {} },
      { provide: FileService, useValue: { getAllFilesName: () => of([]) } },
      { provide: UserService, useValue: {} },
      { provide: ToastService, useValue: { success: () => {} } },
      { provide: UtilService, useValue: {} },
      { provide: NgbModal, useValue: {} }
    ] });
  });

  it('affiche les rôles et autorise la modification de l’e-mail avec les droits de l’administrateur', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/inscription/autre-tribu?adherentId=22', ProfilComponent);
    const form = harness.routeDebugElement!.query(By.directive(UserComponent)).componentInstance as UserComponent;
    expect(form.showAdmin).toBeTrue();
    expect(form.canChangeRole(ERole.ROLE_ADMIN)).toBeTrue();
    expect(form.hasRole(ERole.ROLE_ADMIN)).toBeFalse();
    const roles = harness.routeNativeElement!.querySelector('.adherent-role-checkboxes');
    expect(roles).not.toBeNull();
    expect((roles!.querySelector('input') as HTMLInputElement).disabled).toBeFalse();
    expect((harness.routeNativeElement!.querySelector('input[name="email"]') as HTMLInputElement).readOnly).toBeFalse();
  });

  it('ne répète pas le nom de l’adhérent dans un second titre', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.querySelector('h1')!.textContent).toContain('Autre Adhérent');
    expect(harness.routeNativeElement!.querySelector('.modal-title')).toBeNull();
  });

  it('permet au secrétaire de consulter tous les membres, y compris sans compte, en conservant ses droits', async () => {
    actorRoles = ['ROLE_USER', 'ROLE_SECRETAIRE'];
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    const initialForm = harness.routeDebugElement!.query(By.directive(UserComponent)).componentInstance;
    const links = harness.routeNativeElement!.querySelectorAll('nav[aria-label="Membres de la tribu"] a');
    expect(links.length).toBe(2);
    expect(links[0].getAttribute('aria-current')).toBe('page');
    const childUrl = links[1].getAttribute('href')!;
    expect(childUrl).toContain('adherentId=23');
    const profile = await harness.navigateByUrl(childUrl, ProfilComponent);
    expect(profile.adherent?.id).toBe(23);
    const childForm = harness.routeDebugElement!.query(By.directive(UserComponent)).componentInstance as UserComponent;
    expect(childForm).not.toBe(initialForm);
    expect(childForm.showSecretaire).toBeTrue();
    expect(childForm.canManageRoles).toBeTrue();
    expect(TestBed.inject(TokenStorageService).getUser().id).toBe(1);
    expect(harness.routeNativeElement!.querySelectorAll('nav[aria-label="Membres de la tribu"] a')[1].getAttribute('aria-current')).toBe('page');
  });

  it('n’affiche pas la navigation secrétariat selon les rôles du profil consulté', async () => {
    actorRoles = ['ROLE_USER'];
    target.user.roles = [ERole.ROLE_SECRETAIRE];
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.querySelector('nav[aria-label="Membres de la tribu"]')).toBeNull();
  });

  it('permet de gérer les rôles avec le nom de connexion même si l’identifiant du compte n’est pas fourni', async () => {
    target.user.id = undefined as any;
    const grant = jasmine.createSpy().and.returnValue(of({ ...target.user, roles: [ERole.ROLE_USER, ERole.ROLE_ADMIN] }));
    TestBed.overrideProvider(UserService, { useValue: { grantUser: grant } });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    const role = harness.routeNativeElement!.querySelector('.adherent-role-checkboxes input') as HTMLInputElement;
    expect(role).not.toBeNull();
    if (!role) return;
    expect(role.disabled).toBeFalse();
    role.click();
    expect(grant).toHaveBeenCalledWith(ERole.ROLE_ADMIN, 'autre@example.org');
  });

  it('affiche une explication si l’adhérent ne possède pas de compte pour attribuer des rôles', async () => {
    target.user.username = '';
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.textContent).toContain('Attribution des rôles');
    expect(harness.routeNativeElement!.textContent).toContain('Aucun compte de connexion');
    expect(harness.routeNativeElement!.querySelector('.adherent-role-checkboxes input')).toBeNull();
  });

  it('actualise les actions lorsque les droits de la session sont actualisés après le chargement du profil', async () => {
    actorRoles = ['ROLE_USER'];
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.querySelector('.adherent-role-checkboxes')).toBeNull();
    expect((harness.routeNativeElement!.querySelector('input[name="email"]') as HTMLInputElement).readOnly).toBeTrue();
    actorRoles = ['ROLE_USER', 'ROLE_ADMIN'];
    TestBed.inject(ApiRenderService).notify();
    expect(harness.routeNativeElement!.querySelector('.adherent-role-checkboxes')).not.toBeNull();
    expect((harness.routeNativeElement!.querySelector('input[name="email"]') as HTMLInputElement).readOnly).toBeFalse();
    actorRoles = ['ROLE_USER'];
    TestBed.inject(ApiRenderService).notify();
    expect(harness.routeNativeElement!.querySelector('.adherent-role-checkboxes')).toBeNull();
    expect((harness.routeNativeElement!.querySelector('input[name="email"]') as HTMLInputElement).readOnly).toBeTrue();
  });

  it('affiche la régénération des documents pour l’administrateur', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.querySelector('[data-action="regenerate-documents"]')).not.toBeNull();
  });

  it('n’accorde pas les droits du profil consulté à un membre ordinaire', async () => {
    actorRoles = ['ROLE_USER'];
    target.user.roles = [ERole.ROLE_ADMIN];
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profil/autre-tribu?adherentId=22', ProfilComponent);
    expect(harness.routeNativeElement!.querySelector('.adherent-role-checkboxes')).toBeNull();
    expect((harness.routeNativeElement!.querySelector('input[name="email"]') as HTMLInputElement).readOnly).toBeTrue();
    expect(harness.routeNativeElement!.querySelector('[data-action="regenerate-documents"]')).toBeNull();
  });
});
