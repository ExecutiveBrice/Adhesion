import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, throwError } from 'rxjs';
import { HistoriqueMaterielModalComponent } from './historique-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';

describe('Journal des emprunts', () => {
  let fixture: ComponentFixture<HistoriqueMaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getHistorique']);
    TestBed.configureTestingModule({ imports: [HistoriqueMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: {} }]
    });
    fixture = TestBed.createComponent(HistoriqueMaterielModalComponent);
    fixture.componentInstance.materiel = { id: 2, nom: 'Objet', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
      emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  });

  it('affiche les emprunts ouverts et les retours avec les noms des administrateurs', () => {
    const entree = { id: 1, emprunteurId: 4, emprunteurNom: 'Alex Durand', dateEmprunt: '2026-10-10T10:00:00Z',
      dateRetour: null, enregistreParNom: 'Camille Martin', rapporteParNom: null };
    service.getHistorique.and.returnValue(of([entree, { ...entree, id: 2, dateRetour: '2026-10-10T11:00:00Z', rapporteParNom: 'Autre admin' }]));
    fixture.detectChanges();
    expect(service.getHistorique).toHaveBeenCalledWith(2);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Alex Durand'); expect(text).toContain('Camille Martin');
    expect(text).toContain('Autre admin'); expect(text).toContain('En cours'); expect(text).toContain('Rapporté');
    expect(text).toContain('10/10/2026');
  });

  it('affiche les changements HS avec leur date et leur auteur sans les présenter comme des emprunts', () => {
    const evenement = { id: 1, emprunteurId: null, emprunteurNom: null, dateEmprunt: '2026-10-10T10:00:00Z',
      dateRetour: null, enregistreParNom: 'Camille Martin', rapporteParNom: null, horsService: true };
    service.getHistorique.and.returnValue(of([evenement, { ...evenement, id: 2, horsService: false }]));
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Déclaré hors service');
    expect(text).toContain('Remis en service');
    expect(text).toContain('Camille Martin');
    expect(text).toContain('10/10/2026');
    expect(text).not.toContain('Non rapporté');
    expect(text).not.toContain('En cours');
  });

  it('affiche un état vide ou une erreur de chargement', () => {
    service.getHistorique.and.returnValue(of([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun emprunt enregistré');
    fixture.destroy();
    service.getHistorique.and.returnValue(throwError(() => ({})));
    fixture = TestBed.createComponent(HistoriqueMaterielModalComponent);
    fixture.componentInstance.materiel = { id: 2 } as any;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Le journal des emprunts n’a pas pu être chargé');
  });
});
