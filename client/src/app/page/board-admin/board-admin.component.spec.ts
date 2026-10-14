import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { BoardAdminComponent } from './board-admin.component';
import { MailService, NewsletterSyncProgress } from '../../_services/mail.service';
import { UserService } from '../../_services/user.service';
import { ParamService } from '../../_services/param.service';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from '../../_services/adherent.service';
import { UserService } from '../../_services/user.service';
import { AuthService } from '../../_services/auth.service';
import { TokenStorageService } from '../../_services/token-storage.service';

describe('Administration contacts Brevo', () => {
describe('BoardAdminComponent : présentation des tâches', () => {
  let component: BoardAdminComponent;
  let mail: jasmine.SpyObj<MailService>;
  const progress = (running: boolean): NewsletterSyncProgress => ({
    running, total: 3, processed: running ? 1 : 3, subscribed: 1,
    resubscribed: 1, alreadySubscribed: 1, unresolved: 0, ignored: 2, error: ''
  });
  let params: jasmine.SpyObj<ParamService>;

  beforeEach(() => {
    mail = jasmine.createSpyObj('MailService', ['synchronizeNewsletterContacts', 'getNewsletterContactSyncStatus']);
    TestBed.configureTestingModule({ imports: [BoardAdminComponent], providers: [
      { provide: MailService, useValue: mail },
      ...[UserService, ParamService, AdherentService, AuthService, TokenStorageService]
        .map(provide => ({ provide, useValue: {} }))
    ] });
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

  it('confirms global unblocking and prevents duplicate requests', () => {
    const confirmation = spyOn(window, 'confirm').and.returnValue(false);
    component.synchroniserNewsletter();
    expect(mail.synchronizeNewsletterContacts).not.toHaveBeenCalled();
    expect(confirmation.calls.mostRecent().args[0]).toContain('globalement');
    confirmation.and.returnValue(true);
    mail.synchronizeNewsletterContacts.and.returnValue(new Subject<NewsletterSyncProgress>());
    component.synchroniserNewsletter(); component.synchroniserNewsletter();
    expect(mail.synchronizeNewsletterContacts).toHaveBeenCalledTimes(1);
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
  it('utilise les valeurs par défaut pour les anciennes réponses sans présentation', () => {
    params.getTachesSeance.and.returnValue(of([{ id: 3, nom: 'Matériel' }]));
    component.getTachesSeance();
    expect(component.tachesSeance[0]).toEqual({ id: 3, nom: 'Matériel', icone: 'clipboard-check', couleur: '#176B4A' });
  });
});
