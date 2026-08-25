import { Component, OnInit, TemplateRef, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCheck, faChevronLeft, faChevronRight, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { UserService } from 'src/app/_services/user.service';
import { PresenceSeance, SeanceDuJour } from 'src/app/models/seance';

@Component({
  selector: 'app-seances-secretariat',
  templateUrl: './seances-secretariat.component.html',
  styleUrls: ['./seances-secretariat.component.css'],
  imports: [DatePipe, FaIconComponent, FormsModule, NgClass]
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
  seanceSelectionnee?: SeanceDuJour;
  presences: PresenceSeance[] = [];
  chargementPresences = false;
  erreurPresences = '';
  miseAJourPresences = new Set<number>();
  enregistrementCommentaire = false;
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

  ouvrirPresences(seance: SeanceDuJour, contenu: TemplateRef<unknown>): void {
    this.seanceSelectionnee = seance;
    this.presences = [];
    this.erreurPresences = '';
    this.chargementPresences = true;
    this.modalService.open(contenu, { centered: true, size: 'lg' });
    this.userService.getPresencesPourLeSecretariat(seance.id).subscribe({
      next: presences => {
        this.presences = presences;
        this.chargementPresences = false;
      },
      error: (error: HttpErrorResponse) => {
        this.erreurPresences = error.error?.message || 'Impossible de charger les présences.';
        this.chargementPresences = false;
      }
    });
  }

  changerPresence(presence: PresenceSeance): void {
    if (!this.seanceSelectionnee) return;
    const valeurPrecedente = presence.presence;
    presence.presence = !valeurPrecedente;
    this.miseAJourPresences.add(presence.id);
    this.userService.updatePresencePourLeSecretariat(this.seanceSelectionnee.id, presence.id, presence.presence).subscribe({
      next: miseAJour => {
        presence.presence = miseAJour.presence;
        this.miseAJourPresences.delete(presence.id);
      },
      error: () => {
        presence.presence = valeurPrecedente;
        this.miseAJourPresences.delete(presence.id);
        this.erreurPresences = 'Impossible d’enregistrer la présence.';
      }
    });
  }

  enregistrerCommentaire(): void {
    if (!this.seanceSelectionnee) return;
    this.enregistrementCommentaire = true;
    this.userService.updateCommentaireSeancePourLeSecretariat(this.seanceSelectionnee.id, this.seanceSelectionnee.commentaire).subscribe({
      next: seance => {
        this.seanceSelectionnee = seance;
        const index = this.seances.findIndex(element => element.id === seance.id);
        if (index >= 0) this.seances[index] = seance;
        this.enregistrementCommentaire = false;
      },
      error: () => {
        this.erreurPresences = 'Impossible d’enregistrer le commentaire.';
        this.enregistrementCommentaire = false;
      }
    });
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
