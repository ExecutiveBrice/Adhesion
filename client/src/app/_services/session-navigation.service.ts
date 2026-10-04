import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SessionNavigationService {
  private readonly document = inject(DOCUMENT);

  reloadAccueil(): void {
    // Recréer les pages et services avec les données et droits de la nouvelle session.
    this.document.location.assign(new URL('accueil', this.document.baseURI).href);
  }
}
