import { Injectable, inject } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { Evenement } from '../models/evenement';
import { EvenementDetailComponent } from '../template/evenement-detail/evenement-detail.component';

@Injectable({ providedIn: 'root' })
export class EvenementModalService {
  private readonly modales = inject(NgbModal);
  private readonly ouvertes = new Map<NgbModalRef, Evenement>();

  ouvrir(evenement: Evenement, adherentId?: number): NgbModalRef {
    const modale = this.modales.open(EvenementDetailComponent, {
      centered: true, scrollable: true, ariaLabelledBy: 'evenement-detail-titre'
    });
    modale.componentInstance.evenement = evenement;
    modale.componentInstance.adherentId = adherentId;
    this.ouvertes.set(modale, evenement);
    modale.result.then(() => this.ouvertes.delete(modale), () => this.ouvertes.delete(modale));
    return modale;
  }

  actualiser(evenement: Evenement): void {
    this.ouvertes.forEach((selection, modale) => {
      if (selection === evenement) (modale.componentInstance as EvenementDetailComponent).actualiser();
    });
  }
}
