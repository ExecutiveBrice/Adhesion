import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Activite } from '../models';

const API_URL = environment.server+'/api_adhesion/activite/';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class ActiviteService {
  constructor(private http: HttpClient) { }

  getAll(): Observable<Activite[]> {
    return this.http.get<Activite[]>(API_URL + 'all', { responseType: 'json' });
  }

  save(activite: Activite): Observable<Activite> {
    return this.http.post<Activite>(API_URL + 'save', activite, { responseType: 'json' });
  }
}


