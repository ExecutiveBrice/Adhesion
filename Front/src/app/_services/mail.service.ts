import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { Email } from '../models';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MailService {
  apiUrl = environment.server+'/api_adhesion/email';

  constructor(
    private http: HttpClient
  ) { }

  sendMail(email: Email) {
    return this.http.post(this.apiUrl + "/", email, { responseType: 'json' })
  }

  isInProgress() {
    return this.http.get<boolean>(this.apiUrl + "/isInProgress", { responseType: 'json' })
  }

  getRestant() {
    return this.http.get<number>(this.apiUrl + "/getRestant", { responseType: 'json' })
  }

  getTotal(mailling:string) {
    let params = new HttpParams().set('mailling', '' + mailling + '');
    return this.http.get<number>(this.apiUrl + "/getTotal", {params, responseType: 'json' })
  }

  
}
