import { Component, Input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { ResponsabiliteSeance } from '../../models/responsabiliteSeance';
import { iconeTache, couleurTache } from '../../_helpers/tache-icons';

@Component({
  selector: 'app-responsabilites-seance',
  imports: [FaIconComponent],
  template: `
    @for (responsabilite of responsabilites; track $index) {
      <span class="responsabilite-seance" [class.responsabilite-detail]="details" [title]="libelle(responsabilite)"
        [attr.role]="details ? null : 'img'" [attr.aria-label]="details ? null : libelle(responsabilite)">
        <fa-icon class="responsabilite-icone" [icon]="iconeTache(responsabilite.tache.icone)"
          [style.color]="couleurTache(responsabilite.tache.couleur)" aria-hidden="true"></fa-icon>
        @if (details) { <span>{{libelle(responsabilite)}}</span> }
      </span>
    }
  `,
  styles: [`
    :host { display: flex; flex-wrap: wrap; gap: .25rem; }
    .responsabilite-seance { display: inline-flex; align-items: center; gap: .5rem; min-width: 0; }
    .responsabilite-icone { display: inline-flex; align-items: center; justify-content: center; background: white;
      border-radius: .2rem; width: 1.2rem; height: 1.2rem; flex-shrink: 0; font-size: .85rem; }
    .responsabilite-detail { flex-basis: 100%; overflow-wrap: anywhere; }
    @media (max-width: 575.98px) { .responsabilite-icone { width: 14px; height: 14px; font-size: 10px; } }
  `]
})
export class ResponsabilitesSeanceComponent {
  @Input() responsabilites?: ResponsabiliteSeance[] = [];
  @Input() details = false;
  readonly iconeTache = iconeTache;
  readonly couleurTache = couleurTache;

  libelle(responsabilite: ResponsabiliteSeance): string {
    return `${responsabilite.tache.nom} : ${responsabilite.adherent.prenom} ${responsabilite.adherent.nom}`;
  }
}
