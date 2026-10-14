import { Component, inject, Input, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { EmpruntMateriel, Materiel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-historique-materiel-modal',
  imports: [DatePipe],
  templateUrl: './historique-materiel-modal.component.html',
  styles: ['dd, h3 { overflow-wrap: anywhere; }']
})
export class HistoriqueMaterielModalComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  @Input() materiel!: Materiel;
  emprunts: EmpruntMateriel[] = [];
  chargement = true;
  erreur = '';
  ngOnInit(): void { this.charger(); }
  charger(): void {
    this.chargement = true;
    this.erreur = '';
    this.service.getHistorique(this.materiel.id).subscribe({
      next: emprunts => { this.emprunts = emprunts; this.chargement = false; },
      error: () => { this.chargement = false; this.erreur = 'Le journal des emprunts n’a pas pu être chargé.'; }
    });
  }
}
