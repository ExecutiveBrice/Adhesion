import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { BoardAdminComponent } from './board-admin.component';
import { ParamService } from '../../_services/param.service';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from '../../_services/adherent.service';
import { UserService } from '../../_services/user.service';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { SessionNavigationService } from '../../_services/session-navigation.service';

describe('BoardAdminComponent : présentation des tâches', () => {
  let component: BoardAdminComponent;
  let params: jasmine.SpyObj<ParamService>;

  beforeEach(() => {
    params = jasmine.createSpyObj('ParamService', ['getTachesSeance', 'createTacheSeance', 'updateTacheSeance']);
    params.createTacheSeance.and.callFake(tache => of({ ...tache, id: 3 }));
    params.updateTacheSeance.and.callFake(tache => of(tache));
    TestBed.configureTestingModule({
      imports: [BoardAdminComponent],
      providers: [
        { provide: ParamService, useValue: params },
        { provide: ActiviteService, useValue: {} },
        { provide: AdherentService, useValue: {} },
        { provide: UserService, useValue: {} },
        { provide: AuthService, useValue: {} },
        { provide: TokenStorageService, useValue: {} }
      ]
    });
    component = TestBed.createComponent(BoardAdminComponent).componentInstance;
  });

  it('transmet l’icône et la couleur à la création puis à la modification', () => {
    component.nouvelleTacheSeanceNom = ' Accueil ';
    component.nouvelleTacheSeanceIcone = 'handshake';
    component.nouvelleTacheSeanceCouleur = '#AB47BC';
    component.ajouterTacheSeance();
    expect(params.createTacheSeance).toHaveBeenCalledWith({ nom: 'Accueil', icone: 'handshake', couleur: '#AB47BC' });
    const tache = component.tachesSeance[0];
    tache.icone = 'bullhorn';
    tache.couleur = '#4285F4';
    component.enregistrerTacheSeance(tache);
    expect(params.updateTacheSeance).toHaveBeenCalledWith({ id: 3, nom: 'Accueil', icone: 'bullhorn', couleur: '#4285F4' });
    expect(component.tachesSeance[0].icone).toBe('bullhorn');
  });

  it('utilise les valeurs par défaut pour les anciennes réponses sans présentation', () => {
    params.getTachesSeance.and.returnValue(of([{ id: 3, nom: 'Matériel' }]));
    component.getTachesSeance();
    expect(component.tachesSeance[0]).toEqual({ id: 3, nom: 'Matériel', icone: 'clipboard-check', couleur: '#176B4A' });
  });
});

describe('BoardAdminComponent : impersonnalisation', () => {
  let component: BoardAdminComponent;
  let response: Subject<unknown>;
  let auth: jasmine.SpyObj<AuthService>;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let navigation: jasmine.SpyObj<SessionNavigationService>;

  beforeEach(() => {
    response = new Subject();
    auth = jasmine.createSpyObj('AuthService', ['impersonate']);
    auth.impersonate.and.returnValue(response);
    storage = jasmine.createSpyObj('TokenStorageService', ['saveToken', 'saveUser']);
    navigation = jasmine.createSpyObj('SessionNavigationService', ['reloadAccueil']);
    TestBed.configureTestingModule({
      imports: [BoardAdminComponent],
      providers: [
        { provide: ParamService, useValue: {} },
        { provide: AdherentService, useValue: {} },
        { provide: UserService, useValue: {} },
        { provide: AuthService, useValue: auth },
        { provide: TokenStorageService, useValue: storage },
        { provide: SessionNavigationService, useValue: navigation }
      ]
    });
    component = TestBed.createComponent(BoardAdminComponent).componentInstance;
  });

  it('enregistre la nouvelle session avant de recharger l’accueil après succès', () => {
    const session = { token: 'token-fictif', username: 'elodie@example.test', roles: ['ROLE_USER'] };
    navigation.reloadAccueil.and.callFake(() => {
      expect(storage.saveToken).toHaveBeenCalledWith(session.token);
      expect(storage.saveUser).toHaveBeenCalledWith(session);
    });
    component.impersonate(session.username);
    expect(auth.impersonate).toHaveBeenCalledWith(session.username);
    expect(storage.saveToken).not.toHaveBeenCalled();
    expect(navigation.reloadAccueil).not.toHaveBeenCalled();
    component.impersonate('paul@example.test');
    expect(auth.impersonate).toHaveBeenCalledTimes(1);
    response.next(session);
    expect(navigation.reloadAccueil).toHaveBeenCalledTimes(1);
  });

  it('conserve la session courante sans rechargement après un refus', () => {
    component.impersonate('elodie@example.test');
    response.error({ status: 403 });
    expect(storage.saveToken).not.toHaveBeenCalled();
    expect(storage.saveUser).not.toHaveBeenCalled();
    expect(navigation.reloadAccueil).not.toHaveBeenCalled();
    expect(component.impersonnalisationErreur).toContain('L’impersonnalisation a échoué.');
    expect(component.impersonnalisationEnCours).toBeFalse();
  });
});
