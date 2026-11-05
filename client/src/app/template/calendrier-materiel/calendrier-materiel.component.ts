import { Component, DestroyRef, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { MaterielService } from '../../_services/materiel.service';
import { StatutEmpruntService } from '../../_services/statut-emprunt.service';
import { EmpruntCalendrierMateriel, EmpruntMateriel, Materiel } from '../../models/materiel';

interface JourEmprunts {
  iso: string;
  numero: number;
  libelle: string;
  emprunts: EmpruntCalendrierMateriel[];
}

@Component({
  selector: 'app-calendrier-materiel',
  imports: [DatePipe],
  templateUrl: './calendrier-materiel.component.html',
  styleUrls: ['./calendrier-materiel.component.css']
})
export class CalendrierMaterielComponent {
  private readonly service = inject(MaterielService);
  private readonly destroyRef = inject(DestroyRef);
  readonly statutEmprunt = inject(StatutEmpruntService);
  // Une nouvelle liste signale aussi une création, une restitution ou une modification du matériel.
  readonly materiels = input<Materiel[]>([]);
  readonly empruntSelectionne = output<EmpruntCalendrierMateriel>();
  readonly emprunts = signal<EmpruntCalendrierMateriel[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly selection = signal(this.statutEmprunt.dateCourante);
  readonly joursSemaine = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  private requete?: Subscription;

  readonly mois = computed(() => {
    const aujourdHui = this.statutEmprunt.dateCourante;
    const debut = this.date(aujourdHui);
    const entrees = this.emprunts();
    return [0, 1].map(decalage => {
      const premier = new Date(debut.getFullYear(), debut.getMonth() + decalage, 1);
      const nombreJours = new Date(premier.getFullYear(), premier.getMonth() + 1, 0).getDate();
      const jours: (JourEmprunts | null)[] = Array.from({ length: (premier.getDay() + 6) % 7 }, () => null);
      for (let numero = 1; numero <= nombreJours; numero++) {
        const date = new Date(premier.getFullYear(), premier.getMonth(), numero);
        const iso = this.iso(date);
        jours.push({ iso, numero,
          libelle: new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(date),
          emprunts: entrees.filter(entree => this.presentLeJour(entree, iso, aujourdHui))
        });
      }
      while (jours.length % 7) jours.push(null);
      return { iso: this.iso(premier), libelle: new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(premier), jours };
    });
  });

  readonly jourSelectionne = computed(() => this.mois().flatMap(mois => mois.jours)
    .find(jour => jour?.iso === this.selection()) ?? null);

  constructor() {
    effect(() => {
      this.materiels();
      const aujourdHui = this.statutEmprunt.dateCourante;
      untracked(() => {
        if (!this.jourSelectionne()) this.selection.set(aujourdHui);
        this.charger();
      });
    });
  }

  charger(): void {
    const date = this.date(this.statutEmprunt.dateCourante);
    this.requete?.unsubscribe();
    this.chargement.set(true);
    this.erreur.set('');
    this.requete = this.service.getCalendrier(
      this.iso(new Date(date.getFullYear(), date.getMonth(), 1)),
      this.iso(new Date(date.getFullYear(), date.getMonth() + 2, 0))
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: emprunts => {
        this.emprunts.set([...emprunts].sort((a, b) => a.materielNom.localeCompare(b.materielNom, 'fr')
          || (a.emprunt.emprunteurNom ?? '').localeCompare(b.emprunt.emprunteurNom ?? '', 'fr') || a.emprunt.id - b.emprunt.id));
        this.chargement.set(false);
      },
      error: () => {
        this.emprunts.set([]);
        this.erreur.set('Le calendrier des emprunts n’a pas pu être chargé.');
        this.chargement.set(false);
      }
    });
  }

  libelleJour(jour: JourEmprunts): string {
    return `${jour.libelle} : ${jour.emprunts.length} emprunt(s)`;
  }

  statutCalendrier(emprunt: EmpruntMateriel): 'Terminé' | 'En dépassement' | 'Programmé' | 'En cours' {
    const statut = this.statutEmprunt.statut(emprunt);
    if (statut === 'Rapporté') return 'Terminé';
    if (statut === 'À venir') return 'Programmé';
    return emprunt.dateFinPrevue && emprunt.dateFinPrevue < this.statutEmprunt.dateCourante
      ? 'En dépassement' : 'En cours';
  }

  private presentLeJour(entree: EmpruntCalendrierMateriel, jour: string, aujourdHui: string): boolean {
    const emprunt = entree.emprunt;
    if (emprunt.horsService != null) return false;
    const debut = emprunt.dateDebut || this.iso(new Date(emprunt.dateEmprunt));
    // Un emprunt non restitué reste visible jusqu’à aujourd’hui si sa fin prévue est dépassée.
    const fin = emprunt.dateRetour ? this.iso(new Date(emprunt.dateRetour))
      : emprunt.dateFinPrevue ? [emprunt.dateFinPrevue, aujourdHui].sort().at(-1)!
      : '9999-12-31';
    return debut <= jour && jour <= fin;
  }

  private date(iso: string): Date { return new Date(`${iso}T12:00:00`); }
  private iso(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
