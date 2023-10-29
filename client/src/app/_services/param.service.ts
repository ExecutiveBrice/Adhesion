import { Injectable } from '@angular/core';
import { HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ParamBoolean, ParamNumber, ParamText } from '../models';

const API_URL = environment.server+'/param/';

@Injectable({
  providedIn: 'root'
})
export class ParamService {
  constructor(private http: HttpClient) { }

  getAllText(): Observable<ParamText[]> {
    return this.http.get<ParamText[]>(API_URL + 'allText', { responseType: 'json' });
  }

  saveText(param: ParamText): Observable<ParamText> {
    return this.http.post<ParamText>(API_URL + 'saveText', param, { responseType: 'json' });
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

