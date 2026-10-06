import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface AgendaPreferences {
  sectionsMasquees: number[];
  agendasGoogleMasques: number[];
}

@Injectable({ providedIn: 'root' })
export class AgendaPreferencesService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.server + '/adherent/me/agenda-preferences';

  get() { return this.http.get<AgendaPreferences>(this.url); }
  update(preferences: AgendaPreferences) { return this.http.put<AgendaPreferences>(this.url, preferences); }
}
