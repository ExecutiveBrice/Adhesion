import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { ModifierMaterielModalComponent } from './modifier-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { Materiel, Zonage } from '../../models/materiel';

describe('Modification du matériel', () => {
  let fixture: ComponentFixture<ModifierMaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  const zone: Zonage = { id: 1, nom: 'Stock', couleur: '#123456' };
  const reserve: Zonage = { id: 6, nom: 'Réserve', couleur: '#654321' };
  const objet: Materiel = { id: 2, nom: 'Projecteur', zonage: zone, emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: true };

  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['update', 'delete']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [ModifierMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal }
    ] });
    fixture = TestBed.createComponent(ModifierMaterielModalComponent);
    fixture.componentInstance.materiel = objet;
    fixture.componentInstance.zonages = [zone, reserve];
  });

  it('préremplit le formulaire et enregistre le nom et le nouveau zonage', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#modifierMaterielNom').value).toBe('Projecteur');
    const component = fixture.componentInstance;
    expect(component.zonageId).toBe(1);
    component.nom = '  Enceinte  ';
    component.zonageId = 6;
    const resultat = { ...objet, nom: 'Enceinte', zonage: reserve };
    service.update.and.returnValue(of(resultat));
    component.enregistrer();
    expect(service.update).toHaveBeenCalledWith(2, 'Enceinte', 6);
    expect(modal.close).toHaveBeenCalledWith({ materiel: resultat });
    expect(objet.nom).toBe('Projecteur');
  });

  it('refuse un nom vide ou un zonage manquant', () => {
    const component = fixture.componentInstance;
    component.nom = '   ';
    component.enregistrer();
    component.nom = 'Objet';
    component.zonageId = null;
    component.enregistrer();
    expect(service.update).not.toHaveBeenCalled();
  });

  it('conserve les saisies et permet de réessayer après une erreur sans requête en double', () => {
    const component = fixture.componentInstance;
    const request = new Subject<Materiel>();
    service.update.and.returnValue(request);
    component.nom = 'Enceinte';
    component.enregistrer();
    component.enregistrer();
    expect(service.update).toHaveBeenCalledTimes(1);
    request.error({ error: { detail: 'Zonage introuvable' } });
    expect(component.enregistrement).toBeFalse();
    expect(component.nom).toBe('Enceinte');
    expect(component.erreur).toBe('Zonage introuvable');
    expect(modal.close).not.toHaveBeenCalled();
  });

  it('demande confirmation avant de supprimer et désactive les actions pendant la requête', () => {
    const component = fixture.componentInstance;
    component.supprimer();
    expect(service.delete).not.toHaveBeenCalled();
    fixture.detectChanges();
    const boutons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    boutons.find(bouton => bouton.textContent?.trim() === 'Supprimer')!.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('son journal d’emprunt');
    const request = new Subject<void>();
    service.delete.and.returnValue(request);
    component.supprimer();
    component.supprimer();
    expect(service.delete).toHaveBeenCalledTimes(1);
    expect(component.enregistrement).toBeTrue();
    request.next();
    expect(modal.close).toHaveBeenCalledWith({ supprimeId: 2 });
  });

  it('bloque la suppression pendant un emprunt et affiche un conflit serveur', () => {
    const component = fixture.componentInstance;
    component.materiel = { ...objet, emprunteurId: 3 };
    component.confirmationSuppression = true;
    component.supprimer();
    expect(service.delete).not.toHaveBeenCalled();
    component.materiel = objet;
    service.delete.and.returnValue(throwError(() => ({ status: 409, error: { detail: 'Matériel emprunté' } })));
    component.supprimer();
    expect(component.erreur).toBe('Matériel emprunté');
    expect(component.enregistrement).toBeFalse();
    expect(modal.close).not.toHaveBeenCalled();
  });
});
