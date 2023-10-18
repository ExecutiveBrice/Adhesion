import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Tribu } from '../models';

const API_URL = environment.server+'/tribu/';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class TribuService {
  constructor(private http: HttpClient) { }

  update(tribu: Tribu): Observable<any> {
    return this.http.put(API_URL, tribu, httpOptions);
  }
}


