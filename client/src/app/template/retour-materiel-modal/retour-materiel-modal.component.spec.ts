import { TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, throwError } from 'rxjs';
import { RetourMaterielModalComponent } from './retour-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Materiel } from '../../models/materiel';

describe('Retours de matériel par quantité', () => {
  const emprunt = { id: 11, emprunteurId: 3, emprunteurNom: 'Pierre', quantite: 3, quantiteRetournee: 0,
    dateEmprunt: '2026-10-15T08:00:00Z', dateRetour: null, enregistreParNom: 'Pierre', rapporteParNom: null };
  const tables: Materiel = { id: 1, nom: 'Tables', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
    emprunteurId: 3, emprunteurNom: 'Pierre', dateEmprunt: emprunt.dateEmprunt, horsService: false,
    quantiteTotale: 10, quantiteDisponible: 2, quantiteEmpruntee: 8, quantiteHorsService: 0,
    emprunts: [emprunt, { ...emprunt, id: 12, emprunteurId: 4, emprunteurNom: 'Marie', quantite: 5 }] };
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['restituer']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [RetourMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal },
      { provide: TokenStorageService, useValue: { getUser: () => ({ id: 3, roles: ['ROLE_MEMBRECA'] }) } }
    ] });
  });
  it('limite le choix aux emprunts personnels et enregistre un retour partiel', () => {
    const component = TestBed.createComponent(RetourMaterielModalComponent).componentInstance;
    component.materiel = tables;
    expect(component.emprunts.map(e => e.id)).toEqual([11]);
    expect(component.restant).toBe(3);
    component.quantite = 2;
    const resultat = { ...tables, quantiteDisponible: 4 };
    service.restituer.and.returnValue(of(resultat));
    component.enregistrer();
    expect(service.restituer).toHaveBeenCalledWith(1, 11, 2);
    expect(modal.close).toHaveBeenCalledWith(resultat);
  });
  it('refuse les quantités invalides et conserve les saisies après un conflit', () => {
    const component = TestBed.createComponent(RetourMaterielModalComponent).componentInstance;
    component.materiel = tables;
    for (const quantite of [0, -1, 4, 1.5]) { component.quantite = quantite; component.enregistrer(); }
    expect(service.restituer).not.toHaveBeenCalled();
    service.restituer.and.returnValue(throwError(() => ({ error: { detail: 'Emprunt déjà restitué' } })));
    component.quantite = 2; component.enregistrer();
    expect(component.quantite).toBe(2);
    expect(component.enregistrement).toBeFalse();
    expect(component.erreur).toBe('Emprunt déjà restitué');
  });
});
