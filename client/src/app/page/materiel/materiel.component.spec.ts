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
  const disponible: Materiel = { id: 2, nom: 'Vidéoprojecteur', zonage: zone, emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false };
  const emprunte: Materiel = { ...disponible, emprunteurId: 3, emprunteurNom: 'Camille Martin', dateEmprunt: '2026-10-10T10:30:00Z' };

  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getAll', 'getZonages', 'emprunter', 'restituer', 'updateHorsService']);
    service.getAll.and.returnValue(of([disponible]));
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

  it('permet au CA d’emprunter puis de restituer et affiche le nom et la date', () => {
    service.emprunter.and.returnValue(of(emprunte));
    service.restituer.and.returnValue(of(disponible));
    const component = afficher();
    expect(fixture.nativeElement.textContent).not.toContain('Ajouter du matériel');
    expect(fixture.nativeElement.querySelector('[aria-label="Modifier Vidéoprojecteur"]')).toBeNull();
    fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').click();
    fixture.detectChanges();
    expect(service.emprunter).toHaveBeenCalledWith(2);
    expect(fixture.nativeElement.textContent).toContain('Camille Martin');
    expect(fixture.nativeElement.textContent).toContain('10/10/2026');
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]')).toBeNull();
    fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]').click();
    fixture.detectChanges();
    expect(component.materiels[0].emprunteurId).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Disponible');
  });

  it('permet l’ajout à l’administrateur et la restitution à l’emprunteur ou l’administrateur', () => {
    service.getAll.and.returnValue(of([{ ...emprunte, emprunteurId: 4 }]));
    afficher();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).toBeNull();
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_ADMIN'] });
    afficher();
    expect(fixture.nativeElement.textContent).toContain('Ajouter du matériel');
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).not.toBeNull();
  });

  it('empêche les doubles clics pendant un emprunt', () => {
    const request = new Subject<Materiel>();
    service.emprunter.and.returnValue(request);
    const component = afficher();
    component.emprunter(disponible);
    component.emprunter(disponible);
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(service.emprunter).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').disabled).toBeTrue();
    request.next(emprunte);
    request.complete();
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

  it('actualise le listing après un conflit d’emprunt et affiche sa raison', () => {
    const component = afficher();
    service.getAll.and.returnValue(of([emprunte]));
    service.emprunter.and.returnValue(throwError(() => ({ status: 409, error: { detail: 'Ce matériel est déjà emprunté' } })));
    component.emprunter(disponible);
    TestBed.inject(ApiRenderService).notify();
    fixture.detectChanges();
    expect(component.actionEnCours).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Ce matériel est déjà emprunté');
    expect(fixture.nativeElement.textContent).toContain('Camille Martin');
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

  it('HS reste un indicateur et autorise l’emprunt avec le filtre disponible', () => {
    service.getAll.and.returnValue(of([{ ...disponible, horsService: true }]));
    service.emprunter.and.returnValue(of({ ...emprunte, horsService: true }));
    const component = afficher();
    const hs = { ...disponible, horsService: true };
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]')).not.toBeNull();
    const bouton: HTMLButtonElement = fixture.nativeElement.querySelector('#materielHS2');
    expect(bouton.disabled).toBeTrue();
    expect(bouton.classList.contains('btn-danger')).toBeTrue();
    component.statut = 'disponible';
    expect(component.materielsFiltres).toEqual([hs]);
    component.emprunter(hs);
    expect(service.emprunter).toHaveBeenCalledWith(2);
    expect(component.materielsFiltres).toEqual([]);
    component.statut = 'emprunte';
    expect(component.materielsFiltres.length).toBe(1);
    expect(component.peutRestituer(component.materiels[0])).toBeTrue();
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
