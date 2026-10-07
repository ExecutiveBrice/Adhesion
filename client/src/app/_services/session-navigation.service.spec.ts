import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { SessionNavigationService } from './session-navigation.service';

describe('Redirection vers un accueil actualisé', () => {
  ['https://asso.example/', 'https://asso.example/adhesion/'].forEach(baseURI => {
    it(`recharge l’accueil sans fragment depuis ${baseURI}`, () => {
      const assign = jasmine.createSpy('assign');
      TestBed.configureTestingModule({ providers: [
        { provide: DOCUMENT, useValue: { baseURI, location: { assign } } }
      ] });
      TestBed.inject(SessionNavigationService).reloadAccueil();
      expect(assign).toHaveBeenCalledOnceWith(`${baseURI}accueil`);
    });
  });
});
