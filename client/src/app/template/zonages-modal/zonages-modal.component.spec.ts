import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject } from 'rxjs';
import { ZonagesModalComponent } from './zonages-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { ApiRenderService } from '../../_services/api-render.service';
import { Zonage } from '../../models/materiel';

describe('Modale de gestion des zonages', () => {
  let fixture: ComponentFixture<ZonagesModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;

  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getZonages', 'createZonage']);
    service.getZonages.and.returnValue(of([]));
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [ZonagesModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal }
    ] });
    fixture = TestBed.createComponent(ZonagesModalComponent);
    fixture.detectChanges();
  });

  it('affiche les formulaires et bloque la fermeture pendant un enregistrement', () => {
    expect(fixture.nativeElement.textContent).toContain('Gérer les zonages');
    expect(fixture.nativeElement.querySelector('#zonageNom')).not.toBeNull();
    const request = new Subject<Zonage>();
    service.createZonage.and.returnValue(request);
    const zonages = fixture.componentInstance.zonages!;
    zonages.nom = 'Réserve';
    zonages.ajouter();
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    const fermer: HTMLButtonElement = fixture.nativeElement.querySelector('.modal-footer button');
    const croix: HTMLButtonElement = fixture.nativeElement.querySelector('.btn-close');
    expect(fermer.disabled).toBeTrue();
    expect(croix.disabled).toBeTrue();
    fermer.click();
    expect(modal.close).not.toHaveBeenCalled();
    request.next({ id: 1, nom: 'Réserve', couleur: '#0F9D58' });
    request.complete();
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(fermer.disabled).toBeFalse();
    expect(croix.disabled).toBeFalse();
    fermer.click();
    expect(modal.close).toHaveBeenCalled();
  });
});
