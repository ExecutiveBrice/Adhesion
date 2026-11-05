import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal, NgbCalendar, NgbDate } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { EmpruntMaterielModalComponent } from './emprunt-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Materiel } from '../../models/materiel';
import { ApiRenderService } from '../../_services/api-render.service';

describe('Choix du membre du CA emprunteur', () => {
  let fixture: ComponentFixture<EmpruntMaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  let user: { id: number; roles: string[] };
  const materiel: Materiel = { id: 2, nom: 'Objet', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
    emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  beforeEach(() => {
    user = { id: 3, roles: ['ROLE_ADMIN'] };
    service = jasmine.createSpyObj('MaterielService', ['getMembresCA', 'emprunter']);
    service.getMembresCA.and.returnValue(of([{ id: 3, nom: 'Camille Martin' }, { id: 4, nom: 'Alex Durand' }]));
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [EmpruntMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal },
      { provide: TokenStorageService, useValue: { getUser: () => user } }]
    });
    spyOn(TestBed.inject(NgbCalendar), 'getToday').and.returnValue(new NgbDate(2026, 12, 31));
    fixture = TestBed.createComponent(EmpruntMaterielModalComponent);
    fixture.componentInstance.materiel = materiel;
  });

  it('présélectionne le compte connecté et permet de choisir un autre membre du CA', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    expect(select.selectedOptions[0].textContent).toBe('Camille Martin');
    expect(service.emprunter).not.toHaveBeenCalled();
    const resultat = { ...materiel, emprunteurId: 4, emprunteurNom: 'Alex Durand', dateEmprunt: '2026-10-10T12:00:00Z' };
    service.emprunter.and.returnValue(of(resultat));
    fixture.componentInstance.emprunteurId = 4;
    fixture.componentInstance.emprunter();
    expect(service.emprunter).toHaveBeenCalledWith(2, 4, 1, '2026-12-31', '2027-01-01');
    expect(modal.close).toHaveBeenCalledWith(resultat);
  });

  it('garde la modale ouverte en cas de conflit et empêche les doubles validations', () => {
    fixture.detectChanges();
    const request = new Subject<Materiel>();
    service.emprunter.and.returnValue(request);
    const component = fixture.componentInstance;
    component.emprunter(); component.emprunter();
    expect(service.emprunter).toHaveBeenCalledTimes(1);
    request.error({ error: { detail: 'Ce matériel est hors service' } });
    TestBed.inject(ApiRenderService).notify();
    expect(component.enregistrement).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Ce matériel est hors service');
    expect(modal.close).not.toHaveBeenCalled();
  });

  it('bloque la validation si la liste ne se charge pas ou si le compte n’est pas membre du CA', () => {
    service.getMembresCA.and.returnValue(throwError(() => ({})));
    fixture.detectChanges();
    fixture.componentInstance.emprunter();
    expect(service.emprunter).not.toHaveBeenCalled();
    service.getMembresCA.and.returnValue(of([{ id: 4, nom: 'Alex' }]));
    fixture.componentInstance.charger();
    expect(fixture.componentInstance.emprunteurId).toBeNull();
  });

  it('affiche la période sous la quantité avec aujourd’hui et demain, même au changement d’année', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.debutPlage).toBe('2026-12-31');
    expect(component.finPlage).toBe('2027-01-01');
    expect(component.periodeValide).toBeTrue();
    const input = fixture.nativeElement.querySelector('#quantiteEmprunt');
    expect(input.nextElementSibling.querySelector('ngb-datepicker')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.period-dates').textContent).toContain('31/12/2026');
    expect(fixture.nativeElement.querySelector('.period-dates').textContent).toContain('01/01/2027');
    expect(fixture.nativeElement.querySelectorAll('.ngb-dp-month').length).toBe(2);
  });

  it('sélectionne une plage et interdit la confirmation tant que la fin n’est pas choisie', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const day = (value: string): HTMLElement => Array.from<HTMLElement>(
      fixture.nativeElement.querySelectorAll('.ngb-dp-month:first-child .ngb-dp-day'))
      .find(element => element.textContent?.trim() === value)!;
    day('10').click();
    fixture.detectChanges();
    expect(component.debutPlage).toBe('2026-12-10');
    expect(component.finPlage).toBeNull();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    component.emprunter();
    expect(service.emprunter).not.toHaveBeenCalled();
    component.hoveredDate = '2026-12-15';
    expect(component.isInRange(new NgbDate(2026, 12, 12))).toBeTrue();
    day('15').click();
    fixture.detectChanges();
    expect(component.finPlage).toBe('2026-12-15');
    expect(component.hoveredDate).toBeNull();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeFalse();
    service.emprunter.and.returnValue(of(materiel));
    component.emprunter();
    expect(service.emprunter).toHaveBeenCalledWith(2, 3, 1, '2026-12-10', '2026-12-15');
  });

  it('redéfinit le début pour une date antérieure et accepte une plage d’une journée', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.onDateSelection(new NgbDate(2026, 12, 20));
    component.onDateSelection(new NgbDate(2026, 12, 10));
    expect(component.debutPlage).toBe('2026-12-10');
    expect(component.finPlage).toBeNull();
    component.onDateSelection(new NgbDate(2026, 12, 10));
    expect(component.periodeValide).toBeTrue();
    component.enregistrement = true;
    component.onDateSelection(new NgbDate(2026, 12, 15));
    expect(component.finPlage).toBe('2026-12-10');
  });

  it('envoie aussi les dates pour un membre du CA connecté sans choix d’emprunteur', () => {
    fixture.destroy();
    user.roles = ['ROLE_MEMBRECA'];
    fixture = TestBed.createComponent(EmpruntMaterielModalComponent);
    fixture.componentInstance.materiel = materiel;
    fixture.detectChanges();
    service.emprunter.and.returnValue(of(materiel));
    fixture.componentInstance.emprunter();
    expect(service.getMembresCA).not.toHaveBeenCalled();
    expect(service.emprunter).toHaveBeenCalledWith(2, undefined, 1, '2026-12-31', '2027-01-01');
  });
});
