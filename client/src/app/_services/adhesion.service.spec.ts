import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../environments/environment';
import { Adhesion } from '../models';
import { AdhesionService } from './adhesion.service';

describe('AdhesionService', () => {
  let service: AdhesionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdhesionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ajoute une adhésion sur la route du contrôleur avec les identifiants choisis', () => {
    const adhesion = new Adhesion();
    adhesion.id = 123;
    let result: Adhesion | undefined;

    service.add(42, 7).subscribe(value => result = value);

    const request = http.expectOne(req => req.url === environment.server + '/adhesion/save');
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('adherentId')).toBe('42');
    expect(request.request.params.get('activiteId')).toBe('7');
    expect(request.request.body).toBeNull();
    request.flush(adhesion);

    expect(result).toEqual(adhesion);
  });
});
