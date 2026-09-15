import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { UtilisateurSelectionnable } from 'src/app/models/utilisateurSelectionnable';

@Component({
  selector: 'app-user-checkbox-dropdown',
  templateUrl: './user-checkbox-dropdown.component.html',
  styleUrls: ['./user-checkbox-dropdown.component.css'],
  imports: [NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem]
})
export class UserCheckboxDropdownComponent {
  private static nextId = 0;

  @Input() utilisateurs: UtilisateurSelectionnable[] = [];
  @Input() selection: UtilisateurSelectionnable[] = [];
  @Input() libelle = 'Sélectionner des utilisateurs';
  @Input() disabled = false;
  @Input() menuContainer: 'body' | null = null;
  @Output() selectionChange = new EventEmitter<UtilisateurSelectionnable[]>();

  readonly dropdownId = `user-checkbox-dropdown-${UserCheckboxDropdownComponent.nextId++}`;
  recherche = '';

  get utilisateursTries(): UtilisateurSelectionnable[] {
    const recherche = this.normaliser(this.recherche);
    return this.utilisateurs.filter(utilisateur => !recherche
      || this.normaliser(utilisateur.nom).includes(recherche)
      || this.normaliser(utilisateur.prenom).includes(recherche)
    ).sort((a, b) => {
      const ecartSelection = Number(this.estSelectionne(b)) - Number(this.estSelectionne(a));
      return ecartSelection || `${a.nom} ${a.prenom}`.localeCompare(`${b.nom} ${b.prenom}`, 'fr');
    });
  }

  estSelectionne(utilisateur: UtilisateurSelectionnable): boolean {
    return this.selection.some(selectionne => selectionne.id === utilisateur.id);
  }

  modifierSelection(utilisateur: UtilisateurSelectionnable, coche: boolean): void {
    const selection = this.selection.filter(selectionne => selectionne.id !== utilisateur.id);
    if (coche) {
      selection.push(utilisateur);
    }
    this.selectionChange.emit(selection);
  }

  private normaliser(valeur: string): string {
    return valeur.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('fr');
  }
}
