import { RetourMaterielModalComponent } from '../../template/retour-materiel-modal/retour-materiel-modal.component';
import { ChangeDetectorRef, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSliders } from '@fortawesome/free-solid-svg-icons';
import { forkJoin } from 'rxjs';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { StatutEmpruntService } from '../../_services/statut-emprunt.service';
import { ToastService } from '../../_services/toast.service';
import { EmpruntCalendrierMateriel, EmpruntMateriel, Materiel, Zonage } from '../../models/materiel';
import { MaterielModalComponent } from '../../template/materiel-modal/materiel-modal.component';
import { CreationEmpruntMateriel, EmpruntMaterielModalComponent } from '../../template/emprunt-materiel-modal/emprunt-materiel-modal.component';
import { HistoriqueMaterielModalComponent, OperationReservation } from '../../template/historique-materiel-modal/historique-materiel-modal.component';
import { ModificationMateriel, ModifierMaterielModalComponent } from '../../template/modifier-materiel-modal/modifier-materiel-modal.component';
import { HorsServiceMaterielModalComponent } from '../../template/hors-service-materiel-modal/hors-service-materiel-modal.component';
import { ZonagesModalComponent } from '../../template/zonages-modal/zonages-modal.component';
import { CalendrierMaterielComponent } from '../../template/calendrier-materiel/calendrier-materiel.component';

