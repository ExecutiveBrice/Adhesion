import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { PresencePrevue } from '../models/seance';

@Injectable({ providedIn: 'root' })
export class PresencePrevueService {
  private readonly http = inject(HttpClient);

  getPresences(seanceId: number): Observable<PresencePrevue[]> {
    return this.http.get<PresencePrevue[]>(`${environment.server}/seance/${seanceId}/presences-prevues`);
  }

  updatePresence(seanceId: number, presencePrevue: boolean): Observable<PresencePrevue> {
    return this.http.patch<PresencePrevue>(`${environment.server}/seance/${seanceId}/presence-prevue`, { presencePrevue });
  }
}
