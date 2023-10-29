import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders,HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Activite } from '../models';

const API_URL = environment.server+'/activite/';

@Injectable({
  providedIn: 'root'
})
export class ActiviteService {
  constructor(private http: HttpClient) { }

  getAll(): Observable<Activite[]> {
    return this.http.get<Activite[]>(API_URL + 'all', { responseType: 'json' });
  }

  addReferent(activiteId: number, adherentId: string): Observable<Activite[]> {
    let params = new HttpParams().set('activiteId', '' + activiteId + '').set('adherentId', '' + adherentId + '');
    return this.http.get<Activite[]>(API_URL+"addReferent", {params, responseType: 'json' });
  }

  save(activite: Activite): Observable<Activite> {
    return this.http.post<Activite>(API_URL + 'save', activite, { responseType: 'json' });
  }
}