@Component({
  selector: 'app-materiel',
  imports: [FormsModule, FaIconComponent, CalendrierMaterielComponent],
  templateUrl: './materiel.component.html',
  styleUrls: ['./materiel.component.css']
})
export class MaterielComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly service = inject(MaterielService);
  private readonly toast = inject(ToastService);
  private readonly storage = inject(TokenStorageService);
  private readonly utilisateur: { id?: number; roles?: string[] } = this.storage.getUser();
  private readonly modal = inject(NgbModal);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly rechercheMateriel = viewChild<ElementRef<HTMLInputElement>>('rechercheMateriel');
  readonly statutEmprunt = inject(StatutEmpruntService);
  readonly gestionMateriel = this.utilisateur.roles?.includes('ROLE_LOGISTIQUE') ?? false;
  readonly peutGererEmpruntsEtHorsService = this.utilisateur.roles?.some(role => role === 'ROLE_ADMIN' || role === 'ROLE_MEMBRECA') ?? false;
  readonly faSliders = faSliders;
  materiels: Materiel[] = [];
  zonages: Zonage[] = [];
  chargement = true;
  actionEnCours: number | null = null;
  erreur = '';
  recherche = '';
  zonageId: number | null = null;
  statut = '';

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.chargement = true;
    this.erreur = '';
    forkJoin({ materiels: this.service.getAll(), zonages: this.service.getZonages() }).subscribe({
      next: data => {
        this.materiels = data.materiels;
        this.zonages = data.zonages;
        if (this.zonageId !== null && !this.zonages.some(zone => zone.id === this.zonageId)) this.zonageId = null;
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.erreur = 'Le matériel n’a pas pu être chargé. Réessayez.';
      }
    });
  }

  get materielsFiltres(): Materiel[] {
    const recherche = this.recherche.trim().toLocaleLowerCase('fr');
    return this.materiels.filter(materiel =>
      (!this.zonageId || materiel.zonage.id === this.zonageId)
      && (!this.statut || (this.statut === 'disponible' && (materiel.quantiteDisponible ?? 0) > 0)
        || (this.statut === 'emprunte' && this.statutEmprunt.quantite(materiel, 'En cours') > 0)
        || (this.statut === 'a-venir' && this.statutEmprunt.quantite(materiel, 'À venir') > 0))
      && `${materiel.nom} ${materiel.zonage.nom} ${(materiel.emprunts ?? []).map(e => e.emprunteurNom).join(' ')}`.toLocaleLowerCase('fr').includes(recherche));
  }

  visualiserEmprunt(entree: EmpruntCalendrierMateriel): void {
    this.recherche = entree.materielNom;
    this.zonageId = null;
    this.statut = '';
    this.rechercheMateriel()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  annulerReservationCalendrier({ materielId, materielNom, emprunt }: EmpruntCalendrierMateriel): void {
    if (this.statutEmprunt.statut(emprunt) !== 'À venir' || !this.statutEmprunt.peutGerer(emprunt, this.utilisateur)) return;
    this.historique({ id: materielId, nom: materielNom }, emprunt);
  }

  restituerEmpruntCalendrier({ materielId, materielNom, emprunt }: EmpruntCalendrierMateriel): void {
    this.restituer({ id: materielId, nom: materielNom, emprunts: [emprunt] });
  }

  peutRestituer(materiel: Pick<Materiel, 'emprunts'>): boolean {
    return (materiel.emprunts ?? []).some(emprunt =>
      this.statutEmprunt.statut(emprunt) === 'En cours' && this.statutEmprunt.peutGerer(emprunt, this.utilisateur));
  }

  emprunter(materiel: Materiel): void {
    if (!this.peutGererEmpruntsEtHorsService || this.actionEnCours !== null) return;
    this.ouvrirOperation(materiel, false);
  }

  restituer(materiel: Pick<Materiel, 'id' | 'nom' | 'emprunts'>): void {
    if (!this.peutRestituer(materiel) || this.actionEnCours !== null) return;
    this.ouvrirOperation(materiel, true);
  }

  private ouvrirOperation(materiel: Pick<Materiel, 'id' | 'nom' | 'emprunts'>, retour: boolean): void {
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(retour ? RetourMaterielModalComponent : EmpruntMaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: retour ? 'retour-materiel-title' : 'emprunt-materiel-title',
      size: retour ? undefined : 'lg', scrollable: true,
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.result.then((resultat: Materiel | CreationEmpruntMateriel) => {
      const materielModifie = 'materiel' in resultat ? resultat.materiel : resultat;
      this.materiels = this.materiels.map(item => item.id === materielModifie.id ? materielModifie : item);
      this.actionEnCours = null;
      const message = retour ? 'Retour enregistré.' : 'reservation' in resultat && resultat.reservation
        ? 'Réservation enregistrée.' : 'Emprunt enregistré.';
      this.toast.success(message, 'Matériel');
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  historique(materiel: Pick<Materiel, 'id' | 'nom'>, reservation?: EmpruntMateriel): void {
    if (this.actionEnCours !== null) return;
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(HistoriqueMaterielModalComponent, {
      centered: true, scrollable: true, ariaLabelledBy: 'historique-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.componentInstance.reservation = reservation;
    reference.result.then(({ materiel, message }: OperationReservation) => {
      this.materiels = this.materiels.map(item => item.id === materiel.id ? materiel : item);
      this.actionEnCours = null;
      this.toast.success(message, 'Matériel');
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  changerHorsService(materiel: Materiel): void {
    if (!this.peutGererEmpruntsEtHorsService || this.actionEnCours !== null) return;
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(HorsServiceMaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: 'hors-service-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.result.then((resultat: Materiel) => {
      this.materiels = this.materiels.map(item => item.id === resultat.id ? resultat : item);
      this.actionEnCours = null;
      this.toast.success('État du matériel enregistré.', 'Matériel');
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  parametrer(): void {
    if (!this.gestionMateriel || this.chargement || this.actionEnCours !== null) return;
    const reference = this.modal.open(MaterielModalComponent, {
      centered: true, scrollable: true, backdrop: 'static', ariaLabelledBy: 'materiel-modal-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.zonages = this.zonages;
    reference.result.then((materiel: Materiel) => {
      this.materiels = [...this.materiels, materiel].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
      this.toast.success('Matériel ajouté.', 'Matériel');
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => {});
  }

  modifier(materiel: Materiel): void {
    if (!this.gestionMateriel || this.chargement || this.actionEnCours !== null) return;
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(ModifierMaterielModalComponent, {
      centered: true, scrollable: true, backdrop: 'static', ariaLabelledBy: 'modifier-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.componentInstance.zonages = this.zonages;
    reference.result.then((resultat: ModificationMateriel) => {
      if ('supprimeId' in resultat) {
        this.materiels = this.materiels.filter(item => item.id !== resultat.supprimeId);
        this.toast.success('Matériel supprimé.', 'Matériel');
      } else {
        this.materiels = this.materiels.map(item => item.id === resultat.materiel.id ? resultat.materiel : item)
          .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.toast.success('Matériel modifié.', 'Matériel');
      }
      this.erreur = '';
      this.actionEnCours = null;
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  gererZonages(): void {
    if (!this.gestionMateriel || this.chargement || this.actionEnCours !== null) return;
    const reference = this.modal.open(ZonagesModalComponent, {
      centered: true, scrollable: true, size: 'lg', backdrop: 'static', ariaLabelledBy: 'zonages-modal-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    // Les changements sont enregistrés dans la modale, même si elle est fermée avec Échap ou la croix.
    const actualiser = () => { this.charger(); this.changeDetector.markForCheck(); };
    reference.result.then(actualiser, actualiser);
  }
}
