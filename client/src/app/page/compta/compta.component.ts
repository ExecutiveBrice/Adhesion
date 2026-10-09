import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { ComptaService } from 'src/app/_services/compta.service';
import { ParamService } from 'src/app/_services/param.service';
import { ComptaActivite } from 'src/app/models/comptaActivite';
import { OrderByPipe } from '../../_helpers/sort.pipe';
import { NgbCalendar, NgbDate, NgbDatepickerModule, NgbDateStruct, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AdhesionCompta } from 'src/app/models/comptaPeriode';
import { Paiement } from 'src/app/models/paiement';
import { PaiementEditorComponent } from '../../template/paiement-editor/paiement-editor.component';

@Component({
  selector: 'app-compta',
  templateUrl: './compta.component.html',
  styleUrls: ['./compta.component.css'],
  imports: [FormsModule, OrderByPipe, DatePipe, NgbNavModule, NgbDatepickerModule, PaiementEditorComponent]
})
export class ComptaComponent implements OnInit, OnDestroy {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly comptaService = inject(ComptaService);
  private readonly paramService = inject(ParamService);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  private parametersRequest?: Subscription;

  dataCompta: ComptaActivite[] = [];
  adhesionsCompta: AdhesionCompta[] = [];
  activeTab = 'recap';
  typePaiementFiltre = '';
  activiteFiltre = '';
  rechercheAdherent = '';
  debutPlage: string | null = '';
  finPlage: string | null = '';
  calendarStart: NgbDateStruct = inject(NgbCalendar).getToday();
  hoveredDate: string | null = null;
  jourDebutPlage = 4;
  jourFinPlage = 3;
  loading = false;
  saving = false;
  erreurPaiement = '';
  erreurRapprochement = '';
  erreur = '';
  periodeCalculee: { debut: string; fin: string } | null = null;
  totalHA = 0;
  totalHA3X = 0;
  totalC = 0;
  totalC3X = 0;
  totalPS = 0;
  totalE = 0;
  totalA = 0;
  totalIntermarche = 0;
  totalCB = 0;

  ngOnInit(): void {
    this.parametersRequest = this.paramService.getAllNumber().subscribe({
      next: data => {
        this.jourDebutPlage = this.validDay(data.find(param => param.paramName === 'Jour_Debut_Plage_Compta')?.paramValue, 4);
        this.jourFinPlage = this.validDay(data.find(param => param.paramName === 'Jour_Fin_Plage_Compta')?.paramValue, 3);
        this.initializePeriod();
      },
      error: () => this.initializePeriod()
    });
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
    this.parametersRequest?.unsubscribe();
  }

  private validDay(value: number | null | undefined, fallback: number): number {
    return value != null && Number.isInteger(value) && value >= 1 && value <= 31 ? value : fallback;
  }

  private initializePeriod(): void {
    const today = new Date();
    const dayInMonth = (month: number, day: number): string => {
      const lastDay = new Date(today.getFullYear(), month + 1, 0).getDate();
      const date = new Date(today.getFullYear(), month, Math.min(day, lastDay));
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    };
    this.debutPlage = dayInMonth(today.getMonth() - 1, this.jourDebutPlage);
    this.finPlage = dayInMonth(today.getMonth(), this.jourFinPlage);
    const [year, month, day] = this.debutPlage.split('-').map(Number);
    this.calendarStart = { year, month, day };
    this.updateDate(this.debutPlage, this.finPlage);
  }

  onDateSelection(date: NgbDate): void {
    if (this.saving) return;
    this.onPeriodChange();
    this.hoveredDate = null;
    const selectedDate = this.calendarDate(date);
    if (!this.debutPlage || this.finPlage || selectedDate < this.debutPlage) {
      this.debutPlage = selectedDate;
      this.finPlage = null;
      return;
    }
    this.finPlage = selectedDate;
    this.updateDate(this.debutPlage, this.finPlage);
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
    return !!this.debutPlage && !!end && value >= this.debutPlage && value <= end;
  }

  onPeriodChange(): void {
    this.parametersRequest?.unsubscribe();
    this.clearResults();
  }

  private clearResults(): void {
    this.request?.unsubscribe();
    this.loading = false;
    this.erreur = '';
    this.erreurPaiement = '';
    this.periodeCalculee = null;
    this.erreurRapprochement = '';
    this.dataCompta = [];
    this.adhesionsCompta = [];
    this.totalHA = this.totalHA3X = this.totalC = this.totalC3X = 0;
    this.totalPS = this.totalE = this.totalA = this.totalIntermarche = 0;
    this.totalCB = 0;
  }

