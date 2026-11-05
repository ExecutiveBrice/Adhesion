import { Component, inject, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { Zonage } from '../../models/materiel';
import { PhotoMaterielComponent } from '../photo-materiel/photo-materiel.component';

@Component({
  selector: 'app-materiel-modal',
  imports: [FormsModule, PhotoMaterielComponent],
  templateUrl: './materiel-modal.component.html'
})
export class MaterielModalComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  @Input() zonages: Zonage[] = [];
  nom = '';
  quantiteTotale = 1;
  zonageId: number | null = null;
  horsService = false;
  image: string | undefined;
  chargementImage = false;
  enregistrement = false;
  erreur = '';

  ajouter(): void {
    if (this.enregistrement || this.chargementImage) return;
    if (!this.nom.trim() || this.zonageId === null || !Number.isInteger(this.quantiteTotale) || this.quantiteTotale < 1) {
      this.erreur = 'Saisissez le nom du matériel et choisissez un zonage.';
      return;
    }
    this.enregistrement = true;
    this.erreur = '';
    this.service.create(this.nom.trim(), this.zonageId, this.horsService, this.quantiteTotale, this.image).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || response?.error?.message || 'Le matériel n’a pas pu être ajouté.';
      }
    });
  }
}
