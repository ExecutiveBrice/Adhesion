import { Component, inject, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Materiel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-hors-service-materiel-modal',
  imports: [FormsModule],
  templateUrl: './hors-service-materiel-modal.component.html'
})
export class HorsServiceMaterielModalComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  @Input() set materiel(value: Materiel) {
    this.objet = value;
    this.quantiteHorsService = value.quantiteHorsService ?? (value.horsService ? 1 : 0);
  }
  get materiel(): Materiel { return this.objet; }
  objet!: Materiel;
  quantiteHorsService = 0;
  commentaire = '';
  enregistrement = false;
  erreur = '';

  enregistrer(): void {
    const commentaire = this.commentaire.trim();
    if (this.enregistrement || !commentaire || commentaire.length > 2000 || !Number.isInteger(this.quantiteHorsService) || this.quantiteHorsService < 0 || this.quantiteHorsService > ((this.materiel.quantiteTotale ?? 1) - (this.materiel.quantiteEmpruntee ?? 0))) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.updateHorsService(this.materiel.id, this.quantiteHorsService > 0, commentaire, this.quantiteHorsService).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || response?.error?.message || 'Le statut n’a pas pu être enregistré.';
      }
    });
  }
}
