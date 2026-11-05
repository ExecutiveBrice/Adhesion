import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject } from 'rxjs';
import { MaterielModalComponent } from './materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { Materiel } from '../../models/materiel';
import { By } from '@angular/platform-browser';
import { PhotoMaterielComponent } from '../photo-materiel/photo-materiel.component';

describe('Ajout de matériel', () => {
  let fixture: ComponentFixture<MaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['create']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [MaterielModalComponent], providers: [provideRouter([]),
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal }] });
    fixture = TestBed.createComponent(MaterielModalComponent);
  });

  it('bloque l’ajout sans zonage et indique le bouton de gestion sur la page Matériel', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ajoutez d’abord un zonage');
    expect(fixture.nativeElement.textContent).toContain('Gérer les zonages sur la page Matériel');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    fixture.componentInstance.ajouter();
    expect(service.create).not.toHaveBeenCalled();
  });

  it('ajoute le nom normalisé dans le zonage choisi et ferme la modale avec le résultat', () => {
    const component = fixture.componentInstance;
    component.zonages = [{ id: 1, nom: 'Stock', couleur: '#123456' }];
    component.nom = '  Vidéoprojecteur  ';
    component.zonageId = 1;
    const objet: Materiel = { id: 2, nom: 'Vidéoprojecteur', zonage: component.zonages[0], emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
    service.create.and.returnValue(of(objet));
    component.ajouter();
    expect(service.create).toHaveBeenCalledWith('Vidéoprojecteur', 1, false, 1, undefined);
    expect(modal.close).toHaveBeenCalledWith(objet);
  });

  it('conserve le formulaire en cas d’échec et évite un ajout en double', () => {
    const component = fixture.componentInstance;
    component.nom = 'Objet';
    component.zonageId = 1;
    const request = new Subject<Materiel>();
    service.create.and.returnValue(request);
    component.ajouter();
    component.ajouter();
    expect(service.create).toHaveBeenCalledTimes(1);
    request.error({ error: { detail: 'Zonage introuvable' } });
    expect(component.enregistrement).toBeFalse();
    expect(component.nom).toBe('Objet');
    expect(component.erreur).toBe('Zonage introuvable');
    expect(modal.close).not.toHaveBeenCalled();
  });

  it('attend la lecture de la photo puis la transmet avec le matériel', () => {
    const component = fixture.componentInstance;
    component.zonages = [{ id: 1, nom: 'Stock', couleur: '#123456' }];
    component.nom = 'Objet';
    component.zonageId = 1;
    fixture.detectChanges();
    const photo = fixture.debugElement.query(By.directive(PhotoMaterielComponent)).componentInstance as PhotoMaterielComponent;
    photo.chargementChange.emit(true);
    component.ajouter();
    expect(service.create).not.toHaveBeenCalled();
    photo.imageChange.emit('data:image/png;base64,cGhvdG8=');
    photo.chargementChange.emit(false);
    service.create.and.returnValue(new Subject<Materiel>());
    component.ajouter();
    expect(service.create).toHaveBeenCalledWith('Objet', 1, false, 1, 'data:image/png;base64,cGhvdG8=');
  });
});
