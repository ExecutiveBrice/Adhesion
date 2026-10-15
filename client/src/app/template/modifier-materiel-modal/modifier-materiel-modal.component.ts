import { Component, inject, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { Materiel, Zonage } from '../../models/materiel';

export type ModificationMateriel = { materiel: Materiel } | { supprimeId: number };

@Component({
  selector: 'app-modifier-materiel-modal',
  imports: [FormsModule],
  templateUrl: './modifier-materiel-modal.component.html'
})
export class ModifierMaterielModalComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  @Input() zonages: Zonage[] = [];
  @Input() set materiel(value: Materiel) {
    this.objet = value;
    this.quantiteTotale = value.quantiteTotale ?? 1;
    this.nom = value.nom;
    this.zonageId = value.zonage.id;
  }
  objet!: Materiel;
  nom = '';
  quantiteTotale = 1;
  zonageId: number | null = null;
  enregistrement = false;
  confirmationSuppression = false;
  erreur = '';

  enregistrer(): void {
    if (this.enregistrement || this.confirmationSuppression) return;
    if (!this.nom.trim() || this.nom.trim().length > 150 || this.zonageId === null || !Number.isInteger(this.quantiteTotale) || this.quantiteTotale < 1) {
      this.erreur = 'Saisissez le nom du matériel et choisissez un zonage.';
      return;
    }
    this.enregistrement = true;
    this.erreur = '';
    this.service.update(this.objet.id, this.nom.trim(), this.zonageId, this.quantiteTotale).subscribe({
      next: materiel => this.activeModal.close({ materiel } satisfies ModificationMateriel),
      error: response => this.echec(response, 'Le matériel n’a pas pu être modifié.')
    });
  }

  supprimer(): void {
    if (this.enregistrement || !this.confirmationSuppression || (this.objet.quantiteEmpruntee ?? (this.objet.emprunteurId !== null ? 1 : 0)) > 0) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.delete(this.objet.id).subscribe({
      next: () => this.activeModal.close({ supprimeId: this.objet.id } satisfies ModificationMateriel),
      error: response => this.echec(response, 'Le matériel n’a pas pu être supprimé.')
    });
  }

  private echec(response: any, message: string): void {
    this.enregistrement = false;
    this.erreur = response?.error?.detail || response?.error?.message || message;
  }
}
