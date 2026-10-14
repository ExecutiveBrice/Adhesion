import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ZonagesComponent } from './zonages.component';
import { MaterielService } from '../../_services/materiel.service';
import { Zonage } from '../../models/materiel';
import { ApiRenderService } from '../../_services/api-render.service';

describe('Paramétrage des zonages', () => {
  let fixture: ComponentFixture<ZonagesComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  const zone: Zonage = { id: 1, nom: 'Stock', couleur: '#123456' };
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getZonages', 'createZonage', 'updateZonage', 'deleteZonage']);
    service.getZonages.and.returnValue(of([zone]));
    TestBed.configureTestingModule({ imports: [ZonagesComponent], providers: [{ provide: MaterielService, useValue: service }] });
    fixture = TestBed.createComponent(ZonagesComponent);
    fixture.detectChanges();
  });

  it('ajoute un zonage avec son libellé et sa couleur', () => {
    const component = fixture.componentInstance;
    component.nom = '  Étagère A  ';
    component.couleur = '#ABCDEF';
    service.createZonage.and.returnValue(of({ id: 2, nom: 'Étagère A', couleur: '#ABCDEF' }));
    component.ajouter();
    fixture.detectChanges();
    expect(service.createZonage).toHaveBeenCalledWith('Étagère A', '#ABCDEF');
    expect(component.zonages.length).toBe(2);
    expect(component.nom).toBe('');
  });

  it('enregistre les modifications et supprime un zonage vide', () => {
    const component = fixture.componentInstance;
    const modifie = { ...zone, nom: 'Réserve', couleur: '#ABCDEF' };
    service.updateZonage.and.returnValue(of(modifie));
    component.enregistrer(modifie);
    expect(component.zonages[0]).toEqual(modifie);
    service.deleteZonage.and.returnValue(of(undefined));
    component.supprimer(modifie);
    expect(service.deleteZonage).toHaveBeenCalledWith(1);
    expect(component.zonages.length).toBe(0);
  });

  it('conserve les zonages contenant du matériel et affiche la raison du refus', () => {
    const component = fixture.componentInstance;
    service.deleteZonage.and.returnValue(throwError(() => ({ error: { detail: 'Ce zonage contient du matériel' } })));
    component.supprimer(zone);
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(component.zonages).toEqual([zone]);
    expect(component.enregistrement).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Ce zonage contient du matériel');
  });
});
