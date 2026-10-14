import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject } from 'rxjs';
import { HorsServiceMaterielModalComponent } from './hors-service-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { Materiel } from '../../models/materiel';

describe('Saisie du problème ou de la réparation', () => {
  let fixture: ComponentFixture<HorsServiceMaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  const materiel: Materiel = { id: 2, nom: 'Objet', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
    emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['updateHorsService']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [HorsServiceMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal }
    ] });
    fixture = TestBed.createComponent(HorsServiceMaterielModalComponent);
    fixture.componentInstance.materiel = materiel;
  });

  it('demande le problème et refuse une saisie vide', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Quel est le problème ?');
    fixture.componentInstance.commentaire = '   ';
    fixture.componentInstance.enregistrer();
    expect(service.updateHorsService).not.toHaveBeenCalled();
    const resultat = { ...materiel, horsService: true, probleme: 'Lampe cassée' };
    service.updateHorsService.and.returnValue(of(resultat));
    fixture.componentInstance.commentaire = '  Lampe cassée  ';
    fixture.componentInstance.enregistrer();
    expect(service.updateHorsService).toHaveBeenCalledWith(2, true, 'Lampe cassée');
    expect(modal.close).toHaveBeenCalledWith(resultat);
  });

  it('demande la réparation lors de la remise en service', () => {
    fixture.componentInstance.materiel = { ...materiel, horsService: true };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Quelle réparation a été effectuée ?');
    service.updateHorsService.and.returnValue(of(materiel));
    fixture.componentInstance.commentaire = 'Lampe remplacée';
    fixture.componentInstance.enregistrer();
    expect(service.updateHorsService).toHaveBeenCalledWith(2, false, 'Lampe remplacée');
  });

  it('empêche les doubles validations et conserve la saisie en cas d’erreur', () => {
    const requete = new Subject<Materiel>();
    service.updateHorsService.and.returnValue(requete);
    fixture.componentInstance.commentaire = 'Lampe cassée';
    fixture.componentInstance.enregistrer();
    fixture.componentInstance.enregistrer();
    expect(service.updateHorsService).toHaveBeenCalledTimes(1);
    requete.error({ error: { detail: 'Enregistrement impossible' } });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Enregistrement impossible');
    expect(fixture.componentInstance.commentaire).toBe('Lampe cassée');
    expect(fixture.componentInstance.enregistrement).toBeFalse();
    expect(modal.close).not.toHaveBeenCalled();
  });
});
