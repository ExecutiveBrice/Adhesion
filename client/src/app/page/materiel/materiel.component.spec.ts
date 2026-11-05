import { RetourMaterielModalComponent } from '../../template/retour-materiel-modal/retour-materiel-modal.component';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { MaterielComponent } from './materiel.component';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { Materiel, Zonage } from '../../models/materiel';
import { ApiRenderService } from '../../_services/api-render.service';
import { EmpruntMaterielModalComponent } from '../../template/emprunt-materiel-modal/emprunt-materiel-modal.component';
import { ModifierMaterielModalComponent } from '../../template/modifier-materiel-modal/modifier-materiel-modal.component';
import { MaterielModalComponent } from '../../template/materiel-modal/materiel-modal.component';
import { HorsServiceMaterielModalComponent } from '../../template/hors-service-materiel-modal/hors-service-materiel-modal.component';
import { ZonagesModalComponent } from '../../template/zonages-modal/zonages-modal.component';

describe('Matériel et emprunts', () => {
  let fixture: ComponentFixture<MaterielComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let storage: jasmine.SpyObj<TokenStorageService>;
  const zone: Zonage = { id: 1, nom: 'Étagère A', couleur: '#123456' };
  const disponible: Materiel = { id: 2, nom: 'Vidéoprojecteur', zonage: zone, emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false, quantiteTotale: 1, quantiteDisponible: 1, quantiteEmpruntee: 0, quantiteHorsService: 0, emprunts: [] };
  const emprunte: Materiel = { ...disponible, emprunteurId: 3, emprunteurNom: 'Camille Martin', dateEmprunt: '2026-10-10T10:30:00Z', quantiteDisponible: 0, quantiteEmpruntee: 1, emprunts: [{ id: 10, emprunteurId: 3, emprunteurNom: 'Camille Martin', dateEmprunt: '2026-10-10T10:30:00Z', dateRetour: null, enregistreParNom: null, rapporteParNom: null, quantite: 1, quantiteRetournee: 0 }] };

  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getAll', 'getCalendrier', 'getZonages', 'emprunter', 'restituer', 'updateHorsService']);
    service.getAll.and.returnValue(of([disponible]));
    service.getCalendrier.and.returnValue(of([]));
    service.getZonages.and.returnValue(of([zone]));
    storage = jasmine.createSpyObj('TokenStorageService', ['getUser']);
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_MEMBRECA'] });
    TestBed.configureTestingModule({
      imports: [MaterielComponent],
      providers: [provideRouter([]),
        { provide: MaterielService, useValue: service },
        { provide: TokenStorageService, useValue: storage },
        { provide: NgbModal, useValue: { open: jasmine.createSpy('open') } }]
    });
  });

  function afficher() {
    fixture = TestBed.createComponent(MaterielComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('affiche une carte par ligne sur petit écran et garde une zone photo au format 4:3', () => {
    afficher();
    const cadre = document.createElement('iframe');
    document.body.appendChild(cadre);
    try {
      const doc = cadre.contentDocument!;
      document.querySelectorAll('style').forEach(style => doc.head.appendChild(style.cloneNode(true)));
      doc.body.appendChild(fixture.nativeElement.cloneNode(true));
      const grille = doc.querySelector('.materiel-grid')!;
      for (const [largeur, colonnes] of [[375, 1], [767, 1], [768, 2], [1280, 3]]) {
        cadre.style.width = `${largeur}px`;
        expect(cadre.contentWindow!.getComputedStyle(grille).gridTemplateColumns.split(' ').length).toBe(colonnes);
        const photo = doc.querySelector('.materiel-photo')!.getBoundingClientRect();
        expect(photo.width / photo.height).toBeCloseTo(4 / 3, 2);
      }
    } finally {
      cadre.remove();
    }
  });

  it('réserve un emplacement photo et permet son ajout aux gestionnaires', () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const reference = { componentInstance: {}, result: new Promise(() => {}) };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    afficher();
    const bouton = fixture.nativeElement.querySelector('[aria-label="Ajouter une photo de Vidéoprojecteur"]');
    expect(bouton).not.toBeNull();
    bouton.click();
    expect(open).toHaveBeenCalledWith(ModifierMaterielModalComponent, jasmine.any(Object));
  });

  it('affiche la photo sauvegardée sans les noms ni les dates des emprunteurs', () => {
    const image = 'data:image/jpeg;base64,cGhvdG8=';
    service.getAll.and.returnValue(of([{ ...emprunte, image }]));
    afficher();
    const carte = fixture.nativeElement.querySelector('.materiel-card');
    expect(carte.querySelector('.materiel-photo img').getAttribute('src')).toBe(image);
    expect(carte.querySelector('img').getAttribute('alt')).toBe('Photo de Vidéoprojecteur');
    expect(carte.textContent).not.toContain('Camille Martin');
    expect(carte.textContent).not.toContain('Début emprunt');
  });

  it('filtre le matériel au clic sur un emprunt du jour sélectionné, même déjà rapporté et masqué par les filtres', async () => {
    service.getAll.and.returnValue(of([disponible, { ...disponible, id: 5, nom: 'Enceinte' }]));
    service.getCalendrier.and.returnValue(of([{
      materielId: disponible.id, materielNom: disponible.nom, zonage: zone,
      emprunt: { ...emprunte.emprunts![0], dateDebut: '2000-01-01', dateRetour: new Date().toISOString() }
    }]));
    const component = afficher();
    await fixture.whenStable();
    component.recherche = 'introuvable';
    component.zonageId = 999;
    component.statut = 'emprunte';
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.materiel-card').length).toBe(0);
    const recherche = fixture.nativeElement.querySelector('#materielRecherche') as HTMLInputElement;
    const defiler = spyOn(recherche, 'scrollIntoView');

    fixture.nativeElement.querySelector('.emprunt-detail').click();
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();

    expect(recherche.value).toBe(disponible.nom);
    expect(component.zonageId).toBeNull();
    expect(component.statut).toBe('');
    expect(component.materielsFiltres).toEqual([disponible]);
    const cartes = fixture.nativeElement.querySelectorAll('.materiel-card');
    expect(cartes.length).toBe(1);
    expect(cartes[0].textContent).toContain(disponible.nom);
    expect(defiler).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' });
  });

  it('ouvre les formulaires de quantité pour emprunter et restituer', async () => {
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue({ componentInstance: {}, result: Promise.resolve(emprunte) });
    const component = afficher();
    fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').click();
    expect(open).toHaveBeenCalledWith(EmpruntMaterielModalComponent,
      jasmine.objectContaining({ size: 'lg', scrollable: true }));
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('Camille Martin');
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('10/10/2026');
    expect(fixture.nativeElement.querySelector('.materiel-photo')).not.toBeNull();
    open.and.returnValue({ componentInstance: {}, result: Promise.resolve(disponible) });
    fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]').click();
    expect(open).toHaveBeenCalledWith(RetourMaterielModalComponent, jasmine.any(Object));
    await fixture.whenStable(); fixture.detectChanges();
    expect(component.materiels[0].quantiteDisponible).toBe(1);
  });

  it('permet l’ajout à l’administrateur et la restitution à l’emprunteur ou l’administrateur', () => {
    service.getAll.and.returnValue(of([{ ...emprunte, emprunteurId: 4, emprunts: emprunte.emprunts!.map(e => ({ ...e, emprunteurId: 4 })) }]));
    afficher();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).toBeNull();
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_ADMIN'] });
    afficher();
    expect(fixture.nativeElement.textContent).toContain('Ajouter du matériel');
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).not.toBeNull();
  });

  it('empêche d’ouvrir deux formulaires pendant une opération', () => {
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue({ componentInstance: {}, result: new Promise(() => {}) });
    const component = afficher();
    fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').click();
    component.emprunter(disponible);
    fixture.detectChanges();
    expect(open).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').disabled).toBeTrue();
  });

  it('distingue les emprunts à venir dans les badges et les filtres', () => {
    const futur: Materiel = { ...emprunte, emprunts: emprunte.emprunts!.map(e => ({
      ...e, dateDebut: '9999-01-01', dateFinPrevue: '9999-01-02'
    })) };
    service.getAll.and.returnValue(of([futur]));
    const component = afficher();
    const text = fixture.nativeElement.querySelector('.materiel-card').textContent;
    expect(text).toContain('1 à venir');
    expect(text).not.toContain('1 emprunté(s)');
    component.statut = 'emprunte';
    expect(component.materielsFiltres).toEqual([]);
    component.statut = 'a-venir';
    expect(component.materielsFiltres).toEqual([futur]);
  });

  it('permet à la Logistique d’ajouter du matériel sans accorder les droits d’emprunt', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const reference = { componentInstance: {}, result: Promise.resolve({ ...disponible, id: 8, nom: 'Enceinte' }) };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    const component = afficher();
    expect(fixture.nativeElement.textContent).toContain('Ajouter du matériel');
    expect(fixture.nativeElement.querySelector('[aria-label="Modifier Vidéoprojecteur"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]')).toBeNull();
    component.emprunter(disponible);
    expect(service.emprunter).not.toHaveBeenCalled();
    component.parametrer();
    expect(open).toHaveBeenCalledWith(MaterielModalComponent, jasmine.any(Object));
    await reference.result;
    expect(component.materiels.map(item => item.id)).toEqual([8, 2]);
  });

  it('actualise le matériel après modification et suppression par la Logistique', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const modifie = { ...disponible, nom: 'Projecteur', zonage: { ...zone, id: 6, nom: 'Réserve' } };
    const reference = { componentInstance: {}, result: Promise.resolve({ materiel: modifie }) };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    const component = afficher();
    fixture.nativeElement.querySelector('[aria-label="Modifier Vidéoprojecteur"]').click();
    expect(open).toHaveBeenCalledWith(ModifierMaterielModalComponent, jasmine.any(Object));
    expect((reference.componentInstance as any).materiel).toEqual(disponible);
    await reference.result;
    expect(component.materiels).toEqual([modifie]);
    const suppression = { componentInstance: {}, result: Promise.resolve({ supprimeId: 2 }) };
    open.and.returnValue(suppression);
    component.modifier(modifie);
    await suppression.result;
    expect(component.materiels).toEqual([]);
    expect(component.message).toBe('Matériel supprimé.');
  });

  it('refuse l’ouverture de la modification au CA et conserve le matériel après annulation', async () => {
    const component = afficher();
    component.modifier(disponible);
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    expect(open).not.toHaveBeenCalled();
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_ADMIN'] });
    const annulation = { componentInstance: {}, result: Promise.reject('annulation') };
    open.and.returnValue(annulation);
    const admin = afficher();
    admin.modifier(disponible);
    await annulation.result.catch(() => {});
    expect(admin.materiels).toEqual([disponible]);
    expect(admin.actionEnCours).toBeNull();
  });

  it('ouvre la gestion des zonages pour la Logistique et actualise la liste à la fermeture', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const reference = { componentInstance: { enregistrement: false }, result: Promise.resolve() };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    const component = afficher();
    const nouvelleZone = { ...zone, nom: 'Réserve', couleur: '#ABCDEF' };
    service.getZonages.and.returnValue(of([nouvelleZone]));
    service.getAll.and.returnValue(of([{ ...disponible, zonage: nouvelleZone }]));
    const bouton = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(item => item.textContent?.trim() === 'Gérer les zonages');
    expect(bouton).toBeDefined();
    bouton!.click();
    expect(open).toHaveBeenCalledWith(ZonagesModalComponent, jasmine.objectContaining({ size: 'lg', scrollable: true }));
    const options = open.calls.mostRecent().args[1];
    expect(options.beforeDismiss()).toBeTrue();
    reference.componentInstance.enregistrement = true;
    expect(options.beforeDismiss()).toBeFalse();
    await reference.result;
    expect(component.zonages).toEqual([nouvelleZone]);
    expect(component.materiels[0].zonage).toEqual(nouvelleZone);
  });

  it('réserve les zonages aux gestionnaires et réinitialise un filtre supprimé après Échap', async () => {
    const ca = afficher();
    ca.gererZonages();
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    expect(open).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('Gérer les zonages');
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_ADMIN'] });
    const reference = { componentInstance: {}, result: Promise.reject('Échap') };
    open.and.returnValue(reference);
    const admin = afficher();
    admin.zonageId = zone.id;
    service.getZonages.and.returnValue(of([]));
    service.getAll.and.returnValue(of([]));
    admin.gererZonages();
    await reference.result.catch(() => {});
    expect(admin.zonages).toEqual([]);
    expect(admin.zonageId).toBeNull();
    expect(service.getAll).toHaveBeenCalledTimes(3);
  });

  it('filtre par nom, zonage et disponibilité', () => {
    const component = afficher();
    component.materiels = [disponible, { ...emprunte, id: 5, nom: 'Enceinte', zonage: { ...zone, id: 6, nom: 'Réserve' } }];
    component.recherche = 'camille';
    expect(component.materielsFiltres.map(m => m.id)).toEqual([5]);
    component.recherche = '';
    component.zonageId = 1;
    expect(component.materielsFiltres.map(m => m.id)).toEqual([2]);
    component.zonageId = null;
    component.statut = 'emprunte';
    expect(component.materielsFiltres.map(m => m.id)).toEqual([5]);
  });

  it('ouvre la sélection pour un administrateur sans emprunter avant confirmation', async () => {
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_ADMIN'] });
    const reference = { componentInstance: {}, result: Promise.resolve({ ...emprunte, emprunteurId: 4, emprunteurNom: 'Autre membre du CA' }) };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    const component = afficher();
    component.emprunter(disponible);
    expect(open).toHaveBeenCalledWith(EmpruntMaterielModalComponent, jasmine.any(Object));
    expect((reference.componentInstance as any).materiel).toEqual(disponible);
    expect(service.emprunter).not.toHaveBeenCalled();
    await reference.result;
    expect(component.materiels[0].emprunteurId).toBe(4);
    expect(component.actionEnCours).toBeNull();
  });

  it('une référence partiellement empruntée reste disponible et restituable', () => {
    const tables = { ...emprunte, nom: 'Tables', quantiteTotale: 10, quantiteEmpruntee: 8, quantiteDisponible: 1, quantiteHorsService: 1, horsService: true };
    service.getAll.and.returnValue(of([tables]));
    const component = afficher();
    component.statut = 'disponible';
    expect(component.materielsFiltres).toEqual([tables]);
    component.statut = 'emprunte';
    expect(component.materielsFiltres).toEqual([tables]);
    expect(component.peutRestituer(tables)).toBeTrue();
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Tables"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Tables"]')).not.toBeNull();
  });

  it('ouvre la saisie HS et affiche le problème sans perdre l’emprunt', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_ADMIN'] });
    service.getAll.and.returnValue(of([emprunte]));
    let terminer!: (materiel: Materiel) => void;
    const reference = { componentInstance: { materiel: disponible }, result: new Promise<Materiel>(resolve => terminer = resolve) };
    const modal = TestBed.inject(NgbModal);
    (modal.open as jasmine.Spy).and.returnValue(reference);
    const component = afficher();
    fixture.nativeElement.querySelector('#materielHS2').click();
    expect(modal.open).toHaveBeenCalledWith(HorsServiceMaterielModalComponent, jasmine.any(Object));
    expect(reference.componentInstance.materiel).toEqual(emprunte);
    expect(component.materiels[0].horsService).toBeFalse();
    expect(component.actionEnCours).toBe(2);
    terminer({ ...emprunte, horsService: true, probleme: 'Lampe cassée' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.actionEnCours).toBeNull();
    expect(component.materiels[0].emprunteurNom).toBe('Camille Martin');
    expect(fixture.nativeElement.textContent).toContain('Lampe cassée');
    expect(fixture.nativeElement.querySelector('#materielHS2').getAttribute('aria-pressed')).toBe('true');
  });

  it('annuler la saisie conserve l’état du matériel', async () => {
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_ADMIN'] });
    let annuler!: () => void;
    (TestBed.inject(NgbModal).open as jasmine.Spy).and.returnValue({ componentInstance: {},
      result: new Promise((resolve, reject) => annuler = reject) });
    const component = afficher();
    fixture.nativeElement.querySelector('#materielHS2').click();
    annuler();
    await fixture.whenStable();
    expect(component.materiels[0].horsService).toBeFalse();
    expect(component.actionEnCours).toBeNull();
    expect(service.updateHorsService).not.toHaveBeenCalled();
  });
});
