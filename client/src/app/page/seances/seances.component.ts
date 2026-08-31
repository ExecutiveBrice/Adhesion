import { Component, OnInit, inject } from '@angular/core';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { SeanceDuJour } from 'src/app/models/seance';
import { TokenStorageService } from 'src/app/_services/token-storage.service';
import { UserService } from 'src/app/_services/user.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { faCheck, faTriangleExclamation, faXmark } from '@fortawesome/free-solid-svg-icons';
import { NgClass, DatePipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { PresencesSeanceModalComponent } from 'src/app/template/presences-seance-modal/presences-seance-modal.component';

@Component({
    selector: 'app-seances',
    templateUrl: './seances.component.html',
    styleUrls: ['./seances.component.css'],
    imports: [NgClass, FaIconComponent, DatePipe]
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

  ouvrirPresences(seance: SeanceDuJour): void {
    const modalRef = this.modalService.open(PresencesSeanceModalComponent, { centered: true, size: 'lg' });
    const modal = modalRef.componentInstance as PresencesSeanceModalComponent;
    modal.seance = seance;
    modal.chargerPresences = (seanceId: number) => this.userService.getPresences(seanceId);
    modal.mettreAJourPresence = (seanceId: number, presenceId: number, presence: boolean) =>
      this.userService.updatePresence(seanceId, presenceId, presence);
    modal.enregistrerCommentaire = (seanceId: number, commentaire: string | null) =>
      this.userService.updateCommentaireSeance(seanceId, commentaire);
    modal.ajouterAdherent = (seanceId: number, email: string) =>
      this.userService.ajouterNouvelAdherentSeance(seanceId, email);
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
}
