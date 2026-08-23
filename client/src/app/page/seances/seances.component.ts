import { Component, OnInit, TemplateRef, inject } from '@angular/core';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { PresenceSeance, SeanceDuJour } from 'src/app/models/seance';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { UserService } from 'src/app/_services/user.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { faCheck, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NgClass, DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-seances',
    templateUrl: './seances.component.html',
    styleUrls: ['./seances.component.css'],
    imports: [NgClass, FaIconComponent, FormsModule, DatePipe]
})
export class SeancesComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private tokenStorageService = inject(TokenStorageService);
  private userService = inject(UserService);
  private modalService = inject(NgbModal);
  private router = inject(Router);

  seances: SeanceDuJour[] = [];
  isFailed = false;
  errorMessage = '';
  today = new Date();
  seanceSelectionnee?: SeanceDuJour;
  presences: PresenceSeance[] = [];
  chargementPresences = false;
  erreurPresences = '';
  miseAJourPresences = new Set<number>();
  enregistrementCommentaire = false;
  faCheck = faCheck;
  faXmark = faXmark;
  faTriangleExclamation = faTriangleExclamation;

  ngOnInit(): void {
    const roles = this.tokenStorageService.getUser().roles as string[] | undefined;
    if (!roles) {
      this.router.navigate(['login']);
      return;
    }
    if (!roles.includes('ROLE_PROF') && !roles.includes('ROLE_REFERENT')) {
      this.router.navigate(['inscription']);
      return;
    }

    this.userService.getSeancesDuJour().subscribe({
      next: data => this.seances = data,
      error: (error: HttpErrorResponse) => {
        this.isFailed = true;
        this.errorMessage = error.error?.message || 'Impossible de charger les séances du jour.';
      }
    });
  }

  ouvrirPresences(seance: SeanceDuJour, contenu: TemplateRef<unknown>): void {
    this.seanceSelectionnee = seance;
    this.presences = [];
    this.erreurPresences = '';
    this.chargementPresences = true;
    this.modalService.open(contenu, { centered: true, size: 'lg' });
    this.userService.getPresences(seance.id).subscribe({
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
    const valeurPrecedente = presence.presence;
    presence.presence = !valeurPrecedente;
    this.miseAJourPresences.add(presence.id);
    this.userService.updatePresence(this.seanceSelectionnee!.id, presence.id, presence.presence).subscribe({
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
    this.userService.updateCommentaireSeance(this.seanceSelectionnee.id, this.seanceSelectionnee.commentaire).subscribe({
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
}
