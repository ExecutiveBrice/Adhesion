import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ActiviteLite, Adherent, Document } from '../models';

const API_URL = environment.server+'/api_adhesion/adherent/';



@Injectable({
  providedIn: 'root'
})
export class AdherentService {
  constructor(private http: HttpClient) { }

  deleteAdherent(adherentId : number): Observable<string> {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    return this.http.delete(API_URL + 'deleteAdherent', {params, responseType: 'text' });
  }

  addDocument(document:File, adherentId : number): Observable<Document> {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    const formData: FormData = new FormData();

    formData.append('file', document);

    return this.http.post<Document>(API_URL + 'addDocument', formData, {params, responseType: 'json' });
  }

  deleteDoc(docId : number, adherentId : number): Observable<string> {
    let params = new HttpParams().set('docId', '' + docId + '').set('adherentId', '' + adherentId + '');
    return this.http.delete(API_URL + 'deleteDoc', {params, responseType: 'text' });
  }
  
  getAllId(): Observable<number[]> {
    return this.http.get<number[]>(API_URL + 'allId', { responseType: 'json' });
  }

  getAll(): Observable<Adherent[]> {
    return this.http.get<Adherent[]>(API_URL + 'all', { responseType: 'json' });
  }

  getAllCours(): Observable<ActiviteLite[]> {
    return this.http.get<ActiviteLite[]>(API_URL + 'getAllCours', { responseType: 'json' });
  }


  
  getById(adherentId : number): Observable<Adherent> {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    return this.http.get<Adherent>(API_URL + 'getById', {params, responseType: 'json' });
  }
  
  updateEmail(email: string, adherentId : number): Observable<Adherent> {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    return this.http.post<Adherent>(API_URL+"updateEmail", email, {params, responseType: 'json' });
  }

  update(adherent: Adherent): Observable<Adherent> {
    return this.http.post<Adherent>(API_URL+"update", adherent, { responseType: 'json' });
  }

  save(adherent: Adherent, tribuId:number): Observable<Adherent> {
    let params = new HttpParams().set('tribuId', '' + tribuId + '');
    return this.http.post<Adherent>(API_URL+"save", adherent, {params, responseType: 'json' });
  }

  changeTribu(referentId: number, adherentId: number): Observable<Adherent> {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    return this.http.post<Adherent>(API_URL + 'changeTribu', referentId, {params, responseType: 'json' });
  }
}


