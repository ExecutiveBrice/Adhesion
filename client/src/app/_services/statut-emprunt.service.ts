import { DestroyRef, Injectable, NgZone, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EmpruntMateriel, Materiel } from '../models/materiel';

export type StatutEmprunt = 'À venir' | 'En cours' | 'Rapporté';

@Injectable({ providedIn: 'root' })
export class StatutEmpruntService {
  private readonly zone = inject(NgZone);
  private readonly document = inject(DOCUMENT);
  private readonly aujourdHui = signal(this.dateDuJour());
  private timer?: ReturnType<typeof setTimeout>;

  get dateCourante(): string { return this.aujourdHui(); }

  constructor() {
    this.planifierActualisation();
    const actualiser = () => {
      if (this.document.visibilityState === 'visible') {
        this.aujourdHui.set(this.dateDuJour());
        this.planifierActualisation();
      }
    };
    this.document.addEventListener('visibilitychange', actualiser);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.timer);
      this.document.removeEventListener('visibilitychange', actualiser);
    });
  }

  statut(emprunt: EmpruntMateriel): StatutEmprunt {
    if (emprunt.dateRetour) return 'Rapporté';
    return emprunt.dateDebut && emprunt.dateDebut > this.aujourdHui() ? 'À venir' : 'En cours';
  }

  quantite(materiel: Materiel, statut: StatutEmprunt): number {
    if (!materiel.emprunts) return statut === 'En cours' ? (materiel.quantiteEmpruntee ?? 0) : 0;
    return materiel.emprunts
      .filter(emprunt => emprunt.horsService == null && this.statut(emprunt) === statut)
      .reduce((total, emprunt) => total + (emprunt.quantite ?? 1) - (emprunt.quantiteRetournee ?? 0), 0);
  }

  private dateDuJour(): string {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private planifierActualisation(): void {
    clearTimeout(this.timer);
    const maintenant = new Date();
    const demain = new Date(maintenant.getFullYear(), maintenant.getMonth(), maintenant.getDate() + 1);
    this.zone.runOutsideAngular(() => {
      this.timer = setTimeout(() => {
        this.zone.run(() => this.aujourdHui.set(this.dateDuJour()));
        this.planifierActualisation();
      }, demain.getTime() - maintenant.getTime());
    });
  }
}
