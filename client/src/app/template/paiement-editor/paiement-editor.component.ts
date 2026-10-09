import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSquareMinus } from '@fortawesome/free-solid-svg-icons';
import { Paiement } from '../../models/paiement';

export interface EditablePaiement {
  id: number | null;
  montant: number | null;
  dateReglement: string | String | null;
  typeReglement: string | String | null;
  inclusDansPeriode?: boolean;
}

@Component({
  selector: 'app-paiement-editor',
  imports: [FormsModule, NgbDropdownModule, FaIconComponent],
  templateUrl: './paiement-editor.component.html',
  styles: ['input[type="number"] { min-width: 6rem; } input[type="date"] { min-width: 9rem; }']
})
export class PaiementEditorComponent implements OnChanges {
  @Input() paiements: EditablePaiement[] = [];
  @Input() disabled = false;
  @Input() canDelete = false;
  @Input() addAsDraft = false;
  @Input() defaultDate = '';
  @Input() showPeriodStatus = false;
  @Output() save = new EventEmitter<Paiement>();
  @Output() remove = new EventEmitter<number>();
  @Output() add = new EventEmitter<void>();
  readonly faSquareMinus = faSquareMinus;
  readonly types = ['HelloAsso', 'HelloAsso 3X', 'Chèque', 'Chèque 3X', 'Pass sport', 'Espèces', 'CB', 'Intermarché', 'Autre'];
  rows: EditablePaiement[] = [];
  erreur = '';
  private drafts = new Map<EditablePaiement, EditablePaiement>();
  private nouveau: EditablePaiement | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['paiements']) {
      this.rows = [...this.paiements].sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
      this.drafts.clear();
      this.nouveau = null;
      this.erreur = '';
    }
  }

  draft(paiement: EditablePaiement): EditablePaiement {
    if (!this.drafts.has(paiement)) {
      this.drafts.set(paiement, { ...paiement });
    }
    return this.drafts.get(paiement)!;
  }

  savePaiement(paiement: EditablePaiement): void {
    if (this.disabled) return;
    const value = this.draft(paiement);
    const date = String(value.dateReglement ?? '');
    const parsedDate = new Date(`${date}T00:00:00Z`);
    if (value.montant == null || !Number.isInteger(value.montant)
        || value.montant < -2147483648 || value.montant > 2147483647
        || !/^\d{4}-\d{2}-\d{2}$/.test(date) || Number.isNaN(parsedDate.getTime())
        || parsedDate.toISOString().slice(0, 10) !== date || !this.types.includes(String(value.typeReglement))) {
      this.erreur = 'Renseignez un montant entier, une date valide et un moyen de paiement.';
      return;
    }
    this.erreur = '';
    if (paiement !== this.nouveau && value.montant === paiement.montant
        && value.dateReglement === paiement.dateReglement && value.typeReglement === paiement.typeReglement) return;
    this.save.emit(Object.assign(new Paiement(), {
      id: value.id ?? undefined, montant: value.montant,
      dateReglement: date, typeReglement: String(value.typeReglement)
    }));
  }

  selectType(paiement: EditablePaiement, type: string): void {
    this.draft(paiement).typeReglement = type;
    this.savePaiement(paiement);
  }

  addPaiement(): void {
    if (this.disabled) return;
    if (!this.addAsDraft) {
      this.add.emit();
      return;
    }
    if (this.nouveau) return;
    this.nouveau = { id: null, montant: null, dateReglement: this.defaultDate, typeReglement: 'HelloAsso' };
    this.rows = [...this.rows, this.nouveau];
  }

  removePaiement(paiement: EditablePaiement): void {
    if (this.disabled) return;
    if (paiement === this.nouveau) {
      this.rows = this.rows.filter(row => row !== paiement);
      this.drafts.delete(paiement);
      this.nouveau = null;
      this.erreur = '';
    } else if (this.canDelete && paiement.id != null) {
      this.remove.emit(paiement.id);
    }
  }
}
