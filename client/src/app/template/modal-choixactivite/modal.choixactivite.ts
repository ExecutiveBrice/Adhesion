import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ActiviteDropDown } from 'src/app/models';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
import { OrderByPipe } from '../../_helpers/sort.pipe';

interface GroupeActivites {
  nom: string;
  activites: ActiviteDropDown[];
}

@Component({
    selector: 'modal',
    templateUrl: './modal.choixactivite.html',
    styleUrls: ['./modal.choixactivite.css'],
    imports: [OrderByPipe]
})
export class ModalChoixActivite {
	activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt=faExternalLinkSquareAlt;
  @Input()
  activites!: ActiviteDropDown[];
  @Input()
  secretaire!: boolean;
  @Input()
  admin!: boolean;

  /**
   * Regroupe les activités pour l'affichage sans modifier la liste transmise
   * par le composant parent.
   */
  get groupesActivites(): GroupeActivites[] {
    const groupes = new Map<string, ActiviteDropDown[]>();

    for (const activite of this.activites ?? []) {
      const nomGroupe = activite.groupeFiltre?.trim() || 'Sans groupe';
      const activitesDuGroupe = groupes.get(nomGroupe) ?? [];
      activitesDuGroupe.push(activite);
      groupes.set(nomGroupe, activitesDuGroupe);
    }

    const collator = new Intl.Collator('fr', { sensitivity: 'base' });
    return [...groupes.entries()]
      .map(([nom, activites]) => ({
        nom,
        activites: [...activites].sort((a, b) => collator.compare(a.nom, b.nom))
      }))
      .sort((a, b) => collator.compare(a.nom, b.nom));
  }

  opennewTab(page : string){

    window.open(page, '_blank');
  }


}
