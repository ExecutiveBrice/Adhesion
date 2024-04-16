import { Injectable } from '@angular/core';
import { HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Accord, Activite, Adhesion, Paiement } from '../models';

const API_URL = environment.server+'/adhesion/';

@Injectable({
  providedIn: 'root'
})
export class AdhesionService {
  constructor(private http: HttpClient) { }
  
  deleteAdhesion(adhesionId: number): Observable<any> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '');
    return this.http.delete(API_URL+"deleteAdhesion", {params, responseType: 'text' });
  }

  addAccord(adhesionId: number, nomAccord: string): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('nomAccord', '' + nomAccord + '');
    return this.http.get<Adhesion>(API_URL+"addAccord", {params, responseType: 'json' });
  }

  removeAccord(adhesionId: number, nomAccord: string): Observable<Accord[]> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('nomAccord', '' + nomAccord + '');
    return this.http.get<Accord[]>(API_URL+"removeAccord", {params, responseType: 'json' });
  }

  changeActivite(adhesionId: number, activiteId: number): Observable<Activite> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('activiteId', '' + activiteId + '');
    return this.http.get<Activite>(API_URL+"changeActivite", {params, responseType: 'json' });
  }

  updateDocumentsSecretariat(adhesionId: number, statut: boolean): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('statut', '' + statut + '');
    return this.http.get<Adhesion>(API_URL+"updateDocumentsSecretariat", {params, responseType: 'json' });
  }
  
  updatePaiementSecretariat(adhesionId: number, statut: boolean): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('statut', '' + statut + '');
    return this.http.get<Adhesion>(API_URL+"updatePaiementSecretariat", {params, responseType: 'json' });
  }

  updateFlag(adhesionId: number, statut: boolean): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('statut', '' + statut + '');
    return this.http.get<Adhesion>(API_URL+"updateFlag", {params, responseType: 'json' });
  }
  
  enregistrerRemarque(adhesionId: number, remarqueSecretariat: String): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('remarqueSecretariat', '' + remarqueSecretariat + '');
    return this.http.get<Adhesion>(API_URL+"enregistrerRemarque", {params, responseType: 'json' });
  }
  
  choisirStatut(adhesionId: number, statutActuel: String): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('statutActuel', '' + statutActuel + '');
    return this.http.get<Adhesion>(API_URL+"choisirStatut", {params, responseType: 'json' });
  }
  
  add(adherentId: number, activiteId: number): Observable<Adhesion> {
    console.log(adherentId)
    console.log(activiteId)
    let params = new HttpParams().set('adherentId', '' + adherentId + '').set('activiteId', '' + activiteId + '');
    return this.http.post<Adhesion>(API_URL+"save", null,{params, responseType: 'json' });
  }
  
  getAll(): Observable<Adhesion[]> {
    return this.http.get<Adhesion[]>(API_URL+"all", {responseType: 'json' });
  }

  getAllLiteBysection(sections:string): Observable<Adhesion[]> {
    let params = new HttpParams().set('sections', '' + sections + '');
    return this.http.get<Adhesion[]>(API_URL+"liteBysection", {params, responseType: 'json' });
  }
  
  validation(accords: Accord[], adhesionId : number): Observable<any> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '');
    return this.http.post(API_URL+"validation", accords, {params, responseType: 'json' });
  }

  addVisite(adhesionId : number): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '');
    return this.http.post<Adhesion>(API_URL+"addVisite", null,  {params, responseType: 'json' });
  }
  deleteSurclassement(adhesionId: number): Observable<any> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '');
    return this.http.delete(API_URL+"deleteSurclassement", {params, responseType: 'text' });
  }

  saveSurclassement(adhesionId: number,surClassementId: number): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('surClassementId', '' + surClassementId + '');
    return this.http.post<Adhesion>(API_URL+"saveSurclassement", {}, {params, responseType: 'json' });
  }
  deletePaiement(adhesionId: number,paiementId: number): Observable<any> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '').set('paiementId', '' + paiementId + '');
    return this.http.delete(API_URL+"deletePaiement", {params, responseType: 'text' });
  }

  savePaiement(adhesionId: number,paiement: Paiement): Observable<Adhesion> {
    let params = new HttpParams().set('adhesionId', '' + adhesionId + '');
    return this.http.post<Adhesion>(API_URL+"savePaiement", paiement, {params, responseType: 'json' });
  }
}


