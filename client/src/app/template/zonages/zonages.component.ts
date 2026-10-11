import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { Zonage } from '../../models/materiel';

@Component({
  selector: 'app-zonages',
  imports: [FormsModule, FaIconComponent],
  templateUrl: './zonages.component.html'
})
export class ZonagesComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly service = inject(MaterielService);
  readonly faTrash = faTrash;
  zonages: Zonage[] = [];
  nom = '';
  couleur = '#0F9D58';
  chargement = true;
  enregistrement = false;
  erreur = '';
  message = '';

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.chargement = true;
    this.erreur = '';
    this.service.getZonages().subscribe({
      next: zonages => { this.zonages = zonages; this.chargement = false; },
      error: () => { this.erreur = 'Les zonages n’ont pas pu être chargés.'; this.chargement = false; }
    });
  }

  ajouter(): void {
    if (this.enregistrement || this.chargement) return;
    if (!this.nom.trim()) { this.erreur = 'Saisissez un nom de zonage.'; return; }
    this.commencer();
    this.service.createZonage(this.nom.trim(), this.couleur).subscribe({
      next: zonage => {
        this.zonages = [...this.zonages, zonage].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.nom = '';
        this.terminer('Zonage ajouté.');
      },
      error: response => this.echouer(response)
    });
  }

  enregistrer(zonage: Zonage): void {
    if (this.enregistrement) return;
    if (!zonage.nom.trim()) { this.erreur = 'Le nom du zonage est obligatoire.'; return; }
    this.commencer();
    this.service.updateZonage(zonage).subscribe({
      next: resultat => {
        this.zonages = this.zonages.map(item => item.id === resultat.id ? resultat : item)
          .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.terminer('Zonage enregistré.');
      },
      error: response => this.echouer(response)
    });
  }

  supprimer(zonage: Zonage): void {
    if (this.enregistrement) return;
    this.commencer();
    this.service.deleteZonage(zonage.id).subscribe({
      next: () => { this.zonages = this.zonages.filter(item => item.id !== zonage.id); this.terminer('Zonage supprimé.'); },
      error: response => this.echouer(response)
    });
  }

  private commencer(): void { this.enregistrement = true; this.erreur = ''; this.message = ''; }
  private terminer(message: string): void { this.enregistrement = false; this.message = message; }
  private echouer(response: any): void {
    this.enregistrement = false;
    this.erreur = response?.error?.detail || response?.error?.message || 'Le zonage n’a pas pu être enregistré.';
  }
}
