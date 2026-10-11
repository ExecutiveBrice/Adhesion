import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
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
  const materiel: Materiel = { id: 2, nom: 'Objet', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
    emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getMembresCA', 'emprunter']);
    service.getMembresCA.and.returnValue(of([{ id: 3, nom: 'Camille Martin' }, { id: 4, nom: 'Alex Durand' }]));
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [EmpruntMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal },
      { provide: TokenStorageService, useValue: { getUser: () => ({ id: 3, roles: ['ROLE_ADMIN'] }) } }]
    });
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
    expect(service.emprunter).toHaveBeenCalledWith(2, 4);
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
});
