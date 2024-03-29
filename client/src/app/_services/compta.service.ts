import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ComptaActivite } from '../models/comptaActivite';

const API_URL = environment.server+'/compta/';

@Injectable({
  providedIn: 'root'
})
export class ComptaService {
  constructor(private http: HttpClient) { }

  getAll(dateDebutPeriode : Date, dateFinPeriode : Date): Observable<ComptaActivite[]> {
    let params = new HttpParams().set('dateDebutPeriode', '' + dateDebutPeriode.toISOString() + '').set('dateFinPeriode', '' + dateFinPeriode.toISOString() + '');
    return this.http.get<ComptaActivite[]>(API_URL + 'getAll', {params, responseType: 'json' });
  }

  
  
}


