import { Component, inject, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MembreCAMateriel, Materiel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-emprunt-materiel-modal',
  imports: [FormsModule],
  templateUrl: './emprunt-materiel-modal.component.html'
})
export class EmpruntMaterielModalComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  private readonly storage = inject(TokenStorageService);
  @Input() materiel!: Materiel;
  readonly administrateur = this.storage.getUser().roles?.includes('ROLE_ADMIN') ?? false;
  quantite = 1;
  membresCA: MembreCAMateriel[] = [];
  emprunteurId: number | null = this.storage.getUser().id ?? null;
  chargement = true;
  enregistrement = false;
  erreur = '';

  ngOnInit(): void { if (this.administrateur) this.charger(); else this.chargement = false; }

  charger(): void {
    this.chargement = true;
    this.membresCA = [];
    this.erreur = '';
    this.service.getMembresCA().subscribe({
      next: membresCA => {
        this.membresCA = membresCA;
        if (!membresCA.some(membre => membre.id === this.emprunteurId)) this.emprunteurId = null;
        this.chargement = false;
      },
      error: () => { this.chargement = false; this.erreur = 'La liste des membres du conseil d’administration n’a pas pu être chargée.'; }
    });
  }

  emprunter(): void {
    if (this.enregistrement || this.chargement || this.emprunteurId === null
        || (this.administrateur && !this.membresCA.some(membre => membre.id === this.emprunteurId))
        || !Number.isInteger(this.quantite) || this.quantite < 1 || this.quantite > (this.materiel.quantiteDisponible ?? 1)) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.emprunter(this.materiel.id, this.administrateur ? this.emprunteurId! : undefined, this.quantite).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || response?.error?.message || 'L’emprunt n’a pas pu être enregistré.';
      }
    });
  }
}
