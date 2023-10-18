import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ReportingActivite, ReportingAdhesion } from '../models';

const API_URL = environment.server+'/reporting/';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class ReportingService {
  constructor(private http: HttpClient) { }

  getAllBasket(): Observable<ReportingActivite[]> {
    return this.http.get<ReportingActivite[]>(API_URL + 'getAllBasket', { responseType: 'json' });
  }

  getAllGeneral(): Observable<ReportingActivite[]> {
    return this.http.get<ReportingActivite[]>(API_URL + 'getAllGeneral', { responseType: 'json' });
  }

  getAllAdhesions(): Observable<ReportingAdhesion[]> {
    return this.http.get<ReportingAdhesion[]>(API_URL + 'getAllAdhesions', { responseType: 'json' });
  }

  
}


