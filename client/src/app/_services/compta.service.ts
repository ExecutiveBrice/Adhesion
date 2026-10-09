import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ComptaActivite } from '../models/comptaActivite';
import { ComptaPeriode } from '../models/comptaPeriode';
import { Paiement } from '../models/paiement';

const API_URL = environment.server+'/compta/';

@Injectable({
  providedIn: 'root'
})
export class ComptaService {
  private http = inject(HttpClient);


  getAll(dateDebutPeriode: string, dateFinPeriode: string): Observable<ComptaActivite[]> {
    const params = new HttpParams().set('dateDebutPeriode', dateDebutPeriode).set('dateFinPeriode', dateFinPeriode);
    return this.http.get<ComptaActivite[]>(API_URL + 'getAll', {params, responseType: 'json' });
  }

  getPeriode(dateDebutPeriode: string, dateFinPeriode: string): Observable<ComptaPeriode> {
    const params = new HttpParams().set('dateDebutPeriode', dateDebutPeriode).set('dateFinPeriode', dateFinPeriode);
    return this.http.get<ComptaPeriode>(API_URL + 'periode', { params });
  }

  updateRapprochement(adhesionId: number, rapprochement: boolean): Observable<void> {
    const params = new HttpParams().set('rapprochement', rapprochement);
    return this.http.put<void>(API_URL + `adhesions/${adhesionId}/rapprochement`, null, { params });
  }

  savePaiement(adhesionId: number, paiement: Paiement): Observable<void> {
    return this.http.post<void>(API_URL + `adhesions/${adhesionId}/paiements`, {
      id: paiement.id ?? null, montant: paiement.montant,
      dateReglement: String(paiement.dateReglement), typeReglement: String(paiement.typeReglement)
    });
  }

  deletePaiement(adhesionId: number, paiementId: number): Observable<void> {
    return this.http.delete<void>(API_URL + `adhesions/${adhesionId}/paiements/${paiementId}`);
  }

  
  
}


