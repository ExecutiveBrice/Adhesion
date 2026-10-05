import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface Publicite {
  id: number;
  description: string;
  facebookUrl?: string;
  snapchatUrl?: string;
  websiteUrl?: string;
  sectionIds: number[];
  displayOrder: number;
  active: boolean;
  imageVersion?: number;
}

@Injectable({ providedIn: 'root' })
export class PubliciteService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.server + '/publicites';

  list() { return this.http.get<Publicite[]>(this.url); }
  image(id: number) { return this.http.get(`${this.url}/${id}/image`, { responseType: 'blob' }); }
  create(description: string, file: File, links: Pick<Publicite, 'facebookUrl' | 'snapchatUrl' | 'websiteUrl'>,
         sectionIds: number[], displayOrder: number, active: boolean) {
    const body = this.form(description, links, sectionIds, displayOrder, active);
    body.append('file', file);
    return this.http.post<Publicite>(this.url, body);
  }
  update(id: number, description: string, file: File | undefined,
         links: Pick<Publicite, 'facebookUrl' | 'snapchatUrl' | 'websiteUrl'>,
         sectionIds: number[], displayOrder: number, active: boolean) {
    const body = this.form(description, links, sectionIds, displayOrder, active);
    if (file) body.append('file', file);
    return this.http.put<Publicite>(`${this.url}/${id}`, body);
  }
  private form(description: string, links: Pick<Publicite, 'facebookUrl' | 'snapchatUrl' | 'websiteUrl'>,
               sectionIds: number[], displayOrder: number, active: boolean): FormData {
    const body = new FormData();
    body.append('description', description.trim());
    if (links.facebookUrl?.trim()) body.append('facebookUrl', links.facebookUrl.trim());
    if (links.snapchatUrl?.trim()) body.append('snapchatUrl', links.snapchatUrl.trim());
    if (links.websiteUrl?.trim()) body.append('websiteUrl', links.websiteUrl.trim());
    sectionIds.forEach(sectionId => body.append('sectionIds', String(sectionId)));
    body.append('displayOrder', String(displayOrder));
    body.append('active', String(active));
    return body;
  }
  delete(id: number) { return this.http.delete<void>(`${this.url}/${id}`); }
}
