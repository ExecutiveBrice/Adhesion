import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCheck, faChevronLeft, faChevronRight, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { UserService } from 'src/app/_services/user.service';
import { SeanceDuJour } from 'src/app/models/seance';
import { PresencesSeanceModalComponent } from 'src/app/template/presences-seance-modal/presences-seance-modal.component';

@Component({
  selector: 'app-seances-secretariat',
  templateUrl: './seances-secretariat.component.html',
  styleUrls: ['./seances-secretariat.component.css'],
  imports: [DatePipe, FaIconComponent, NgClass]
})
export class SeancesSecretariatComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly tokenStorageService = inject(TokenStorageService);
  private readonly userService = inject(UserService);
  private readonly modalService = inject(NgbModal);
  private readonly router = inject(Router);

  seances: SeanceDuJour[] = [];
  dateSelectionnee = this.aujourdhui();
  isFailed = false;
  errorMessage = '';
  chargement = false;
  faCheck = faCheck;
  faXmark = faXmark;
  faTriangleExclamation = faTriangleExclamation;
  faChevronLeft = faChevronLeft;
  faChevronRight = faChevronRight;

  ngOnInit(): void {
    const roles = this.tokenStorageService.getUser().roles as string[] | undefined;
    if (!roles) {
      this.router.navigate(['login']);
      return;
    }
    if (!roles.includes('ROLE_SECRETAIRE')) {
      this.router.navigate(['inscription']);
      return;
    }
    this.chargerSeances();
  }

  changerJour(nombreDeJours: number): void {
    const nouvelleDate = new Date(this.dateSelectionnee);
    nouvelleDate.setDate(nouvelleDate.getDate() + nombreDeJours);
    this.dateSelectionnee = nouvelleDate;
    this.chargerSeances();
  }

  allerAujourdhui(): void {
    this.dateSelectionnee = this.aujourdhui();
    this.chargerSeances();
  }

  ouvrirPresences(seance: SeanceDuJour): void {
    const modalRef = this.modalService.open(PresencesSeanceModalComponent, { centered: true, size: 'lg' });
    const modal = modalRef.componentInstance as PresencesSeanceModalComponent;
    modal.seance = seance;
    modal.chargerPresences = (seanceId: number) => this.userService.getPresencesPourLeSecretariat(seanceId);
    modal.mettreAJourPresence = (seanceId: number, presenceId: number, presence: boolean) =>
      this.userService.updatePresencePourLeSecretariat(seanceId, presenceId, presence);
    modal.enregistrerCommentaire = (seanceId: number, commentaire: string | null) =>
      this.userService.updateCommentaireSeancePourLeSecretariat(seanceId, commentaire);
    modal.seanceMiseAJour.subscribe((seanceMiseAJour: SeanceDuJour) => this.remplacerSeance(seanceMiseAJour));
    modal.initialiser();
  }

  private remplacerSeance(seance: SeanceDuJour): void {
    const index = this.seances.findIndex(element => element.id === seance.id);
    if (index >= 0) this.seances[index] = seance;
  }

  libelleEtat(etat: SeanceDuJour['etatSeance']): string {
    return etat === 'REALISEE' ? 'Réalisée' : etat === 'ANNULEE' ? 'Annulée'
      : etat === 'MODIFIEE' ? 'Modifiée' : 'Programmée';
  }

  iconeEtat(etat: SeanceDuJour['etatSeance']) {
    return etat === 'REALISEE' ? this.faCheck : etat === 'ANNULEE' ? this.faXmark
      : etat === 'MODIFIEE' ? this.faTriangleExclamation : null;
  }

  private chargerSeances(): void {
    this.chargement = true;
    this.isFailed = false;
    this.errorMessage = '';
    this.userService.getSeancesDuJourPourLeSecretariat(this.dateParametre()).subscribe({
      next: seances => {
        this.seances = seances;
        this.chargement = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isFailed = true;
        this.errorMessage = error.error?.message || 'Impossible de charger les séances sélectionnées.';
        this.chargement = false;
      }
    });
  }

  private aujourdhui(): Date {
    const date = new Date();
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  private dateParametre(): string {
    const annee = this.dateSelectionnee.getFullYear();
    const mois = String(this.dateSelectionnee.getMonth() + 1).padStart(2, '0');
    const jour = String(this.dateSelectionnee.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }
}
