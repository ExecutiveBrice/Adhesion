import { DatePipe, NgClass } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { ActiviteService } from 'src/app/_services/activite.service';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { SeanceCalendrier } from 'src/app/models/seance';

interface ActiviteAvecEvenements {
  id: number;
  nom: string;
  evenements: SeanceCalendrier[];
}

@Component({
  selector: 'app-prochains-evenements-famille',
  templateUrl: './prochains-evenements-famille.component.html',
  styleUrls: ['./prochains-evenements-famille.component.css'],
  imports: [DatePipe, NgClass]
})
export class ProchainsEvenementsFamilleComponent implements OnChanges {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private activiteService = inject(ActiviteService);

  @Input() tribuUuid?: string;

  activites: ActiviteAvecEvenements[] = [];
  chargement = false;
  erreur = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tribuUuid']) {
      this.chargerEvenements();
    }
  }

  classeEtat(etat: SeanceCalendrier['etatSeance']): string {
    return `etat-${etat.toLowerCase()}`;
  }

  libelleEtat(etat: SeanceCalendrier['etatSeance']): string {
    switch (etat) {
      case 'ANNULEE':
        return 'Annulée';
      case 'MODIFIEE':
        return 'Modifiée';
      case 'REALISEE':
        return 'Réalisée';
      default:
        return 'Programmée';
    }
  }

  lienActivite(evenement: SeanceCalendrier): string | null {
    const lien = evenement.lien?.trim();
    if (!lien) {
      return null;
    }
    return /^https?:\/\//i.test(lien) ? lien : `https://${lien}`;
  }

  lienAdresseSalle(evenement: SeanceCalendrier): string | null {
    const adresse = evenement.adresseSalle?.trim();
    return adresse ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(adresse)}` : null;
  }

  libelleLieu(evenement: SeanceCalendrier): string {
    return [evenement.salle, evenement.adresseSalle]
      .map(valeur => valeur?.trim())
      .filter((valeur): valeur is string => !!valeur)
      .join(' — ');
  }

  private chargerEvenements(): void {
    if (!this.tribuUuid) {
      this.activites = [];
      this.chargement = false;
      this.erreur = '';
      return;
    }

    const maintenant = new Date();
    const dateFin = new Date(maintenant);
    dateFin.setDate(dateFin.getDate() + 365);

    this.chargement = true;
    this.erreur = '';
    this.activiteService.getCalendrier(this.dateIso(maintenant), this.dateIso(dateFin), this.tribuUuid).subscribe({
      next: evenements => {
        this.activites = this.regrouperEvenements(evenements, maintenant);
        this.chargement = false;
      },
      error: () => {
        this.activites = [];
        this.erreur = "Les prochains événements ne sont pas disponibles pour le moment.";
        this.chargement = false;
      }
    });
  }

  private regrouperEvenements(evenements: SeanceCalendrier[], maintenant: Date): ActiviteAvecEvenements[] {
    const activites = new Map<number, ActiviteAvecEvenements>();

    for (const evenement of evenements) {
      const debut = new Date(evenement.debut);
      if (Number.isNaN(debut.getTime()) || debut < maintenant || evenement.etatSeance === 'ANNULEE') {
        continue;
      }

      const activite = activites.get(evenement.activiteId) ?? {
        id: evenement.activiteId,
        nom: evenement.activiteNom,
        evenements: []
      };
      activite.evenements.push(evenement);
      activites.set(evenement.activiteId, activite);
    }

    return [...activites.values()]
      .map(activite => ({
        ...activite,
        evenements: activite.evenements
          .sort((premier, second) => new Date(premier.debut).getTime() - new Date(second.debut).getTime())
          .slice(0, 5)
      }))
      .sort((premiere, seconde) => premiere.evenements[0].debut.localeCompare(seconde.evenements[0].debut));
  }

  private dateIso(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
