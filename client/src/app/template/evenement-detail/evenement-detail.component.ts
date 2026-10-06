import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, Input, OnInit, inject, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCalendarPlus, faFileArrowDown } from '@fortawesome/free-solid-svg-icons';
import { finalize } from 'rxjs';
import { PresencePrevueService } from '../../_services/presence-prevue.service';
import { Evenement } from '../../models/evenement';
import { ResponsabilitesSeanceComponent } from '../responsabilites-seance/responsabilites-seance.component';

@Component({
  selector: 'app-evenement-detail',
  imports: [DatePipe, FaIconComponent, ResponsabilitesSeanceComponent],
  templateUrl: './evenement-detail.component.html',
  styleUrl: './evenement-detail.component.css',
  changeDetection: ChangeDetectionStrategy.Default
})
export class EvenementDetailComponent implements OnInit {
  readonly modal = inject(NgbActiveModal);
  private readonly presencesService = inject(PresencePrevueService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly changeDetector = inject(ChangeDetectorRef);
  @Input() evenement!: Evenement;
  @Input() adherentId?: number;
  readonly presenceChange = output<void>();
  readonly faCalendarPlus = faCalendarPlus;
  readonly faFileArrowDown = faFileArrowDown;

  ngOnInit(): void {
    if (this.peutVoirPresences && !this.evenement.presences && !this.evenement.erreurPresence) this.chargerPresences();
  }

  get dernierJour(): string {
    const e = this.evenement;
    if (!e.journeeEntiere) return e.fin;
    // Les agendas Google renvoient une date de fin exclusive pour les journées entières.
    const date = new Date(`${e.fin.substring(0, 10)}T12:00:00`);
    date.setDate(date.getDate() - 1);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  get etatLibelle(): string {
    const etat = this.evenement.etatSeance;
    return etat ? { PROGRAMMEE: 'Programmée', ANNULEE: 'Annulée', REALISEE: 'Réalisée', MODIFIEE: 'Modifiée' }[etat] : '';
  }

  get destination(): { adresse: string; googleMaps: string; waze: string } | null {
    const e = this.evenement;
    const adresse = e.adresseSalle?.trim() || (e.source === 'GOOGLE' ? e.lieu?.trim() : '');
    if (!adresse) return null;
    const query = encodeURIComponent(adresse);
    return { adresse, googleMaps: `https://www.google.com/maps/search/?api=1&query=${query}`, waze: `https://waze.com/ul?q=${query}` };
  }

  get lienActivite(): string | null {
    const lien = this.evenement.lien?.trim();
    if (!lien) return null;
    if (/^https?:\/\//i.test(lien)) return lien;
    return /^[\w.-]+\.[a-z]{2,}(?:[/:?#]|$)/i.test(lien) ? `https://${lien}` : null;
  }

  get peutVoirPresences(): boolean {
    return this.adherentId != null && this.evenement.personnelle === true
      && this.evenement.seanceId != null && this.evenement.sectionType === 'COMPETITION';
  }

  get peutRenseignerPresence(): boolean {
    return this.peutVoirPresences
      && (this.evenement.etatSeance === 'PROGRAMMEE' || this.evenement.etatSeance === 'MODIFIEE')
      && new Date(this.evenement.debut).getTime() > Date.now();
  }

  get maPresence() { return this.evenement.presences?.find(p => p.adherentId === this.adherentId); }
  nombrePresences(valeur: boolean): number { return this.evenement.presences?.filter(p => p.presencePrevue === valeur).length ?? 0; }

  chargerPresences(): void {
    const e = this.evenement;
    if (!this.peutVoirPresences || e.chargementPresences) return;
    e.chargementPresences = true;
    e.erreurPresence = '';
    this.actualiserPresence();
    this.presencesService.getPresences(e.seanceId!).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => { e.chargementPresences = false; })
    ).subscribe({
      next: presences => { e.presences = presences; e.chargementPresences = false; this.actualiserPresence(); },
      error: error => {
        e.erreurPresence = error.error?.detail || 'Impossible de charger les présences prévues.';
        e.chargementPresences = false;
        this.actualiserPresence();
      }
    });
  }

  definirPresence(valeur: boolean): void {
    const e = this.evenement;
    if (!this.peutRenseignerPresence || !this.maPresence || e.chargementPresences || e.enregistrementPresence
      || this.maPresence.presencePrevue === valeur) return;
    e.enregistrementPresence = true;
    e.erreurPresence = '';
    this.actualiserPresence();
    this.presencesService.updatePresence(e.seanceId!, valeur).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        // Une fermeture pendant l'enregistrement impose de relire la réponse du serveur.
        if (e.enregistrementPresence) { e.enregistrementPresence = false; e.presences = undefined; }
      })
    ).subscribe({
      next: presence => {
        e.presences = e.presences?.map(p => p.id === presence.id ? presence : p);
        e.enregistrementPresence = false;
        this.chargerPresences();
      },
      error: error => {
        e.erreurPresence = error.error?.detail || 'Impossible d’enregistrer votre présence prévue.';
        e.enregistrementPresence = false;
        this.actualiserPresence();
      }
    });
  }

