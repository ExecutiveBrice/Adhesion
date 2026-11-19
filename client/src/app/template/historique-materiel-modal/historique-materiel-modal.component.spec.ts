import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { HistoriqueMaterielModalComponent } from './historique-materiel-modal.component';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { EmpruntMateriel, Materiel } from '../../models/materiel';

describe('Journal des emprunts', () => {
  const materiel: Materiel = { id: 2, nom: 'Objet', zonage: { id: 1, nom: 'Stock', couleur: '#123456' },
    emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  let fixture: ComponentFixture<HistoriqueMaterielModalComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getHistorique', 'annulerReservation', 'commencerReservation']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [HistoriqueMaterielModalComponent], providers: [
      { provide: MaterielService, useValue: service }, { provide: NgbActiveModal, useValue: modal },
      { provide: TokenStorageService, useValue: { getUser: () => ({ id: 4, roles: ['ROLE_MEMBRECA'] }) } }]
    });
    fixture = TestBed.createComponent(HistoriqueMaterielModalComponent);
    fixture.componentInstance.materiel = materiel;
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
      dateRetour: null, enregistreParNom: 'Camille Martin', rapporteParNom: null, horsService: true, commentaire: 'Lampe cassée' };
    service.getHistorique.and.returnValue(of([evenement, { ...evenement, id: 2, horsService: false, commentaire: 'Lampe remplacée' }]));
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Déclaré hors service');
    expect(text).toContain('Remis en service');
    expect(text).toContain('Lampe cassée');
    expect(text).toContain('Lampe remplacée');
    expect(text).toContain('Réparation effectuée');
    expect(text).toContain('Camille Martin');
    expect(text).toContain('10/10/2026');
    expect(text).not.toContain('Non rapporté');
    expect(text).not.toContain('En cours');
  });

  it('affiche un emprunt futur à venir plutôt qu’en cours', () => {
    service.getHistorique.and.returnValue(of([{
      id: 1, emprunteurId: 4, emprunteurNom: 'Alex Durand', dateEmprunt: '2026-10-10T10:00:00Z',
      dateDebut: '9999-01-01', dateFinPrevue: '9999-01-02',
      dateRetour: null, enregistreParNom: 'Camille Martin', rapporteParNom: null
    }]));
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('À venir');
    expect(text).toContain('1 prévu(s)');
    expect(text).not.toContain('En cours');
    expect(text).not.toContain('Début prévu');
    expect(text).toContain('Début emprunt');
  });

  it('demande confirmation avant d’annuler une réservation personnelle dans le journal', () => {
    const reservation: EmpruntMateriel = {
      id: 7, emprunteurId: 4, emprunteurNom: 'Alex Durand', dateEmprunt: '2026-10-10T10:00:00Z',
      dateDebut: '9999-01-01', dateFinPrevue: '9999-01-02', reservation: true,
      dateRetour: null, enregistreParNom: 'Alex Durand', rapporteParNom: null
    };
    service.getHistorique.and.returnValue(of([
      reservation, { ...reservation, id: 8, emprunteurId: 9 }, { ...reservation, id: 9, reservation: false },
      { ...reservation, id: 10, dateAnnulation: '2026-10-11T10:00:00Z' }
    ]));
    const requete = new Subject<Materiel>();
    service.annulerReservation.and.returnValue(requete);
    fixture.detectChanges();

    const boutons = (libelle: string) => Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button'))
      .filter(bouton => bouton.textContent?.trim() === libelle);
    expect(boutons('Annuler la réservation').length).toBe(1);
    expect(boutons('Enregistrer la remise').length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Annulé');
    boutons('Annuler la réservation')[0].click();
    fixture.detectChanges();
    expect(service.annulerReservation).not.toHaveBeenCalled();
    const confirmer = boutons('Confirmer l’annulation')[0];
    confirmer.click();
    fixture.detectChanges();
    expect(confirmer.disabled).toBeTrue();
    expect(service.annulerReservation).toHaveBeenCalledOnceWith(2, 7);

    requete.next(materiel);
    expect(modal.close).toHaveBeenCalledWith({ materiel, message: 'Réservation annulée.' });
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
