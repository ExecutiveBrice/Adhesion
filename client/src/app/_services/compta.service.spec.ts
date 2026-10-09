import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ComptaService } from './compta.service';
import { Paiement } from '../models/paiement';

describe('ComptaService', () => {
  it('transmits dates as calendar days without a timezone conversion', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(ComptaService).getAll('2026-03-29', '2026-10-25').subscribe();
    const request = http.expectOne(req => req.url.endsWith('/compta/getAll'));
    expect(request.request.params.get('dateDebutPeriode')).toBe('2026-03-29');
    expect(request.request.params.get('dateFinPeriode')).toBe('2026-10-25');
    request.flush([]);
    http.verify();
  });

  it('loads the summary and contributing adhesions in one request', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(ComptaService).getPeriode('2026-09-01', '2026-09-30').subscribe(data => {
      expect(data).toEqual({ recap: [], adhesions: [] });
    });
    const request = http.expectOne(req => req.url.endsWith('/compta/periode'));
    expect(request.request.params.get('dateDebutPeriode')).toBe('2026-09-01');
    expect(request.request.params.get('dateFinPeriode')).toBe('2026-09-30');
    request.flush({ recap: [], adhesions: [] });
    http.verify();
  });

  it('persists both reconciliation choices for the selected adhesion', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ComptaService);
    for (const value of [true, false]) {
      service.updateRapprochement(7, value).subscribe();
      const request = http.expectOne(req => req.url.endsWith('/compta/adhesions/7/rapprochement'));
      expect(request.request.method).toBe('PUT');
      expect(request.request.params.get('rapprochement')).toBe(String(value));
      request.flush(null);
    }
    http.verify();
  });

  it('saves complete payment values and deletes within the adhesion', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const service = TestBed.inject(ComptaService);
    service.savePaiement(7, Object.assign(new Paiement(), {
      id: 8, montant: 15, dateReglement: '2026-09-30', typeReglement: 'CB'
    })).subscribe();
    const save = http.expectOne(req => req.url.endsWith('/compta/adhesions/7/paiements'));
    expect(save.request.method).toBe('POST');
    expect(save.request.body).toEqual({ id: 8, montant: 15, dateReglement: '2026-09-30', typeReglement: 'CB' });
    save.flush(null);
    service.deletePaiement(7, 8).subscribe();
    const remove = http.expectOne(req => req.url.endsWith('/compta/adhesions/7/paiements/8'));
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
    http.verify();
  });
});
