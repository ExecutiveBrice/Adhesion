import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { UserComponent } from './user.component';
import { Accord, Adherent, Adhesion, ParamText, Tribu, User } from '../../models';
import { MailService } from '../../_services/mail.service';
import { AdherentService } from '../../_services/adherent.service';
import { UserService } from '../../_services/user.service';
import { AdhesionService } from '../../_services/adhesion.service';
import { ActiviteService } from '../../_services/activite.service';
import { UtilService } from '../../_services/util.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamService } from '../../_services/param.service';
import { FileService } from '../../_services/file.service';
import { ToastService } from '../../_services/toast.service';
import { ApiRenderService } from '../../_services/api-render.service';

describe('Adherent modal accords', () => {
  let fixture: ComponentFixture<UserComponent>;
  let component: UserComponent;
  let param: jasmine.SpyObj<ParamService>;
  let adherents: jasmine.SpyObj<AdherentService>;
  let adhesions: jasmine.SpyObj<AdhesionService>;
  const textes: ParamText[] = [
    { id: 1, paramName: 'RGPD', paramValue: 'Texte données personnelles' },
    { id: 2, paramName: 'DroitImage', paramValue: 'Texte droit image' }
  ];

  beforeEach(() => {
    param = jasmine.createSpyObj('ParamService', ['getAllText', 'getAllBoolean']);
    param.getAllText.and.returnValue(of(textes));
    param.getAllBoolean.and.returnValue(of([
      { paramName: 'Inscription', paramValue: true }, { paramName: 'Ouvert', paramValue: true }
    ] as any));
    adherents = jasmine.createSpyObj('AdherentService', ['update']);
    adhesions = jasmine.createSpyObj('AdhesionService', ['add']);
    TestBed.configureTestingModule({ imports: [UserComponent], providers: [provideRouter([]),
      { provide: ParamService, useValue: param },
      { provide: AdherentService, useValue: adherents },
      { provide: AdhesionService, useValue: adhesions },
      { provide: TokenStorageService, useValue: { getUser: () => ({ roles: ['ROLE_USER'] }) } },
      { provide: ActiviteService, useValue: { fillObjects: () => {} } },
      ...[MailService, UserService, UtilService, FileService, NgbActiveModal].map(provide => ({ provide, useValue: {} })),
      { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['success', 'error']) }
    ] });
    fixture = TestBed.createComponent(UserComponent);
    component = fixture.componentInstance;
    component.adherent = Object.assign(new Adherent(), {
      id: 7, nom: 'Martin', prenom: 'Alice', completAdhesion: true, activitesNm1: [],
      telephone: '0600000000', adresse: '1 rue des Fleurs', user: Object.assign(new User(), { username: 'alice@example.test' })
    });
    component.tribu = Object.assign(new Tribu(), { uuid: 'tribu', adherents: [component.adherent] });
    spyOn(component, 'fillFiles').and.stub();
    spyOn(component, 'chargerNewsletter').and.stub();
  });
  afterEach(() => fixture.destroy());

  it('restores both defaults when opening and blocks activities until saving succeeds', () => {
    const pendingSave = new Subject<Adherent>();
    adherents.update.and.returnValue(pendingSave);
    fixture.detectChanges();
    expect(component.adherent.accords.map(accord => accord.nom).sort()).toEqual(['DroitImage', 'RGPD']);
    expect(component.adherent.accords.every(accord => accord.etat && accord.datePassage == null)).toBeTrue();
    expect(component.adherent.accords.find(accord => accord.nom === 'RGPD')!.text).toBe(textes[0].paramValue);
    expect(component.adherent.accords.find(accord => accord.nom === 'RGPD')!.refusable).toBeFalse();
    expect(component.adherent.completAdhesion).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Enregistrez la fiche adhérent');
    component.choisir(component.adherent, 3);
    expect(adhesions.add).not.toHaveBeenCalled();

    component.adherent.accords.find(accord => accord.nom === 'DroitImage')!.etat = false;
    component.onSubmit(component.adherent);
    expect(adherents.update.calls.mostRecent().args[0].accords.find(accord => accord.nom === 'DroitImage')!.etat).toBeFalse();
    expect(component.accordsAdherentEnregistres(component.adherent)).toBeFalse();
    pendingSave.next(Object.assign(new Adherent(), component.adherent, {
      completAdhesion: true,
      accords: component.adherent.accords.map((accord, index) => ({ ...accord, id: index + 1, datePassage: new Date() }))
    }));
    expect(component.adherent.completAdhesion).toBeTrue();
    expect(component.accordsAdherentEnregistres(component.adherent)).toBeTrue();
    adhesions.add.and.returnValue(new Subject<Adhesion>());
    component.choisir(component.adherent, 3);
    expect(adhesions.add).toHaveBeenCalledWith(7, 3);
  });

  it('preserves existing consent and adds only the missing default', () => {
    const image = Object.assign(new Accord(), { id: 9, nom: 'DroitImage', etat: false, datePassage: new Date('2026-01-01') });
    component.adherent.accords = [image];
    fixture.detectChanges();
    component.initialiserAccords();
    expect(component.adherent.accords.length).toBe(2);
    expect(component.adherent.accords.find(accord => accord.nom === 'DroitImage')).toBe(image);
    expect(image.etat).toBeFalse();
    expect(image.datePassage).toEqual(new Date('2026-01-01'));
    expect(param.getAllText).toHaveBeenCalledTimes(1);
  });

  it('keeps a complete member unchanged when both accords have already been saved', () => {
    component.adherent.accords = ['RGPD', 'DroitImage'].map(nom => Object.assign(new Accord(), { nom, etat: true, datePassage: new Date() }));
    fixture.detectChanges();
    expect(component.adherent.completAdhesion).toBeTrue();
    expect(param.getAllText).not.toHaveBeenCalled();
  });

  it('prevents saving while texts load and allows retry after failure', () => {
    const pending = new Subject<ParamText[]>();
    param.getAllText.and.returnValue(pending);
    fixture.detectChanges();
    component.onSubmit(component.adherent);
    expect(adherents.update).not.toHaveBeenCalled();
    pending.error(new Error('network'));
    TestBed.inject(ApiRenderService).notify();
    expect(component.accordsError).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Réessayer');
    param.getAllText.and.returnValue(of(textes));
    component.initialiserAccords();
    expect(component.accordsAdherentRenseignes(component.adherent)).toBeTrue();
    TestBed.inject(ApiRenderService).notify();
    expect(fixture.nativeElement.textContent).toContain("J'accepte l'utilisation de mes données personnelles");
  });

  it('keeps activity addition blocked after a failed save', () => {
    fixture.detectChanges();
    adherents.update.and.returnValue(throwError(() => ({ status: 500, error: 'network' })));
    component.onSubmit(component.adherent);
    component.choisir(component.adherent, 3);
    expect(component.adherent.completAdhesion).toBeFalse();
    expect(adhesions.add).not.toHaveBeenCalled();
  });
});
