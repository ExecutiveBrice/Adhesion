import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import Chart from 'chart.js/auto';
import { HttpErrorResponse } from '@angular/common/http';
import { registerApiViewRefresh } from 'src/app/_services/api-render.service';
import { ReportingService } from 'src/app/_services/reporting.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ExcelService } from 'src/app/_services/excel.service';
import { ToastService } from 'src/app/_services/toast.service';
import { ReportingActivite } from 'src/app/models';

interface ReportingGroup {
  nom: string;
  activites: ReportingActivite[];
  total: ReportingActivite;
}

@Component({
  selector: 'app-reporting',
  templateUrl: './reporting.component.html',
  styleUrls: ['./reporting.component.css'],
  imports: [DecimalPipe]
})
export class ReportingComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly toastr = inject(ToastService);
  private readonly excelService = inject(ExcelService);
  private readonly adherentService = inject(AdherentService);
  private readonly reportingService = inject(ReportingService);

  chart: Chart | undefined;
  dataBasket: ReportingActivite[] = [];
  dataGeneral: ReportingActivite[] = [];
  reportingGroups: ReportingGroup[] = [];
  totalGeneral = this.emptyTotal();
  label: string[] = [];
  initiee: number[] = [];
  payee: number[] = [];
  validee: number[] = [];
  loader = false;

  ngOnInit(): void {
    this.reportingService.getAllBasket().subscribe({
      next: data => { this.dataBasket = data; this.buildReportingGroups(); },
      error: () => undefined
    });
    this.reportingService.getAllGeneral().subscribe({
      next: data => { this.dataGeneral = data; this.buildReportingGroups(); },
      error: () => undefined
    });
    this.reportingService.getAllAdhesions().subscribe({
      next: data => {
        data.forEach(adh => {
          this.initiee.push(adh.nbInitiee);
          this.payee.push(adh.nbPayee);
          this.validee.push(adh.nbValidee);
          this.label.push(adh.x);
        });
        this.chart = new Chart('canvas', {
          type: 'line',
          data: { labels: this.label, datasets: [
            { label: 'Initiées', data: this.initiee },
            { label: 'Payées', data: this.payee },
            { label: 'Validées', data: this.validee }
          ] }
        });
      },
      error: () => undefined
    });
  }

  percentage(value: number, total: number): number {
    return total ? (value / total) * 100 : 0;
  }

  exportAsXLSX(): void {
    this.loader = true;
    this.adherentService.getAllExportLite().subscribe({
      next: data => {
        this.loader = false;
        this.excelService.exportAsExcelFile(data, 'adherents');
      },
      error: (error: HttpErrorResponse) => {
        this.loader = false;
        this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + error.message, 'Erreur');
      }
    });
  }

  private buildReportingGroups(): void {
    const groups = new Map<string, ReportingGroup>();
    [...this.dataGeneral, ...this.dataBasket]
      .sort((a, b) => (a.groupe || 'Sans groupe').localeCompare(b.groupe || 'Sans groupe') || a.nomActivite.localeCompare(b.nomActivite))
      .forEach(activite => {
        const nom = activite.groupe || 'Sans groupe';
        const group = groups.get(nom) ?? { nom, activites: [], total: this.emptyTotal() };
        group.activites.push(activite);
        this.addToTotal(group.total, activite);
        groups.set(nom, group);
      });
    this.reportingGroups = [...groups.values()];
    this.totalGeneral = this.emptyTotal();
    this.reportingGroups.forEach(group => this.addToTotal(this.totalGeneral, group.total));
  }

  private emptyTotal(): ReportingActivite {
    return { nomActivite: '', groupe: '', nbInitee: 0, nbPayee: 0, nbValidee: 0, nbF: 0, nbM: 0, cotisations: 0 };
  }

  private addToTotal(total: ReportingActivite, activite: ReportingActivite): void {
    total.nbInitee += activite.nbInitee;
    total.nbPayee += activite.nbPayee;
    total.nbValidee += activite.nbValidee;
    total.nbF += activite.nbF;
    total.nbM += activite.nbM;
    total.cotisations += activite.cotisations;
  }
}