  actualiser(): void { this.changeDetector.markForCheck(); }
  private actualiserPresence(): void { this.presenceChange.emit(); this.actualiser(); }

  get lienGoogleAgenda(): string | null {
    const e = this.evenement;
    if (e.etatSeance === 'ANNULEE') return null;
    const date = (valeur: string) => e.journeeEntiere ? valeur.substring(0, 10).replace(/-/g, '')
      : this.dateCalendrier(valeur);
    const params = new URLSearchParams({ action: 'TEMPLATE', text: e.titre, dates: `${date(e.debut)}/${date(e.fin)}`, ctz: 'Europe/Paris' });
    if (this.descriptionCalendrier) params.set('details', this.descriptionCalendrier);
    if (e.adresseSalle || e.lieu) params.set('location', e.adresseSalle || e.lieu!);
    return `https://calendar.google.com/calendar/render?${params.toString()}`;
  }

  telechargerEvenement(): void {
    const e = this.evenement;
    const echapper = (texte: string) => texte.replace(/\\/g, '\\\\').replace(/\r?\n/g, '\\n').replace(/;/g, '\\;').replace(/,/g, '\\,');
    const date = (valeur: string) => new Date(valeur).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
    const dateEvenement = (cle: string, valeur: string) => e.journeeEntiere
      ? `${cle};VALUE=DATE:${valeur.substring(0, 10).replace(/-/g, '')}`
      : `${cle}${/(?:Z|[+-]\d{2}:\d{2})$/.test(valeur) ? '' : ';TZID=Europe/Paris'}:${this.dateCalendrier(valeur)}`;
    const contenu = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Adhesion//Evenement//FR', 'BEGIN:VEVENT',
      `UID:${echapper(e.id)}@adhesion`, `DTSTAMP:${date(new Date().toISOString())}`,
      dateEvenement('DTSTART', e.debut), dateEvenement('DTEND', e.fin), `SUMMARY:${echapper(e.titre)}`,
      `DESCRIPTION:${echapper(this.descriptionCalendrier)}`, `LOCATION:${echapper(e.adresseSalle || e.lieu || '')}`,
      ...(e.etatSeance === 'ANNULEE' ? ['STATUS:CANCELLED'] : []), 'END:VEVENT', 'END:VCALENDAR', ''].join('\r\n');
    const url = URL.createObjectURL(new Blob([contenu], { type: 'text/calendar;charset=utf-8' }));
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = 'evenement.ics';
    lien.click();
    URL.revokeObjectURL(url);
  }

  private dateCalendrier(valeur: string): string {
    // Les heures sans décalage renvoyées par l'API sont celles de Paris, indépendamment du navigateur.
    return /(?:Z|[+-]\d{2}:\d{2})$/.test(valeur)
      ? new Date(valeur).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')
      : valeur.replace(/[-:]/g, '').replace(/\.\d+$/, '');
  }

  private get descriptionCalendrier(): string {
    const e = this.evenement;
    const texte = new DOMParser().parseFromString(e.commentaire || '', 'text/html');
    return [e.descriptif, e.sectionNom, e.causeAnnulation, texte.body.textContent?.trim(),
      ...(e.responsabilites ?? []).map(r => `${r.tache.nom} : ${r.adherent.prenom} ${r.adherent.nom}`), this.lienActivite]
      .filter(Boolean).join('\n\n');
  }
}
