import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Tribu } from '../models';
import { ActivitesNm1 } from '../models/activitesNm1';

const API_URL = environment.server+'/tribu/';


@Injectable({
  providedIn: 'root'
})
export class TribuService {
  constructor(private http: HttpClient) { }

  update(tribu: Tribu): Observable<any> {
    return this.http.put(API_URL, tribu, {responseType: 'json'});
  }


  getTribuByUuid(tribuUuid: String): Observable<Tribu> {
    let params = new HttpParams().set('tribuUuid', '' + tribuUuid + '');
    return this.http.get<Tribu>(API_URL + 'getTribuByUuid', {params, responseType: 'json'});
  }

  getConnected(): Observable<Tribu> {
    return this.http.get<Tribu>(API_URL + 'getConnectedTribu', { responseType: 'json'});
  }

  addActivitesNm1(tribuUuid: String, activitesNm1: ActivitesNm1[]): Observable<Tribu> {
    let params = new HttpParams().set('tribuUuid', '' + tribuUuid + '');
    return this.http.post<Tribu>(API_URL + 'addActivitesNm1', activitesNm1,{params, responseType: 'json' });
  }


}


