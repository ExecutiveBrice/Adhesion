import { Component, inject, Input, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal, NgbCalendar, NgbDate, NgbDatepickerModule, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { MembreCAMateriel, Materiel } from '../../models/materiel';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-emprunt-materiel-modal',
  imports: [FormsModule, DatePipe, NgbDatepickerModule],
  templateUrl: './emprunt-materiel-modal.component.html',
  styleUrls: ['./emprunt-materiel-modal.component.css']
})
export class EmpruntMaterielModalComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  private readonly service = inject(MaterielService);
  private readonly storage = inject(TokenStorageService);
  private readonly calendar = inject(NgbCalendar);
  @Input() materiel!: Materiel;
  readonly administrateur = this.storage.getUser().roles?.includes('ROLE_ADMIN') ?? false;
  quantite = 1;
  calendarStart = this.calendar.getToday();
  debutPlage = this.calendarDate(this.calendarStart);
  finPlage: string | null = this.calendarDate(this.calendar.getNext(this.calendarStart, 'd', 1));
  hoveredDate: string | null = null;
  membresCA: MembreCAMateriel[] = [];
  emprunteurId: number | null = this.storage.getUser().id ?? null;
  chargement = true;
  enregistrement = false;
  erreur = '';

  ngOnInit(): void { if (this.administrateur) this.charger(); else this.chargement = false; }

  onDateSelection(date: NgbDate): void {
    if (this.enregistrement) return;
    this.hoveredDate = null;
    const selectedDate = this.calendarDate(date);
    if (this.finPlage || selectedDate < this.debutPlage) {
      this.debutPlage = selectedDate;
      this.finPlage = null;
    } else {
      this.finPlage = selectedDate;
    }
  }

  calendarDate(date: NgbDateStruct): string {
    return `${String(date.year).padStart(4, '0')}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`;
  }

  isRangeBoundary(date: NgbDateStruct): boolean {
    const value = this.calendarDate(date);
    return value === this.debutPlage || value === this.finPlage;
  }

  isInRange(date: NgbDateStruct): boolean {
    const value = this.calendarDate(date);
    const end = this.finPlage || this.hoveredDate;
    return !!end && value >= this.debutPlage && value <= end;
  }

  get periodeValide(): boolean {
    return !!this.debutPlage && !!this.finPlage && this.debutPlage <= this.finPlage;
  }

  charger(): void {
    this.chargement = true;
    this.membresCA = [];
    this.erreur = '';
    this.service.getMembresCA().subscribe({
      next: membresCA => {
        this.membresCA = membresCA;
        if (!membresCA.some(membre => membre.id === this.emprunteurId)) this.emprunteurId = null;
        this.chargement = false;
      },
      error: () => { this.chargement = false; this.erreur = 'La liste des membres du conseil d’administration n’a pas pu être chargée.'; }
    });
  }

  emprunter(): void {
    if (this.enregistrement || this.chargement || this.emprunteurId === null || !this.periodeValide
        || (this.administrateur && !this.membresCA.some(membre => membre.id === this.emprunteurId))
        || !Number.isInteger(this.quantite) || this.quantite < 1 || this.quantite > (this.materiel.quantiteDisponible ?? 1)) return;
    this.enregistrement = true;
    this.erreur = '';
    this.service.emprunter(this.materiel.id, this.administrateur ? this.emprunteurId! : undefined,
      this.quantite, this.debutPlage, this.finPlage!).subscribe({
      next: materiel => this.activeModal.close(materiel),
      error: response => {
        this.enregistrement = false;
        this.erreur = response?.error?.detail || response?.error?.message || 'L’emprunt n’a pas pu être enregistré.';
      }
    });
  }
}
