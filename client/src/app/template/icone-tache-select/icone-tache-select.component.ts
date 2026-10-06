import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { ICONES_TACHES, ICONE_TACHE_PAR_DEFAUT, iconeTache, couleurTache } from '../../_helpers/tache-icons';

@Component({
  selector: 'app-icone-tache-select',
  imports: [FaIconComponent, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem],
  template: `
    <div ngbDropdown container="body">
      <button type="button" class="btn btn-outline-secondary w-100" ngbDropdownToggle [disabled]="disabled" [attr.aria-label]="libelle">
        <fa-icon [icon]="iconeTache(icone)" [style.color]="couleurTache(couleur)" class="me-2" aria-hidden="true"></fa-icon>{{nomSelectionne}}
      </button>
      <div ngbDropdownMenu class="icones-taches-menu">
        @for (choix of choixIcones; track choix.nom) {
          <button type="button" ngbDropdownItem (click)="iconeChange.emit(choix.nom)" [class.active]="icone === choix.nom" [attr.aria-pressed]="icone === choix.nom">
            <fa-icon [icon]="choix.icone" class="me-2" aria-hidden="true"></fa-icon>{{choix.libelle}}
          </button>
        }
      </div>
    </div>
  `,
  styles: [`:host { display: block; } .icones-taches-menu { max-height: 20rem; overflow-y: auto; }`]
})
export class IconeTacheSelectComponent {
  @Input() icone?: string = ICONE_TACHE_PAR_DEFAUT;
  @Input() couleur?: string;
  @Input() disabled = false;
  @Input() libelle = 'Choisir une icône';
  @Output() iconeChange = new EventEmitter<string>();
  readonly choixIcones = ICONES_TACHES;
  readonly iconeTache = iconeTache;
  readonly couleurTache = couleurTache;

  get nomSelectionne(): string {
    return ICONES_TACHES.find(choix => choix.nom === this.icone)?.libelle ?? 'Organisation';
  }
}
