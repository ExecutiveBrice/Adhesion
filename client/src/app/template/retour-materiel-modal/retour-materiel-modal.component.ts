import { Component, inject, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Materiel, EmpruntMateriel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-retour-materiel-modal',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="modal-header"><h2 id="retour-materiel-title" class="h5 modal-title">Restituer {{materiel.nom}}</h2><button type="button" class="btn-close" aria-label="Fermer" [disabled]="enregistrement" (click)="activeModal.dismiss()"></button></div>
    <form (ngSubmit)="enregistrer()" #form="ngForm">
      <div class="modal-body">
        <label for="empruntRetour" class="form-label">Emprunt à restituer</label>
        <select id="empruntRetour" name="empruntId" class="form-select mb-3" required [(ngModel)]="empruntId" (ngModelChange)="quantite = restant" [disabled]="enregistrement">
          <option [ngValue]="null">Choisir un emprunt</option>
          @for (emprunt of emprunts; track emprunt.id) { <option [ngValue]="emprunt.id">{{emprunt.emprunteurNom}} — {{emprunt.dateEmprunt | date:'dd/MM/yyyy HH:mm'}} — {{(emprunt.quantite ?? 1) - (emprunt.quantiteRetournee ?? 0)}} à rendre</option> }
        </select>
        <label for="quantiteRetour" class="form-label">Quantité rendue</label>
        <input id="quantiteRetour" name="quantite" class="form-control" type="number" min="1" [max]="restant" step="1" required [(ngModel)]="quantite" [disabled]="enregistrement || empruntId === null">
        @if (erreur) { <div class="alert alert-danger mt-3" role="alert">{{erreur}}</div> }
      </div>
      <div class="modal-footer"><button type="button" class="btn btn-outline-secondary" [disabled]="enregistrement" (click)="activeModal.dismiss()">Annuler</button><button type="submit" class="btn btn-primary" [disabled]="enregistrement || form.invalid || empruntId === null">Enregistrer le retour</button></div>
    </form>`
})
export class RetourMaterielModalComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  private readonly utilisateur = inject(TokenStorageService).getUser();
  @Input() set materiel(value: Materiel) {
    this.objet = value;
    this.emprunts = (value.emprunts ?? []).filter(e => this.utilisateur.roles?.includes('ROLE_ADMIN') || e.emprunteurId === this.utilisateur.id);
    this.empruntId = this.emprunts.length === 1 ? this.emprunts[0].id : null;
    this.quantite = this.restant;
  }
  get materiel(): Materiel { return this.objet; }
  objet!: Materiel;
  emprunts: EmpruntMateriel[] = [];
  empruntId: number | null = null;
  quantite = 1;
  enregistrement = false;
  erreur = '';
  get restant(): number {
    const emprunt = this.emprunts.find(e => e.id === this.empruntId);
    return emprunt ? (emprunt.quantite ?? 1) - (emprunt.quantiteRetournee ?? 0) : 0;
  }
  enregistrer(): void {
    if (this.enregistrement || this.empruntId === null || !Number.isInteger(this.quantite) || this.quantite < 1 || this.quantite > this.restant) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.restituer(this.materiel.id, this.empruntId, this.quantite).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || 'Le retour n’a pas pu être enregistré.';
      }
    });
  }
}
