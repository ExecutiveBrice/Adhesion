import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { BoardAdminComponent } from './board-admin.component';
import { MailService, NewsletterSyncProgress } from '../../_services/mail.service';
import { UserService } from '../../_services/user.service';
import { ParamService } from '../../_services/param.service';
import { AdherentService } from '../../_services/adherent.service';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';

describe('Administration', () => {
  let component: BoardAdminComponent;
  let fixture: ComponentFixture<BoardAdminComponent>;
  let mail: jasmine.SpyObj<MailService>;
  let auth: jasmine.SpyObj<AuthService>;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let params: jasmine.SpyObj<ParamService>;
  const progress = (running: boolean): NewsletterSyncProgress => ({
    running, total: 3, processed: running ? 1 : 3, subscribed: 1,
    resubscribed: 1, alreadySubscribed: 1, unresolved: 0, ignored: 2, error: ''
  });

  beforeEach(() => {
    mail = jasmine.createSpyObj('MailService', ['synchronizeNewsletterContacts', 'getNewsletterContactSyncStatus']);
    auth = jasmine.createSpyObj('AuthService', ['impersonate']);
    storage = jasmine.createSpyObj('TokenStorageService', ['saveToken', 'saveUser']);
    params = jasmine.createSpyObj('ParamService', ['getTachesSeance', 'createTacheSeance', 'updateTacheSeance']);
    params.createTacheSeance.and.callFake(tache => of({ ...tache, id: 3 }));
    params.updateTacheSeance.and.callFake(tache => of(tache));
    TestBed.configureTestingModule({ imports: [BoardAdminComponent], providers: [
      { provide: MailService, useValue: mail },
      { provide: AuthService, useValue: auth },
      { provide: TokenStorageService, useValue: storage },
      { provide: ParamService, useValue: params },
      ...[UserService, AdherentService]
        .map(provide => ({ provide, useValue: {} }))
    ] });
    fixture = TestBed.createComponent(BoardAdminComponent);
    component = fixture.componentInstance;
  });

  it('filtre les adhérents depuis le champ de recherche, sans tenir compte des accents et de la casse', fakeAsync(() => {
    spyOn(component, 'getAllBoolean');
    spyOn(component, 'getAllText');
    spyOn(component, 'getAllNumber');
    spyOn(component, 'getAgendasGoogle');
    spyOn(component, 'getSalles');
    spyOn(component, 'getTachesSeance');
    spyOn(component, 'getSections');
    spyOn(component, 'fillLists');
    spyOn(component, 'actualiserSynchronisationNewsletter');
    component.usersLite = [
      { id: 1, adherent: 'Élodie Martin', username: 'elodie@example.fr', roles: [] },
      { id: 2, adherent: 'Paul Dupont', username: 'paul@example.fr', roles: [] }
    ];
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[ngbAccordionItem="utilisateurs"] [ngbAccordionButton]').click();
    fixture.detectChanges();
    const toggle = fixture.nativeElement.querySelector('#dropdownImpersonate');
    toggle.click();
    fixture.detectChanges();
    tick();
    const menu = document.querySelector('[aria-labelledby="dropdownImpersonate"]')!;
    const input = menu.querySelector('input')!;
    const rechercher = (valeur: string) => {
      input.value = valeur;
      input.dispatchEvent(new Event('input'));
      tick();
      fixture.detectChanges();
    };

    rechercher(' ELODIE ');
    expect(menu.querySelectorAll('button').length).toBe(1);
    expect(menu.querySelector('button')!.textContent).toContain('Élodie Martin');
    rechercher('PAUL@EXAMPLE');
    expect(menu.querySelector('button')!.textContent).toContain('Paul Dupont');
    rechercher('inexistant');
    expect(menu.querySelectorAll('button').length).toBe(0);
    expect(menu.textContent).toContain('Aucun adhérent trouvé');
    rechercher('');
    expect(menu.querySelectorAll('button').length).toBe(2);
    toggle.click();
    fixture.detectChanges();
    tick();
  }));

  it('enregistre la nouvelle session avant de recharger la page inscription', () => {
    const session = { token: 'token-adherent', username: 'elodie@example.fr' };
    const rechargement = spyOn<any>(component, 'rechargerInscription').and.callFake(() => {
      expect(storage.saveToken).toHaveBeenCalledWith(session.token);
      expect(storage.saveUser).toHaveBeenCalledWith(session);
    });
    const reponse = new Subject<typeof session>();
    auth.impersonate.and.returnValue(reponse);

    component.impersonate(session.username);
    expect(auth.impersonate).toHaveBeenCalledWith(session.username);
    expect(rechargement).not.toHaveBeenCalled();
    reponse.next(session);
    expect(rechargement).toHaveBeenCalledTimes(1);
  });

  it('conserve la session et la page courantes en cas de refus de l’impersonnalisation', () => {
    const rechargement = spyOn<any>(component, 'rechargerInscription');
    auth.impersonate.and.returnValue(throwError(() => new Error('refus')));
    component.impersonate('elodie@example.fr');
    expect(storage.saveToken).not.toHaveBeenCalled();
    expect(storage.saveUser).not.toHaveBeenCalled();
    expect(rechargement).not.toHaveBeenCalled();
  });

  it('confirms global unblocking and prevents duplicate requests', () => {
    const confirmation = spyOn(window, 'confirm').and.returnValue(false);
    component.synchroniserNewsletter();
    expect(mail.synchronizeNewsletterContacts).not.toHaveBeenCalled();
    expect(confirmation.calls.mostRecent().args[0]).toContain('globalement');
    confirmation.and.returnValue(true);
    mail.synchronizeNewsletterContacts.and.returnValue(new Subject<NewsletterSyncProgress>());
    component.synchroniserNewsletter(); component.synchroniserNewsletter();
    expect(mail.synchronizeNewsletterContacts).toHaveBeenCalledTimes(1);
  });

  it('follows a running batch and stops polling on completion', fakeAsync(() => {
    mail.getNewsletterContactSyncStatus.and.returnValues(of(progress(true)), of(progress(false)));
    component.actualiserSynchronisationNewsletter(); tick(0);
    expect(component.newsletterSyncBusy).toBeTrue();
    tick(2000);
    expect(component.newsletterSyncBusy).toBeFalse();
    expect(component.newsletterSync?.resubscribed).toBe(1);
    tick(4000);
    expect(mail.getNewsletterContactSyncStatus).toHaveBeenCalledTimes(2);
    expect(mail.synchronizeNewsletterContacts).not.toHaveBeenCalled();
  }));

  it('reports uncertain start without replaying the mutation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    mail.synchronizeNewsletterContacts.and.returnValue(throwError(() => new Error('network')));
    component.synchroniserNewsletter();
    expect(component.newsletterSyncError).toContain('Actualisez le suivi');
    expect(component.newsletterSyncBusy).toBeFalse();
    expect(mail.synchronizeNewsletterContacts).toHaveBeenCalledTimes(1);
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
