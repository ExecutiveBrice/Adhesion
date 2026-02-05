import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import {Email, Groupe} from '../models';
import { environment } from 'src/environments/environment';
import {Historique} from "../models/historique";

@Injectable({
  providedIn: 'root'
})
export class MailService {
  apiUrl = environment.server+'/email';

  constructor(
    private http: HttpClient
  ) { }

  sendTemplate(maillingListe:Groupe[], templateId: number) {
    let params = new HttpParams().set('templateId', '' + templateId + '');
    return this.http.post<Historique>(this.apiUrl + "/sendTemplate", maillingListe, {params, responseType: 'json' })
  }


  sendMail(email: Email) {
    return this.http.post<Historique>(this.apiUrl + "/", email, { responseType: 'json' })
  }

  isInProgress() {
    return this.http.get<boolean>(this.apiUrl + "/isInProgress", { responseType: 'json' })
  }


  getHistorique() {
    return this.http.get<Historique[]>(this.apiUrl + "/historique", { responseType: 'json' })
  }
}
