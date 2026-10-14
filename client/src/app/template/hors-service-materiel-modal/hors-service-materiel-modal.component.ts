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
  @Input() materiel!: Materiel;
  commentaire = '';
  enregistrement = false;
  erreur = '';

  enregistrer(): void {
    const commentaire = this.commentaire.trim();
    if (this.enregistrement || !commentaire || commentaire.length > 2000) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.updateHorsService(this.materiel.id, !this.materiel.horsService, commentaire).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || response?.error?.message || 'Le statut n’a pas pu être enregistré.';
      }
    });
  }
}
