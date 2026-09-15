import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { PresenceSeance, SeanceDuJour } from 'src/app/models/seance';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faFileSignature, faPiggyBank } from '@fortawesome/free-solid-svg-icons';

type ChargerPresences = (seanceId: number) => Observable<PresenceSeance[]>;
type MettreAJourPresence = (seanceId: number, presenceId: number, presence: boolean) => Observable<PresenceSeance>;
type EnregistrerCommentaire = (seanceId: number, commentaire: string | null) => Observable<SeanceDuJour>;
type AjouterAdherent = (seanceId: number, email: string) => Observable<PresenceSeance>;

@Component({
  selector: 'app-presences-seance-modal',
  templateUrl: './presences-seance-modal.component.html',
  styleUrls: ['./presences-seance-modal.component.css'],
  imports: [FormsModule, FaIconComponent]
})
export class PresencesSeanceModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  @Input({ required: true }) seance!: SeanceDuJour;
  @Input({ required: true }) chargerPresences!: ChargerPresences;
  @Input({ required: true }) mettreAJourPresence!: MettreAJourPresence;
  @Input({ required: true }) enregistrerCommentaire!: EnregistrerCommentaire;
  @Input() ajouterAdherent?: AjouterAdherent;
  @Output() seanceMiseAJour = new EventEmitter<SeanceDuJour>();

  presences: PresenceSeance[] = [];
  chargementPresences = false;
  erreurPresences = '';
  miseAJourPresences = new Set<number>();
  enregistrementCommentaire = false;
  emailNouvelAdherent = '';
  ajoutNouvelAdherent = false;
  faPiggyBank = faPiggyBank;
  faFileSignature = faFileSignature;
  faCircleCheck = faCircleCheck;

  initialiser(): void {
    this.chargementPresences = true;
    this.chargerPresences(this.seance.id).subscribe({
      next: presences => {
        this.presences = presences;
        this.chargementPresences = false;
        this.rafraichirVue();
      },
      error: error => {
        this.erreurPresences = error.error?.message || 'Impossible de charger les présences.';
        this.chargementPresences = false;
        this.rafraichirVue();
      }
    });
  }

  definirPresence(presence: PresenceSeance, valeur: boolean): void {
    if (presence.presence === valeur) return;

    const valeurPrecedente = presence.presence;
    presence.presence = valeur;
    this.miseAJourPresences.add(presence.id);
    this.mettreAJourPresence(this.seance.id, presence.id, presence.presence).subscribe({
      next: miseAJour => {
        presence.presence = miseAJour.presence;
        this.miseAJourPresences.delete(presence.id);
        this.rafraichirVue();
      },
      error: () => {
        presence.presence = valeurPrecedente;
        this.miseAJourPresences.delete(presence.id);
        this.erreurPresences = 'Impossible d’enregistrer la présence.';
        this.rafraichirVue();
      }
    });
  }

  sauvegarderCommentaire(): void {
    this.enregistrementCommentaire = true;
    this.enregistrerCommentaire(this.seance.id, this.seance.commentaire).subscribe({
      next: seance => {
        this.seance = seance;
        this.seanceMiseAJour.emit(seance);
        this.enregistrementCommentaire = false;
        this.rafraichirVue();
      },
      error: () => {
        this.erreurPresences = 'Impossible d’enregistrer le commentaire.';
        this.enregistrementCommentaire = false;
        this.rafraichirVue();
      }
    });
  }

  ajouterNouvelAdherent(): void {
    const email = this.emailNouvelAdherent.trim();
    if (!this.ajouterAdherent || !email) return;

    this.erreurPresences = '';
    this.ajoutNouvelAdherent = true;
    this.ajouterAdherent(this.seance.id, email).subscribe({
      next: presence => {
        this.presences = [...this.presences, presence].sort((a, b) =>
          `${a.nom} ${a.prenom}`.localeCompare(`${b.nom} ${b.prenom}`, 'fr'));
        this.emailNouvelAdherent = '';
        this.ajoutNouvelAdherent = false;
        this.rafraichirVue();
      },
      error: error => {
        this.erreurPresences = error.error?.detail || error.error?.message || 'Impossible d’ajouter le nouvel adhérent.';
        this.ajoutNouvelAdherent = false;
        this.rafraichirVue();
      }
    });
  }

  adhesionValidee(presence: PresenceSeance): boolean {
    return presence.statutAdhesion === 'Validée' || presence.statutAdhesion === 'Licence générée';
  }

  private rafraichirVue(): void {
    this.changeDetectorRef.detectChanges();
  }
}
