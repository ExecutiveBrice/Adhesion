import { RetourMaterielModalComponent } from '../../template/retour-materiel-modal/retour-materiel-modal.component';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSliders } from '@fortawesome/free-solid-svg-icons';
import { forkJoin } from 'rxjs';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { Materiel, Zonage } from '../../models/materiel';
import { MaterielModalComponent } from '../../template/materiel-modal/materiel-modal.component';
import { EmpruntMaterielModalComponent } from '../../template/emprunt-materiel-modal/emprunt-materiel-modal.component';
import { HistoriqueMaterielModalComponent } from '../../template/historique-materiel-modal/historique-materiel-modal.component';
import { ModificationMateriel, ModifierMaterielModalComponent } from '../../template/modifier-materiel-modal/modifier-materiel-modal.component';
import { HorsServiceMaterielModalComponent } from '../../template/hors-service-materiel-modal/hors-service-materiel-modal.component';
import { ZonagesModalComponent } from '../../template/zonages-modal/zonages-modal.component';

@Component({
  selector: 'app-materiel',
  imports: [DatePipe, FormsModule, FaIconComponent],
  templateUrl: './materiel.component.html',
  styleUrls: ['./materiel.component.css']
})
export class MaterielComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly service = inject(MaterielService);
  private readonly storage = inject(TokenStorageService);
  private readonly modal = inject(NgbModal);
  private readonly changeDetector = inject(ChangeDetectorRef);
  readonly administrateur = this.storage.getUser().roles?.includes('ROLE_ADMIN') ?? false;
  readonly gestionMateriel = this.administrateur || (this.storage.getUser().roles?.includes('ROLE_LOGISTIQUE') ?? false);
  readonly peutEmprunter = this.administrateur || (this.storage.getUser().roles?.includes('ROLE_MEMBRECA') ?? false);
  readonly utilisateurId: number = this.storage.getUser().id;
  readonly faSliders = faSliders;
  materiels: Materiel[] = [];
  zonages: Zonage[] = [];
  chargement = true;
  actionEnCours: number | null = null;
  erreur = '';
  message = '';
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
        || (this.statut === 'emprunte' && (materiel.quantiteEmpruntee ?? 0) > 0))
      && `${materiel.nom} ${materiel.zonage.nom} ${(materiel.emprunts ?? []).map(e => e.emprunteurNom).join(' ')}`.toLocaleLowerCase('fr').includes(recherche));
  }

  peutRestituer(materiel: Materiel): boolean {
    return this.peutEmprunter && (materiel.emprunts ?? []).some(e => this.administrateur || e.emprunteurId === this.utilisateurId);
  }

  emprunter(materiel: Materiel): void {
    if (!this.peutEmprunter || (materiel.quantiteDisponible ?? 0) < 1 || this.actionEnCours !== null) return;
    this.ouvrirOperation(materiel, false);
  }

  restituer(materiel: Materiel): void {
    if (!this.peutRestituer(materiel) || this.actionEnCours !== null) return;
    this.ouvrirOperation(materiel, true);
  }

  private ouvrirOperation(materiel: Materiel, retour: boolean): void {
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(retour ? RetourMaterielModalComponent : EmpruntMaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: retour ? 'retour-materiel-title' : 'emprunt-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.result.then((resultat: Materiel) => {
      this.materiels = this.materiels.map(item => item.id === resultat.id ? resultat : item);
      this.actionEnCours = null;
      this.message = retour ? 'Retour enregistré.' : 'Emprunt enregistré.';
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  historique(materiel: Materiel): void {
    const reference = this.modal.open(HistoriqueMaterielModalComponent, { centered: true, scrollable: true, ariaLabelledBy: 'historique-materiel-title' });
    reference.componentInstance.materiel = materiel;
    reference.result.catch(() => {});
  }

  changerHorsService(materiel: Materiel): void {
    if (!this.administrateur || this.actionEnCours !== null) return;
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(HorsServiceMaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: 'hors-service-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.result.then((resultat: Materiel) => {
      this.materiels = this.materiels.map(item => item.id === resultat.id ? resultat : item);
      this.actionEnCours = null;
      this.message = 'État du matériel enregistré.';
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => { this.actionEnCours = null; this.changeDetector.markForCheck(); });
  }

  parametrer(): void {
    if (!this.gestionMateriel || this.chargement || this.actionEnCours !== null) return;
    const reference = this.modal.open(MaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: 'materiel-modal-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.zonages = this.zonages;
    reference.result.then((materiel: Materiel) => {
      this.materiels = [...this.materiels, materiel].sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
      this.message = 'Matériel ajouté.';
      this.erreur = '';
      this.changeDetector.markForCheck();
    }, () => {});
  }

  modifier(materiel: Materiel): void {
    if (!this.gestionMateriel || this.chargement || this.actionEnCours !== null) return;
    this.actionEnCours = materiel.id;
    const reference = this.modal.open(ModifierMaterielModalComponent, {
      centered: true, backdrop: 'static', ariaLabelledBy: 'modifier-materiel-title',
      beforeDismiss: () => !reference.componentInstance.enregistrement
    });
    reference.componentInstance.materiel = materiel;
    reference.componentInstance.zonages = this.zonages;
    reference.result.then((resultat: ModificationMateriel) => {
      if ('supprimeId' in resultat) {
        this.materiels = this.materiels.filter(item => item.id !== resultat.supprimeId);
        this.message = 'Matériel supprimé.';
      } else {
        this.materiels = this.materiels.map(item => item.id === resultat.materiel.id ? resultat.materiel : item)
          .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
        this.message = 'Matériel modifié.';
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
