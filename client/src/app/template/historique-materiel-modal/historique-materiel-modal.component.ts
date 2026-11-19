import { Component, inject, Input, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { EmpruntMateriel, Materiel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { StatutEmpruntService } from '../../_services/statut-emprunt.service';
import { TokenStorageService } from '../../_services/token-storage.service';

export interface OperationReservation {
  materiel: Materiel;
  message: string;
}

@Component({
  selector: 'app-historique-materiel-modal',
  imports: [DatePipe],
  templateUrl: './historique-materiel-modal.component.html',
  styles: ['dd, h3 { overflow-wrap: anywhere; }']
})
export class HistoriqueMaterielModalComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  readonly statutEmprunt = inject(StatutEmpruntService);
  private readonly service = inject(MaterielService);
  private readonly utilisateur = inject(TokenStorageService).getUser();
  @Input() materiel!: Pick<Materiel, 'id' | 'nom'>;
  @Input() reservation?: EmpruntMateriel;
  emprunts: EmpruntMateriel[] = [];
  chargement = true;
  erreur = '';
  enregistrement = false;
  reservationAAnnuler: number | null = null;
  erreurReservation = '';
  ngOnInit(): void {
    if (this.reservation) {
      this.emprunts = [this.reservation];
      this.chargement = false;
      this.demanderAnnulation(this.reservation);
    } else {
      this.charger();
    }
  }

  charger(): void {
    this.chargement = true;
    this.erreur = '';
    this.service.getHistorique(this.materiel.id).subscribe({
      next: emprunts => { this.emprunts = emprunts; this.chargement = false; },
      error: () => { this.chargement = false; this.erreur = 'Le journal des emprunts n’a pas pu être chargé.'; }
    });
  }

  peutGererReservation(emprunt: EmpruntMateriel): boolean {
    return this.statutEmprunt.statut(emprunt) === 'À venir'
      && this.statutEmprunt.peutGerer(emprunt, this.utilisateur);
  }

  peutCommencerReservation(emprunt: EmpruntMateriel): boolean {
    return this.peutGererReservation(emprunt) && !!emprunt.dateDebut
      && emprunt.dateDebut <= this.statutEmprunt.dateCourante;
  }

  demanderAnnulation(emprunt: EmpruntMateriel): void {
    if (this.enregistrement || !this.peutGererReservation(emprunt)) return;
    this.erreurReservation = '';
    this.reservationAAnnuler = emprunt.id;
  }

  conserverReservation(): void {
    if (this.reservation) this.activeModal.dismiss();
    else this.reservationAAnnuler = null;
  }

  enregistrerReservation(emprunt: EmpruntMateriel, annulation: boolean): void {
    if (this.enregistrement || (annulation
      ? this.reservationAAnnuler !== emprunt.id || !this.peutGererReservation(emprunt)
      : !this.peutCommencerReservation(emprunt))) return;

    this.enregistrement = true;
    this.erreurReservation = '';
    const requete = annulation ? this.service.annulerReservation(this.materiel.id, emprunt.id)
      : this.service.commencerReservation(this.materiel.id, emprunt.id);
    const message = annulation ? 'Réservation annulée.' : 'Emprunt enregistré.';
    requete.subscribe({
      next: materiel => this.activeModal.close({ materiel, message } satisfies OperationReservation),
      error: response => {
        this.enregistrement = false;
        this.erreurReservation = response?.error?.detail
          || (annulation ? 'La réservation n’a pas pu être annulée.' : 'L’emprunt n’a pas pu être enregistré.');
      }
    });
  }
}
