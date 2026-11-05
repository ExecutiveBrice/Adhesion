import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { MembreCAMateriel, EmpruntCalendrierMateriel, EmpruntMateriel, Materiel, Zonage } from '../models/materiel';

const API_URL = environment.server + '/materiel';

@Injectable({ providedIn: 'root' })
export class MaterielService {
  private readonly http = inject(HttpClient);

  getAll() { return this.http.get<Materiel[]>(API_URL); }
  getCalendrier(debut: string, fin: string) {
    return this.http.get<EmpruntCalendrierMateriel[]>(`${API_URL}/calendrier`, { params: { debut, fin } });
  }
  create(nom: string, zonageId: number, horsService = false, quantiteTotale = 1, image?: string) {
    return this.http.post<Materiel>(API_URL, { nom, zonageId, horsService, quantiteTotale, image });
  }
  update(id: number, nom: string, zonageId: number, quantiteTotale = 1, image?: string) {
    return this.http.put<Materiel>(`${API_URL}/${id}`, { nom, zonageId, quantiteTotale, image });
  }
  delete(id: number) { return this.http.delete<void>(`${API_URL}/${id}`); }
  emprunter(id: number, emprunteurId?: number, quantite = 1, dateDebut?: string, dateFinPrevue?: string) {
    return this.http.post<Materiel>(`${API_URL}/${id}/emprunt`, { emprunteurId, quantite, dateDebut, dateFinPrevue });
  }
  restituer(id: number, empruntId?: number, quantite?: number) { return this.http.post<Materiel>(`${API_URL}/${id}/retour`, { empruntId, quantite }); }
  getMembresCA() { return this.http.get<MembreCAMateriel[]>(`${API_URL}/membres-ca`); }
  getHistorique(id: number) { return this.http.get<EmpruntMateriel[]>(`${API_URL}/${id}/historique`); }
  updateHorsService(id: number, horsService: boolean, commentaire: string, quantiteHorsService?: number) {
    return this.http.patch<Materiel>(`${API_URL}/${id}/hors-service`, { horsService, commentaire, quantiteHorsService });
  }
  getZonages() { return this.http.get<Zonage[]>(`${API_URL}/zonages`); }
  createZonage(nom: string, couleur: string) {
    return this.http.post<Zonage>(`${API_URL}/zonages`, { nom, couleur });
  }
  updateZonage(zonage: Zonage) {
    return this.http.put<Zonage>(`${API_URL}/zonages/${zonage.id}`, zonage);
  }
  deleteZonage(id: number) { return this.http.delete<void>(`${API_URL}/zonages/${id}`); }
}
