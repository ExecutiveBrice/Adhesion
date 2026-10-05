import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { SessionNavigationService } from './session-navigation.service';

describe('Redirection vers un accueil actualisé', () => {
  it('recharge l’accueil même depuis l’accueil et respecte le chemin de déploiement', () => {
    const assign = jasmine.createSpy('assign');
    TestBed.configureTestingModule({ providers: [
      { provide: DOCUMENT, useValue: { baseURI: 'https://asso.example/', location: { assign } } }
    ] });
    TestBed.inject(SessionNavigationService).reloadAccueil();
    expect(assign).toHaveBeenCalledOnceWith('https://asso.example/adhesion/accueil');
  });
});
