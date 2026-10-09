import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ComptaComponent } from './compta.component';
import { ComptaService } from '../../_services/compta.service';
import { ParamService } from '../../_services/param.service';
import { ComptaActivite } from '../../models/comptaActivite';
import { ComptaPeriode } from '../../models/comptaPeriode';
import { NgbDate, NgbNavConfig } from '@ng-bootstrap/ng-bootstrap';
import { Paiement } from '../../models/paiement';
import { ApiRenderService } from '../../_services/api-render.service';

describe('ComptaComponent', () => {
  let component: ComptaComponent;
  let fixture: ComponentFixture<ComptaComponent>;
  let service: jasmine.SpyObj<ComptaService>;
  let parameters: jasmine.SpyObj<ParamService>;
  const report: ComptaActivite = {
    nomActivite: 'Basket', helloAsso: 10, helloAsso3x: 20, cheque: 30, cheque3x: 40,
    espece: 50, passport: 60, intermarche: 70, autre: 80, cb: 0
  };

  const periode: ComptaPeriode = {
    recap: [report],
    adhesions: [{
      id: 7, nomActivite: 'Basket', nomAdherent: 'Martin', prenomAdherent: 'Camille',
      statutActuel: 'Validée', emailAdherent: 'camille@example.org', totalPeriode: 360, rapprochement: null,
      paiements: [
        { id: 1, dateReglement: '2026-09-01', typeReglement: 'HelloAsso', montant: 10, inclusDansPeriode: true },
        { id: 2, dateReglement: '2026-09-30', typeReglement: 'HelloAsso 3X', montant: 20, inclusDansPeriode: true },
        { id: 3, dateReglement: '2026-09-01', typeReglement: 'Chèque', montant: 30, inclusDansPeriode: true },
        { id: 4, dateReglement: '2026-09-30', typeReglement: 'Chèque 3X', montant: 40, inclusDansPeriode: true },
        { id: 5, dateReglement: '2026-09-01', typeReglement: 'Espèces', montant: 50, inclusDansPeriode: true },
        { id: 6, dateReglement: '2026-09-30', typeReglement: 'Pass sport', montant: 60, inclusDansPeriode: true },
        { id: 7, dateReglement: '2026-09-01', typeReglement: 'Intermarché', montant: 70, inclusDansPeriode: true },
        { id: 8, dateReglement: '2026-09-30', typeReglement: 'Autre', montant: 80, inclusDansPeriode: true }
      ]
    }]
  };
  const filterPeriod: ComptaPeriode = {
    recap: [{ ...report, espece: 150 }, {
      nomActivite: 'Danse', helloAsso: 0, helloAsso3x: 0, cheque: 0, cheque3x: 0,
      espece: 10, passport: 0, intermarche: 0, autre: 0, cb: 50
    }],
    adhesions: [periode.adhesions[0], {
      ...periode.adhesions[0], id: 8, nomAdherent: 'Durand', prenomAdherent: 'Élodie',
      emailAdherent: 'elodie.durand@example.org', totalPeriode: 100,
      paiements: [
        { id: 81, dateReglement: '2026-09-15', typeReglement: 'Espèces', montant: 100, inclusDansPeriode: true },
        { id: 82, dateReglement: '2026-08-31', typeReglement: 'CB', montant: 200, inclusDansPeriode: false }
      ]
    }, {
      ...periode.adhesions[0], id: 9, nomActivite: 'Danse', nomAdherent: 'Durand',
      prenomAdherent: 'Élodie', emailAdherent: 'danse@example.org', totalPeriode: 50,
      paiements: [{ id: 91, dateReglement: '2026-09-15', typeReglement: 'CB', montant: 50, inclusDansPeriode: true }]
    }, {
      ...periode.adhesions[0], id: 10, nomActivite: 'Danse', nomAdherent: null,
      prenomAdherent: null, emailAdherent: null, totalPeriode: 10,
      paiements: [{ id: 101, dateReglement: '2026-09-15', typeReglement: 'Espèces', montant: 10, inclusDansPeriode: true }]
    }]
  };
  beforeEach(() => {
    service = jasmine.createSpyObj('ComptaService', ['getPeriode', 'savePaiement', 'deletePaiement', 'updateRapprochement']);
    service.updateRapprochement.and.returnValue(of(undefined));
    service.savePaiement.and.returnValue(of(undefined));
    service.deletePaiement.and.returnValue(of(undefined));
    parameters = jasmine.createSpyObj('ParamService', ['getAllNumber']);
    service.getPeriode.and.returnValue(of(periode));
    parameters.getAllNumber.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ComptaComponent],
      providers: [
        { provide: ComptaService, useValue: service },
        { provide: ParamService, useValue: parameters }
      ]
    });
    TestBed.inject(NgbNavConfig).animation = false;
    fixture = TestBed.createComponent(ComptaComponent);
    component = fixture.componentInstance;
  });

  it('sends the selected calendar dates unchanged, including a one-day period', () => {
    component.updateDate('2024-02-29', '2024-02-29');
    expect(service.getPeriode).toHaveBeenCalledWith('2024-02-29', '2024-02-29');
    expect(component.periodeCalculee).toEqual({ debut: '2024-02-29', fin: '2024-02-29' });
  });

  it('shows two months and automatically calculates only after the second calendar click', () => {
    fixture.detectChanges();
    component.calendarStart = { year: 2026, month: 9, day: 1 };
    fixture.detectChanges();
    service.getPeriode.calls.reset();
    const months: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.ngb-dp-month'));
    expect(months.length).toBe(2);
    const clickDay = (month: HTMLElement, day: number) => {
      const days: HTMLElement[] = Array.from(month.querySelectorAll('.calendar-day'));
      days.find(element => element.textContent?.trim() === String(day))!.click();
      fixture.detectChanges();
    };
    clickDay(months[0], 4);
    expect(service.getPeriode).not.toHaveBeenCalled();
    expect(component.finPlage).toBeNull();
    expect(component.periodeCalculee).toBeNull();
    expect(fixture.nativeElement.querySelector('.period-dates').textContent).toContain('04/09/2026 à 00h00');
    clickDay(months[1], 3);
    expect(service.getPeriode).toHaveBeenCalledOnceWith('2026-09-04', '2026-10-03');
    expect(fixture.nativeElement.querySelector('.period-dates').textContent).toContain('03/10/2026 à 23h59');
    expect(months[0].querySelectorAll('.in-range').length).toBe(27);
    expect(months[1].querySelectorAll('.in-range').length).toBe(3);
    expect(fixture.nativeElement.querySelector('.period-selector').textContent).not.toContain('Calculer');
  });

  it('allows a one-day selection and starts a fresh range after a completed selection', () => {
    component.onDateSelection(new NgbDate(2024, 2, 29));
    expect(service.getPeriode).not.toHaveBeenCalled();
    component.onDateSelection(new NgbDate(2024, 2, 29));
    expect(service.getPeriode).toHaveBeenCalledOnceWith('2024-02-29', '2024-02-29');
    component.onDateSelection(new NgbDate(2024, 3, 1));
    expect(component.debutPlage).toBe('2024-03-01');
    expect(component.finPlage).toBeNull();
    expect(component.totalGeneral).toBe(0);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('moves the start to an earlier click and calculates a range crossing the year boundary', () => {
    component.onDateSelection(new NgbDate(2026, 12, 15));
    component.onDateSelection(new NgbDate(2026, 12, 4));
    expect(component.debutPlage).toBe('2026-12-04');
    expect(component.finPlage).toBeNull();
    expect(service.getPeriode).not.toHaveBeenCalled();
    component.onDateSelection(new NgbDate(2027, 1, 3));
    expect(service.getPeriode).toHaveBeenCalledOnceWith('2026-12-04', '2027-01-03');
  });

  it('cancels a pending calculation and parameter initialization when selecting a new start', () => {
    const pendingParameters = new Subject<[]>();
    parameters.getAllNumber.and.returnValue(pendingParameters);
    component.ngOnInit();
    const pending = new Subject<ComptaPeriode>();
    service.getPeriode.and.returnValue(pending);
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    component.onDateSelection(new NgbDate(2026, 10, 1));
    pendingParameters.next([]);
    pending.next(periode);
    expect(component.debutPlage).toBe('2026-10-01');
    expect(component.finPlage).toBeNull();
    expect(component.periodeCalculee).toBeNull();
    expect(component.totalGeneral).toBe(0);
    expect(component.loading).toBeFalse();
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('prevents period selection while a payment is being saved', () => {
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    const pending = new Subject<void>();
    service.savePaiement.and.returnValue(pending);
    component.updatePaiement(periode.adhesions[0], Object.assign(new Paiement(), periode.adhesions[0].paiements[0]));
    component.onDateSelection(new NgbDate(2026, 10, 1));
    expect(component.debutPlage).toBe('2026-09-01');
    expect(component.finPlage).toBe('2026-09-30');
    expect(component.periodeCalculee).toEqual({ debut: '2026-09-01', fin: '2026-09-30' });
    pending.next();
    expect(component.saving).toBeFalse();
    expect(service.getPeriode).toHaveBeenCalledTimes(2);
  });

  it('includes every payment type in row and overall totals without accumulation', () => {
    component.updateDate('2026-09-01', '2026-09-30');
    component.updateDate('2026-09-01', '2026-09-30');
    expect(component.totalActivite(report)).toBe(360);
    expect(component.totalGeneral).toBe(360);
    expect(component.totalE).toBe(50);
    expect(component.totalPS).toBe(60);
    expect(component.totalIntermarche).toBe(70);
  });

  it('renders cash, PassSport and Intermarché under their corresponding headings', () => {
    fixture.detectChanges();
    const table: HTMLTableElement = fixture.nativeElement.querySelector('table');
    expect(table.rows[0].cells[5].textContent).toBe('Espèces');
    expect(table.rows[1].cells[5].textContent).toBe('50');
    expect(table.rows[0].cells[6].textContent).toBe('PassSport');
    expect(table.rows[1].cells[6].textContent).toBe('60');
    expect(table.rows[0].cells[7].textContent).toBe('Intermarché');
    expect(table.rows[1].cells[7].textContent).toBe('70');
    expect(table.rows[1].cells[10].textContent).toBe('360');
    expect(table.rows[2].cells[10].textContent).toBe('360');
  });

  it('rejects missing, impossible and reversed dates without requesting data', () => {
    for (const [start, end] of [[null, '2026-09-30'], ['2026-02-30', '2026-03-01'],
      ['2026-10-01', '2026-09-30'], ['invalid', '2026-09-30']]) {
      component.updateDate(start, end);
      expect(component.erreur).toBeTruthy();
      expect(component.periodeCalculee).toBeNull();
    }
    expect(service.getPeriode).not.toHaveBeenCalled();
  });

  it('opens the adhesion tab and shows the payments used for the summary', async () => {
    fixture.detectChanges();
    const tabs: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('[ngbNavLink]'));
    expect(tabs.length).toBe(2);
    expect(tabs[0].textContent).toContain('Récapitulatif');
    tabs[1].click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.activeTab).toBe('adhesions');
    const panel: HTMLElement = fixture.nativeElement.querySelector('[role="tabpanel"]');
    expect(panel.textContent).toContain('Martin Camille');
    const dates: HTMLInputElement[] = Array.from(panel.querySelectorAll('input[type="date"]'));
    expect(dates.map(input => input.value)).toContain('2026-09-01');
    expect(dates.map(input => input.value)).toContain('2026-09-30');
    expect(panel.querySelector('a')?.getAttribute('href')).toBe('mailto:camille@example.org');
    const detailsTable: HTMLTableElement = panel.querySelector('table')!;
    expect(detailsTable.rows[0].cells.length).toBe(6);
    expect(detailsTable.rows[0].cells[0].textContent).toBe('Adhérent');
    expect(panel.textContent).not.toContain('#7');
    expect(panel.textContent).toContain('Intermarché');
    const footer: HTMLElement = panel.querySelector('tfoot')!;
    expect(footer.textContent).toContain('360');
    expect(component.totalAdhesions).toBe(component.totalGeneral);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('shows gray choices by default and saves exclusive green and red selections', async () => {
    const adhesion = { ...periode.adhesions[0] };
    service.getPeriode.and.returnValue(of({ ...periode, adhesions: [adhesion] }));
    fixture.detectChanges();
    component.activeTab = 'adhesions';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const table: HTMLTableElement = fixture.nativeElement.querySelector('[role="tabpanel"] table');
    expect(table.rows[0].cells[3].textContent).toBe('Rapprochement');
    const buttons: HTMLButtonElement[] = Array.from(table.tBodies[0].rows[0].cells[3].querySelectorAll('button'));
    expect(buttons.map(button => button.textContent)).toEqual(['Oui', 'Non']);
    expect(buttons.every(button => button.classList.contains('btn-secondary'))).toBeTrue();
    expect(buttons.every(button => button.getAttribute('aria-pressed') === 'false')).toBeTrue();
    const pending = new Subject<void>();
    service.updateRapprochement.and.returnValue(pending);
    buttons[0].click();
    fixture.detectChanges();
    expect(service.updateRapprochement).toHaveBeenCalledOnceWith(7, true);
    expect(buttons.every(button => button.disabled)).toBeTrue();
    expect(adhesion.rapprochement).toBeNull();
    pending.next();
    pending.complete();
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(buttons[0].classList.contains('btn-success')).toBeTrue();
    expect(buttons[0].getAttribute('aria-pressed')).toBe('true');
    expect(buttons[1].classList.contains('btn-secondary')).toBeTrue();
    service.updateRapprochement.and.returnValue(of(undefined));
    buttons[1].click();
    fixture.detectChanges();
    expect(service.updateRapprochement).toHaveBeenCalledWith(7, false);
    expect(buttons[1].classList.contains('btn-danger')).toBeTrue();
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');
    expect(buttons[0].classList.contains('btn-secondary')).toBeTrue();
    expect(component.totalGeneral).toBe(360);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('restores a saved choice on reload and preserves it when a write fails', async () => {
    const adhesion = { ...periode.adhesions[0], rapprochement: false };
    service.getPeriode.and.returnValue(of({ ...periode, adhesions: [adhesion] }));
    fixture.detectChanges();
    component.activeTab = 'adhesions';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('tbody .btn-group button'));
    expect(buttons[1].classList.contains('btn-danger')).toBeTrue();
    service.updateRapprochement.and.returnValue(throwError(() => new Error('failure')));
    buttons[0].click();
    fixture.detectChanges();
    expect(adhesion.rapprochement).toBeFalse();
    expect(buttons[1].classList.contains('btn-danger')).toBeTrue();
    expect(component.saving).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('rapprochement n’a pas été enregistré');
  });

  it('shows an empty state when no adhesion contributes to the period', async () => {
    service.getPeriode.and.returnValue(of({ recap: [], adhesions: [] }));
    fixture.detectChanges();
    component.activeTab = 'adhesions';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune adhésion');
    expect(component.totalAdhesions).toBe(0);
  });

  it('displays all payments while counting only the payment retained for the period', async () => {
    service.getPeriode.and.returnValue(of({
      recap: [{ ...report, helloAsso: 10, helloAsso3x: 0, cheque: 0, cheque3x: 0,
        espece: 0, passport: 0, intermarche: 0, autre: 0 }],
      adhesions: [{ ...periode.adhesions[0], totalPeriode: 10, paiements: [
        { id: 1, dateReglement: '2026-09-01', typeReglement: 'HelloAsso', montant: 10, inclusDansPeriode: true },
        { id: 2, dateReglement: '2026-08-31', typeReglement: 'HelloAsso', montant: 100, inclusDansPeriode: false },
        { id: 3, dateReglement: '2026-10-01', typeReglement: 'HelloAsso', montant: 200, inclusDansPeriode: false },
        { id: 4, dateReglement: null, typeReglement: null, montant: null, inclusDansPeriode: false }
      ] }]
    }));
    fixture.detectChanges();
    component.activeTab = 'adhesions';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const panel: HTMLElement = fixture.nativeElement.querySelector('[role="tabpanel"]');
    await fixture.whenStable();
    fixture.detectChanges();
    const amounts: HTMLInputElement[] = Array.from(panel.querySelectorAll('input[type="number"]'));
    expect(amounts.map(input => input.value)).toEqual(['10', '100', '200', '']);
    expect(amounts.every(input => !input.disabled)).toBeTrue();
    expect(Array.from(panel.querySelectorAll('.badge')).map(badge => badge.textContent))
      .toEqual(['Dans le total', 'Non comptabilisé', 'Non comptabilisé', 'Non comptabilisé']);
    expect(component.totalGeneral).toBe(10);
    expect(component.totalAdhesions).toBe(10);
    const footer: HTMLElement = panel.querySelector('tfoot')!;
    expect(footer.textContent).toContain('10');
  });

  it('saves edits and recalculates both tabs for the selected period', () => {
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    const modified = Object.assign(new Paiement(), periode.adhesions[0].paiements[0], { montant: 15 });
    service.getPeriode.and.returnValue(of({
      recap: [{ ...report, helloAsso: 15 }],
      adhesions: [{ ...periode.adhesions[0], totalPeriode: 365 }]
    }));
    component.updatePaiement(periode.adhesions[0], modified);
    expect(service.savePaiement).toHaveBeenCalledWith(7, modified);
    expect(service.getPeriode).toHaveBeenCalledTimes(2);
    expect(component.totalGeneral).toBe(365);
    expect(component.totalAdhesions).toBe(365);
    expect(component.saving).toBeFalse();
  });

  it('combines filters across member fields and all linked payments without changing accounting data', () => {
    service.getPeriode.and.returnValue(of(filterPeriod));
    component.updateDate('2026-09-01', '2026-09-30');
    component.typePaiementFiltre = 'CB';
    component.activiteFiltre = 'Basket';
    component.rechercheAdherent = '  EXAMPLE.ORG   ELODIE durand  ';
    expect(component.adhesionsFiltrees.map(adhesion => adhesion.id)).toEqual([8]);
    expect(component.adhesionsFiltrees[0].paiements).toBe(filterPeriod.adhesions[1].paiements);
    expect(component.adhesionsFiltrees[0].paiements.length).toBe(2);
    expect(component.totalAdhesionsFiltrees).toBe(100);
    expect(component.totalGeneral).toBe(520);
    expect(component.totalAdhesions).toBe(520);
    component.activiteFiltre = '';
    component.rechercheAdherent = 'durand ÉLODIE';
    expect(component.adhesionsFiltrees.map(adhesion => adhesion.id)).toEqual([8, 9]);
    component.rechercheAdherent = 'danse@';
    expect(component.adhesionsFiltrees.map(adhesion => adhesion.id)).toEqual([9]);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('filters through the table controls, explains empty results and resets without reloading', async () => {
    service.getPeriode.and.returnValue(of(filterPeriod));
    fixture.detectChanges();
    component.activeTab = 'adhesions';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const panel: HTMLElement = fixture.nativeElement.querySelector('[role="tabpanel"]');
    const search: HTMLInputElement = panel.querySelector('#compta-adherent-search')!;
    const payment: HTMLSelectElement = panel.querySelector('#compta-payment-filter')!;
    const activity: HTMLSelectElement = panel.querySelector('#compta-activity-filter')!;
    search.value = 'elodie';
    search.dispatchEvent(new Event('input'));
    payment.value = 'CB';
    payment.dispatchEvent(new Event('change'));
    activity.value = 'Basket';
    activity.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const table: HTMLTableElement = panel.querySelector('table')!;
    expect(table.tBodies[0].rows.length).toBe(1);
    expect(table.tBodies[0].rows[0].cells[0].textContent).toContain('Durand Élodie');
    expect(table.tBodies[0].rows[0].querySelectorAll('input[type="number"]').length).toBe(2);
    expect(table.tFoot!.rows[0].cells[1].textContent).toBe('100');
    expect(panel.querySelector('[role="status"]')!.textContent).toContain('1 adhésion(s) affichée(s) sur 4');
    search.value = 'inexistant';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(table.tBodies[0].textContent).toContain('Aucune adhésion ne correspond aux filtres');
    expect(table.tFoot!.rows[0].cells[1].textContent).toBe('0');
    const reset: HTMLButtonElement = panel.querySelector('.btn-outline-secondary')!;
    reset.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(table.tBodies[0].rows.length).toBe(4);
    expect(table.tFoot!.rows[0].cells[1].textContent).toBe('520');
    expect(search.value).toBe('');
    expect(payment.value).toBe('');
    expect(activity.value).toBe('');
    expect(reset.disabled).toBeTrue();
    expect(component.totalGeneral).toBe(520);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
  });

  it('keeps filter selections and updates matching rows after editing the last payment of that type', () => {
    service.getPeriode.and.returnValue(of(filterPeriod));
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    component.typePaiementFiltre = 'CB';
    component.activiteFiltre = 'Basket';
    component.rechercheAdherent = 'elodie';
    expect(component.adhesionsFiltrees.length).toBe(1);
    const changedPeriod: ComptaPeriode = {
      ...filterPeriod,
      recap: filterPeriod.recap.map(compte => ({ ...compte, espece: compte.espece + compte.cb, cb: 0 })),
      adhesions: filterPeriod.adhesions.map(adhesion => ({ ...adhesion,
        paiements: adhesion.paiements.map(payment => payment.typeReglement === 'CB'
          ? { ...payment, typeReglement: 'Espèces' } : payment) }))
    };
    service.getPeriode.and.returnValue(of(changedPeriod));
    component.updatePaiement(filterPeriod.adhesions[1], Object.assign(new Paiement(),
      filterPeriod.adhesions[1].paiements[1], { typeReglement: 'Espèces' }));
    expect(component.typePaiementFiltre).toBe('CB');
    expect(component.activiteFiltre).toBe('Basket');
    expect(component.rechercheAdherent).toBe('elodie');
    expect(component.typesPaiementDisponibles).toContain('CB');
    expect(component.adhesionsFiltrees).toEqual([]);
    expect(component.totalAdhesionsFiltrees).toBe(0);
    expect(component.totalGeneral).toBe(520);
    expect(component.adhesionsCompta[1].paiements.length).toBe(2);
    expect(service.getPeriode).toHaveBeenCalledTimes(2);
  });

  it('keeps all payments visible after one moves outside the period and updates the period total', () => {
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    service.getPeriode.and.returnValue(of({
      recap: [{ ...report, helloAsso: 0 }],
      adhesions: [{ ...periode.adhesions[0], totalPeriode: 350, paiements: periode.adhesions[0].paiements.map(payment =>
        payment.id === 1 ? { ...payment, dateReglement: '2026-10-01', inclusDansPeriode: false } : payment) }]
    }));
    component.updatePaiement(periode.adhesions[0], Object.assign(new Paiement(),
      periode.adhesions[0].paiements[0], { dateReglement: '2026-10-01' }));
    expect(component.adhesionsCompta[0].paiements.length).toBe(8);
    expect(component.adhesionsCompta[0].paiements[0].inclusDansPeriode).toBeFalse();
    expect(component.totalGeneral).toBe(350);
    expect(component.totalAdhesions).toBe(350);
  });

  it('recalculates after deleting a payment and preserves totals if deletion fails', () => {
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    service.deletePaiement.and.returnValue(throwError(() => new Error('failure')));
    component.retraitPaiement(periode.adhesions[0], 1);
    expect(component.erreurPaiement).toBeTruthy();
    expect(component.totalGeneral).toBe(360);
    expect(component.adhesionsCompta[0].paiements.length).toBe(8);
    expect(service.getPeriode).toHaveBeenCalledTimes(1);
    service.deletePaiement.and.returnValue(of(undefined));
    service.getPeriode.and.returnValue(of({ recap: [], adhesions: [] }));
    component.retraitPaiement(periode.adhesions[0], 1);
    expect(service.deletePaiement).toHaveBeenCalledWith(7, 1);
    expect(component.totalGeneral).toBe(0);
    expect(component.erreurPaiement).toBe('');
  });

  it('prevents concurrent payment writes and leaves a failed edit available for retry', () => {
    component.debutPlage = '2026-09-01';
    component.finPlage = '2026-09-30';
    component.updateDate(component.debutPlage, component.finPlage);
    const pending = new Subject<void>();
    service.savePaiement.and.returnValue(pending);
    const modified = Object.assign(new Paiement(), periode.adhesions[0].paiements[0], { montant: 15 });
    component.updatePaiement(periode.adhesions[0], modified);
    component.updatePaiement(periode.adhesions[0], modified);
    expect(service.savePaiement).toHaveBeenCalledTimes(1);
    expect(component.saving).toBeTrue();
    pending.error(new Error('failure'));
    expect(component.saving).toBeFalse();
    expect(component.totalGeneral).toBe(360);
    expect(component.erreurPaiement).toBeTruthy();
    expect(periode.adhesions[0].paiements[0].montant).toBe(10);
  });

  it('discards an earlier response when a new period is requested', () => {
    const previous = new Subject<ComptaPeriode>();
    const current = new Subject<ComptaPeriode>();
    service.getPeriode.and.returnValues(previous, current);
    component.updateDate('2026-09-01', '2026-09-30');
    component.updateDate('2026-10-01', '2026-10-31');
    current.next(periode);
    previous.next({ ...periode, recap: [{ ...report, helloAsso: 1000 }] });
    expect(component.totalHA).toBe(10);
    expect(component.periodeCalculee?.debut).toBe('2026-10-01');
  });

  it('clears results and cancels the pending calculation when a date changes', () => {
    component.updateDate('2026-09-01', '2026-09-30');
    const pending = new Subject<ComptaPeriode>();
    service.getPeriode.and.returnValue(pending);
    component.updateDate('2026-10-01', '2026-10-31');
    component.onPeriodChange();
    pending.next(periode);
    expect(component.dataCompta).toEqual([]);
    expect(component.adhesionsCompta).toEqual([]);
    expect(component.totalAdhesions).toBe(0);
    expect(component.totalGeneral).toBe(0);
    expect(component.periodeCalculee).toBeNull();
    expect(component.loading).toBeFalse();
  });

  it('clears old totals and displays an error when the request fails', () => {
    component.updateDate('2026-09-01', '2026-09-30');
    service.getPeriode.and.returnValue(throwError(() => new Error('failure')));
    component.updateDate('2026-10-01', '2026-10-31');
    expect(component.totalGeneral).toBe(0);
    expect(component.dataCompta).toEqual([]);
    expect(component.erreur).toContain('Impossible');
    expect(component.loading).toBeFalse();
  });

  it('initializes the default period even when parameters fail', () => {
    parameters.getAllNumber.and.returnValue(throwError(() => new Error('failure')));
    component.ngOnInit();
    expect(service.getPeriode).toHaveBeenCalled();
    expect(component.debutPlage?.slice(-2)).toBe('04');
    expect(component.finPlage?.slice(-2)).toBe('03');
  });

  it('clamps configured days to the last day of the selected month', () => {
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2024, 2, 15));
      parameters.getAllNumber.and.returnValue(of([
        { id: 1, paramName: 'Jour_Debut_Plage_Compta', paramValue: 31 },
        { id: 2, paramName: 'Jour_Fin_Plage_Compta', paramValue: 31 }
      ]));
      component.ngOnInit();
      expect(service.getPeriode).toHaveBeenCalledWith('2024-02-29', '2024-03-31');
    } finally {
      jasmine.clock().uninstall();
    }
  });
});