  private isDate(value: string | null): value is string {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return false;
    }
    const date = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
  }

  updateDate(dateDebut: string | null, dateFin: string | null): void {
    this.clearResults();
    if (!this.isDate(dateDebut) || !this.isDate(dateFin)) {
      this.erreur = 'Sélectionnez une date de début et une date de fin valides.';
      return;
    }
    if (dateDebut > dateFin) {
      this.erreur = 'La date de début doit être antérieure ou égale à la date de fin.';
      return;
    }
    this.loading = true;
    this.request = this.comptaService.getPeriode(dateDebut, dateFin).subscribe({
      next: periode => {
        const data = periode.recap;
        this.dataCompta = data;
        this.adhesionsCompta = periode.adhesions;
        const sum = (field: keyof Omit<ComptaActivite, 'nomActivite'>): number =>
          data.reduce((total, report) => total + (report[field] ?? 0), 0);
        this.totalHA = sum('helloAsso');
        this.totalHA3X = sum('helloAsso3x');
        this.totalC = sum('cheque');
        this.totalC3X = sum('cheque3x');
        this.totalPS = sum('passport');
        this.totalE = sum('espece');
        this.totalA = sum('autre');
        this.totalIntermarche = sum('intermarche');
        this.totalCB = sum('cb');
        this.periodeCalculee = { debut: dateDebut, fin: dateFin };
        this.loading = false;
      },
      error: () => {
        this.clearResults();
        this.erreur = 'Impossible de calculer la comptabilité pour cette période. Réessayez.';
      }
    });
  }

  totalActivite(compte: ComptaActivite): number {
    return (compte.helloAsso ?? 0) + (compte.helloAsso3x ?? 0) + (compte.cheque ?? 0)
      + (compte.cheque3x ?? 0) + (compte.espece ?? 0) + (compte.passport ?? 0)
      + (compte.intermarche ?? 0) + (compte.autre ?? 0) + (compte.cb ?? 0);
  }

  get totalGeneral(): number {
    return this.totalHA + this.totalHA3X + this.totalC + this.totalC3X
      + this.totalPS + this.totalE + this.totalA + this.totalIntermarche + this.totalCB;
  }

  get totalAdhesions(): number {
    return this.adhesionsCompta.reduce((total, adhesion) => total + adhesion.totalPeriode, 0);
  }

  get typesPaiementDisponibles(): string[] {
    const types = new Set(this.adhesionsCompta.flatMap(adhesion => adhesion.paiements)
      .map(paiement => paiement.typeReglement).filter((type): type is string => !!type));
    if (this.typePaiementFiltre) types.add(this.typePaiementFiltre);
    return [...types].sort((a, b) => a.localeCompare(b, 'fr'));
  }

  get activitesDisponibles(): string[] {
    const activites = new Set([
      ...this.dataCompta.map(compte => compte.nomActivite),
      ...this.adhesionsCompta.map(adhesion => adhesion.nomActivite)
    ]);
    if (this.activiteFiltre) activites.add(this.activiteFiltre);
    return [...activites].sort((a, b) => a.localeCompare(b, 'fr'));
  }

  get adhesionsFiltrees(): AdhesionCompta[] {
    const mots = this.normalizeSearch(this.rechercheAdherent).split(/\s+/).filter(Boolean);
    return this.adhesionsCompta.filter(adhesion => {
      if (this.activiteFiltre && adhesion.nomActivite !== this.activiteFiltre) return false;
      if (this.typePaiementFiltre && !adhesion.paiements.some(paiement => paiement.typeReglement === this.typePaiementFiltre)) return false;
      const adherent = this.normalizeSearch([
        adhesion.nomAdherent, adhesion.prenomAdherent, adhesion.emailAdherent
      ].filter(Boolean).join(' '));
      return mots.every(mot => adherent.includes(mot));
    });
  }

  get totalAdhesionsFiltrees(): number {
    return this.adhesionsFiltrees.reduce((total, adhesion) => total + adhesion.totalPeriode, 0);
  }

  get filtresAdhesionsActifs(): boolean {
    return !!(this.typePaiementFiltre || this.activiteFiltre || this.rechercheAdherent.trim());
  }

  resetAdhesionFilters(): void {
    this.typePaiementFiltre = '';
    this.activiteFiltre = '';
    this.rechercheAdherent = '';
  }

  private normalizeSearch(value: string): string {
    return value.normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('fr').trim();
  }

  updateRapprochement(adhesion: AdhesionCompta, rapprochement: boolean): void {
    if (this.saving || this.loading || !this.periodeCalculee || adhesion.rapprochement === rapprochement) return;
    this.saving = true;
    this.erreurRapprochement = '';
    this.comptaService.updateRapprochement(adhesion.id, rapprochement)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          adhesion.rapprochement = rapprochement;
          this.saving = false;
        },
        error: () => {
          this.saving = false;
          this.erreurRapprochement = 'Le rapprochement n’a pas été enregistré. Réessayez.';
        }
      });
  }

  updatePaiement(adhesion: AdhesionCompta, paiement: Paiement): void {
    if (this.saving || !this.periodeCalculee) return;
    this.modifyPaiement(this.comptaService.savePaiement(adhesion.id, paiement));
  }

  retraitPaiement(adhesion: AdhesionCompta, paiementId: number): void {
    if (this.saving || !this.periodeCalculee) return;
    this.modifyPaiement(this.comptaService.deletePaiement(adhesion.id, paiementId));
  }

  private modifyPaiement(operation: Observable<void>): void {
    const periode = this.periodeCalculee!;
    this.saving = true;
    this.erreurPaiement = '';
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving = false;
        if (this.debutPlage === periode.debut && this.finPlage === periode.fin) {
          this.updateDate(periode.debut, periode.fin);
        }
      },
      error: () => {
        this.saving = false;
        this.erreurPaiement = 'Le règlement n’a pas été enregistré. Réessayez : les totaux restent ceux du dernier calcul.';
      }
    });
  }
}
