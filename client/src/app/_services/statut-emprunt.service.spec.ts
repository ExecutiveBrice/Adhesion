import { TestBed } from '@angular/core/testing';
import { EmpruntMateriel, Materiel } from '../models/materiel';
import { StatutEmpruntService } from './statut-emprunt.service';

describe('Statut des emprunts selon le calendrier', () => {
  let service: StatutEmpruntService;
  const emprunt: EmpruntMateriel = {
    id: 1, emprunteurId: 3, emprunteurNom: 'Camille', quantite: 3, quantiteRetournee: 0,
    dateEmprunt: '2026-11-01T10:00:00Z', dateDebut: '2026-11-06', dateFinPrevue: '2026-11-07',
    dateRetour: null, enregistreParNom: 'Camille', rapporteParNom: null
  };

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 10, 5, 23, 59, 59));
    TestBed.configureTestingModule({});
    service = TestBed.inject(StatutEmpruntService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    jasmine.clock().uninstall();
  });

  it('reste à venir avant le début et passe automatiquement en cours à minuit', () => {
    expect(service.statut(emprunt)).toBe('À venir');
    jasmine.clock().tick(1000);
    expect(service.statut(emprunt)).toBe('En cours');
  });

  it('reste en cours après la fin prévue tant que le matériel n’est pas rapporté', () => {
    jasmine.clock().tick(3 * 24 * 60 * 60 * 1000);
    expect(service.statut(emprunt)).toBe('En cours');
  });

  it('donne la priorité au retour et conserve les anciens emprunts en cours', () => {
    expect(service.statut({ ...emprunt, dateRetour: '2026-11-05T10:00:00Z' })).toBe('Rapporté');
    expect(service.statut({ ...emprunt, dateDebut: null })).toBe('En cours');
  });

  it('distingue les quantités à venir des quantités déjà empruntées et des retours', () => {
    const materiel = { emprunts: [emprunt,
      { ...emprunt, id: 2, dateDebut: '2026-11-05', quantiteRetournee: 1 },
      { ...emprunt, id: 3, dateRetour: '2026-11-05T10:00:00Z', quantiteRetournee: 3 }
    ] } as Materiel;
    expect(service.quantite(materiel, 'À venir')).toBe(3);
    expect(service.quantite(materiel, 'En cours')).toBe(2);
    jasmine.clock().tick(1000);
    expect(service.quantite(materiel, 'À venir')).toBe(0);
    expect(service.quantite(materiel, 'En cours')).toBe(5);
  });
});
