import { Injectable, inject } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { AgendaGoogleConfiguration, ParamBoolean, ParamNumber, ParamText, SalleConfiguration } from '../models';

const API_URL = environment.server+'/param/';

@Injectable({
  providedIn: 'root'
})
export class ParamService {
  private http = inject(HttpClient);


  getAllText(): Observable<ParamText[]> {
    return this.http.get<ParamText[]>(API_URL + 'allText', { responseType: 'json' });
  }

  saveText(param: ParamText): Observable<ParamText> {
    return this.http.post<ParamText>(API_URL + 'saveText', param, { responseType: 'json' });
  }

  getAgendasGoogle(): Observable<AgendaGoogleConfiguration[]> {
    return this.http.get<AgendaGoogleConfiguration[]>(API_URL + 'agendas', { responseType: 'json' });
  }

  createAgendaGoogle(agenda: AgendaGoogleConfiguration): Observable<AgendaGoogleConfiguration> {
    return this.http.post<AgendaGoogleConfiguration>(API_URL + 'agendas', agenda, { responseType: 'json' });
  }

  updateAgendaGoogle(agenda: AgendaGoogleConfiguration): Observable<AgendaGoogleConfiguration> {
    return this.http.put<AgendaGoogleConfiguration>(API_URL + 'agendas/' + agenda.id, agenda, { responseType: 'json' });
  }

  deleteAgendaGoogle(agendaId: number): Observable<void> {
    return this.http.delete<void>(API_URL + 'agendas/' + agendaId);
  }

  getSalles(): Observable<SalleConfiguration[]> {
    return this.http.get<SalleConfiguration[]>(API_URL + 'salles', { responseType: 'json' });
  }

  createSalle(salle: SalleConfiguration): Observable<SalleConfiguration> {
    return this.http.post<SalleConfiguration>(API_URL + 'salles', salle, { responseType: 'json' });
  }

  updateSalle(salle: SalleConfiguration): Observable<SalleConfiguration> {
    return this.http.put<SalleConfiguration>(API_URL + 'salles/' + salle.id, salle, { responseType: 'json' });
  }

  deleteSalle(salleId: number): Observable<void> {
    return this.http.delete<void>(API_URL + 'salles/' + salleId);
  }

  getAllBoolean(): Observable<ParamBoolean[]> {
    return this.http.get<ParamBoolean[]>(API_URL + 'allBoolean', { responseType: 'json' });
  }

  saveBoolean(param: ParamBoolean): Observable<ParamBoolean> {
    return this.http.post<ParamBoolean>(API_URL + 'saveBoolean', param, { responseType: 'json' });
  }

  isClose(): Observable<Boolean> {
    return this.http.get<Boolean>(API_URL + 'isClose', { responseType: 'json' })
  }

  getAllNumber(): Observable<ParamNumber[]> {
    return this.http.get<ParamNumber[]>(API_URL + 'allNumber', { responseType: 'json' });
  }

  saveNumber(param: ParamNumber): Observable<ParamNumber> {
    return this.http.post<ParamNumber>(API_URL + 'saveNumber', param, { responseType: 'json' });
  }

}
