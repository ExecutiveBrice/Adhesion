import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MaterielService } from './materiel.service';

describe('MaterielService — période d’emprunt', () => {
  it('transmet la photo à la création puis son retrait avec les autres informations', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(MaterielService);
    const image = 'data:image/png;base64,cGhvdG8=';
    service.create('Objet', 1, false, 2, image).subscribe();
    const creation = http.expectOne(req => req.url.endsWith('/materiel'));
    expect(creation.request.body).toEqual({ nom: 'Objet', zonageId: 1, horsService: false, quantiteTotale: 2, image });
    creation.flush({});
    service.update(2, 'Objet renommé', 1, 2, '').subscribe();
    const modification = http.expectOne(req => req.url.endsWith('/materiel/2'));
    expect(modification.request.body).toEqual({ nom: 'Objet renommé', zonageId: 1, quantiteTotale: 2, image: '' });
    modification.flush({});
    http.verify();
  });

  it('transmet les jours sélectionnés sans conversion de fuseau horaire', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(MaterielService).emprunter(2, 3, 4, '2026-03-29', '2026-03-30').subscribe();
    const request = http.expectOne(req => req.url.endsWith('/materiel/2/emprunt'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      emprunteurId: 3, quantite: 4, dateDebut: '2026-03-29', dateFinPrevue: '2026-03-30'
    });
    request.flush({});
    http.verify();
  });
});
