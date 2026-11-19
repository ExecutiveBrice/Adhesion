import { RetourMaterielModalComponent } from '../../template/retour-materiel-modal/retour-materiel-modal.component';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { NgbModal, NgbModalConfig } from '@ng-bootstrap/ng-bootstrap';
import { MaterielComponent } from './materiel.component';
import { MaterielService } from '../../_services/materiel.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ToastService } from '../../_services/toast.service';
import { EmpruntMateriel, Materiel, Zonage } from '../../models/materiel';
import { ApiRenderService } from '../../_services/api-render.service';
import { StatutEmpruntService } from '../../_services/statut-emprunt.service';
import { EmpruntMaterielModalComponent } from '../../template/emprunt-materiel-modal/emprunt-materiel-modal.component';
import { ModifierMaterielModalComponent } from '../../template/modifier-materiel-modal/modifier-materiel-modal.component';
import { MaterielModalComponent } from '../../template/materiel-modal/materiel-modal.component';
import { HorsServiceMaterielModalComponent } from '../../template/hors-service-materiel-modal/hors-service-materiel-modal.component';
import { ZonagesModalComponent } from '../../template/zonages-modal/zonages-modal.component';
import { HistoriqueMaterielModalComponent } from '../../template/historique-materiel-modal/historique-materiel-modal.component';

describe('Matériel et emprunts', () => {
  let fixture: ComponentFixture<MaterielComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  let storage: jasmine.SpyObj<TokenStorageService>;
  const zone: Zonage = { id: 1, nom: 'Étagère A', couleur: '#123456' };
  const disponible: Materiel = { id: 2, nom: 'Vidéoprojecteur', zonage: zone, emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: false, quantiteTotale: 1, quantiteDisponible: 1, quantiteEmpruntee: 0, quantiteHorsService: 0, emprunts: [] };
  const emprunte: Materiel = { ...disponible, emprunteurId: 3, emprunteurNom: 'Camille Martin', dateEmprunt: '2026-10-10T10:30:00Z', quantiteDisponible: 0, quantiteEmpruntee: 1, emprunts: [{ id: 10, emprunteurId: 3, emprunteurNom: 'Camille Martin', dateEmprunt: '2026-10-10T10:30:00Z', dateRetour: null, enregistreParNom: null, rapporteParNom: null, quantite: 1, quantiteRetournee: 0 }] };

  beforeEach(() => {
    service = jasmine.createSpyObj('MaterielService', ['getAll', 'getCalendrier', 'getZonages', 'getHistorique',
      'emprunter', 'annulerReservation', 'restituer', 'updateHorsService']);
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

  function avecModales() {
    TestBed.overrideProvider(NgbModal, { useFactory: () => new NgbModal() });
    TestBed.overrideProvider(NgbModalConfig, { useFactory: () => Object.assign(new NgbModalConfig(), { animation: false }) });
  }

  function entreeCalendrier(emprunt: EmpruntMateriel) {
    return { materielId: disponible.id, materielNom: disponible.nom, zonage: zone, emprunt };
  }

  function bouton(zone: ParentNode, libelle: string) {
    return Array.from(zone.querySelectorAll('button')).find(element => element.textContent?.trim() === libelle);
  }

  function choisirEmprunt(id: number) {
    fixture.nativeElement.querySelector(`[aria-controls="actions-emprunt-${id}"]`).click();
    fixture.detectChanges();
    return fixture.nativeElement.querySelector(`#actions-emprunt-${id}`) as HTMLElement;
  }

  it('replie le calendrier par défaut et affiche sa navigation à l’ouverture sur mobile et ordinateur', () => {
    afficher();
    const cadre = document.createElement('iframe');
    document.body.appendChild(cadre);
    try {
      const doc = cadre.contentDocument!;
      document.querySelectorAll('style').forEach(style => doc.head.appendChild(style.cloneNode(true)));
      doc.body.appendChild(fixture.nativeElement.cloneNode(true));
      const panneau = doc.querySelector<HTMLDetailsElement>('.calendrier-panneau')!;
      const titre = panneau.querySelector('summary')!;
      const navigation = panneau.querySelector('.calendrier-navigation')!;
      for (const largeur of [375, 1280]) {
        cadre.style.width = `${largeur}px`;
        expect(panneau.open).toBeFalse();
        titre.click();
        expect(panneau.open).toBeTrue();
        expect(cadre.contentWindow!.getComputedStyle(navigation).display).toBe('flex');
        titre.click();
        expect(panneau.open).toBeFalse();
      }
    } finally {
      cadre.remove();
    }
  });

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

  it('permet de retrouver dans la liste le matériel d’un emprunt rapporté, même masqué par les filtres', async () => {
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

    fixture.nativeElement.querySelector('.calendrier-panneau summary').click();
    const actions = choisirEmprunt(10);
    expect(bouton(actions, 'Annuler la réservation')).toBeUndefined();
    expect(bouton(actions, 'Restituer')).toBeUndefined();
    bouton(actions, 'Afficher dans la liste du matériel')!.click();
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

  it('annule la réservation sélectionnée après confirmation et actualise le calendrier', async () => {
    avecModales();
    const jour = TestBed.inject(StatutEmpruntService).dateCourante;
    const reservation = { ...emprunte.emprunts![0], dateDebut: jour, dateFinPrevue: jour, reservation: true };
    service.getAll.and.returnValue(of([{ ...disponible, emprunts: [reservation] }]));
    service.getCalendrier.and.returnValue(of([entreeCalendrier(reservation)]));
    service.annulerReservation.and.returnValue(of(disponible));
    afficher();
    fixture.nativeElement.querySelector('.calendrier-panneau summary').click();

    const actions = choisirEmprunt(reservation.id);
    expect(bouton(actions, 'Restituer')).toBeUndefined();
    bouton(actions, 'Annuler la réservation')!.click();
    await fixture.whenStable();
    const modale = document.querySelector('ngb-modal-window')!;
    expect(service.getHistorique).not.toHaveBeenCalled();
    expect(service.annulerReservation).not.toHaveBeenCalled();
    service.getCalendrier.and.returnValue(of([]));
    bouton(modale, 'Confirmer l’annulation')!.click();
    await fixture.whenStable(); fixture.detectChanges();

    expect(service.annulerReservation).toHaveBeenCalledOnceWith(disponible.id, reservation.id);
    expect(service.getCalendrier).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('.emprunt-detail')).toBeNull();
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('1 à venir');
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Réservation annulée.' }));
  });

  it('prépare le retour de l’emprunt en retard sélectionné et tient compte des retours partiels', async () => {
    avecModales();
    const retard = { ...emprunte.emprunts![0], id: 11, dateDebut: '2026-10-10', dateFinPrevue: '2026-10-11',
      reservation: false, quantite: 4, quantiteRetournee: 2 };
    service.getAll.and.returnValue(of([{ ...emprunte, emprunts: [...emprunte.emprunts!, retard] }]));
    service.getCalendrier.and.returnValue(of([entreeCalendrier(retard)]));
    service.restituer.and.returnValue(of(emprunte));
    afficher();
    fixture.nativeElement.querySelector('.calendrier-panneau summary').click();

    const actions = choisirEmprunt(retard.id);
    expect(bouton(actions, 'Annuler la réservation')).toBeUndefined();
    bouton(actions, 'Restituer')!.click();
    await fixture.whenStable();
    const modale = document.querySelector('ngb-modal-window')!;
    expect(modale.querySelector<HTMLInputElement>('#quantiteRetour')!.valueAsNumber).toBe(2);
    service.getCalendrier.and.returnValue(of([entreeCalendrier({
      ...retard, quantiteRetournee: 4, dateRetour: new Date().toISOString()
    })]));
    bouton(modale, 'Enregistrer le retour')!.click();
    await fixture.whenStable(); fixture.detectChanges();

    expect(service.restituer).toHaveBeenCalledOnceWith(disponible.id, retard.id, 2);
    expect(service.getCalendrier).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('.emprunt-detail .badge').textContent).toBe('Terminé');
    const actionsActualisees = fixture.nativeElement.querySelector(`#actions-emprunt-${retard.id}`);
    expect(bouton(actionsActualisees, 'Restituer')).toBeUndefined();
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Retour enregistré.' }));
  });

  it('réserve les actions du calendrier à l’emprunteur autorisé ou à un administrateur', () => {
    const emprunt = { ...emprunte.emprunts![0], emprunteurId: 7, dateDebut: '2000-01-01' };
    const reservation = { ...emprunt, id: 20, reservation: true };
    service.getCalendrier.and.returnValue(of([entreeCalendrier(emprunt), entreeCalendrier(reservation)]));
    afficher();
    expect(bouton(choisirEmprunt(emprunt.id), 'Restituer')).toBeUndefined();
    expect(bouton(choisirEmprunt(reservation.id), 'Annuler la réservation')).toBeUndefined();
    fixture.destroy();

    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_ADMIN'] });
    afficher();
    expect(bouton(choisirEmprunt(emprunt.id), 'Restituer')).toBeDefined();
    expect(bouton(choisirEmprunt(reservation.id), 'Annuler la réservation')).toBeDefined();
  });

  it('ouvre les formulaires de quantité pour emprunter et restituer', async () => {
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue({ componentInstance: {}, result: Promise.resolve({ materiel: emprunte, reservation: false }) });
    const component = afficher();
    fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').click();
    expect(open).toHaveBeenCalledWith(EmpruntMaterielModalComponent,
      jasmine.objectContaining({ size: 'lg', scrollable: true }));
    await fixture.whenStable(); fixture.detectChanges();
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Emprunt enregistré.', type: 'success' }));
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('Camille Martin');
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('10/10/2026');
    expect(fixture.nativeElement.querySelector('.materiel-photo')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').disabled).toBeFalse();
    open.and.returnValue({ componentInstance: {}, result: Promise.resolve(disponible) });
    fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]').click();
    expect(open).toHaveBeenCalledWith(RetourMaterielModalComponent, jasmine.any(Object));
    await fixture.whenStable(); fixture.detectChanges();
    expect(component.materiels[0].quantiteDisponible).toBe(1);
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Retour enregistré.', type: 'success' }));
  });

  it('confirme une réservation dans un toast', async () => {
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue({ componentInstance: {}, result: Promise.resolve({ materiel: disponible, reservation: true }) });
    afficher();
    fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Réservation enregistrée.', type: 'success' }));
    expect(fixture.nativeElement.textContent).not.toContain('Réservation enregistrée.');
  });

  it('permet à l’administrateur de restituer un emprunt sans lui donner les boutons de gestion', () => {
    service.getAll.and.returnValue(of([{ ...emprunte, emprunteurId: 4, emprunts: emprunte.emprunts!.map(e => ({ ...e, emprunteurId: 4 })) }]));
    afficher();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).toBeNull();
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 3, roles: ['ROLE_ADMIN'] });
    const admin = afficher();
    expect(fixture.nativeElement.textContent).not.toContain('Ajouter du matériel');
    expect(fixture.nativeElement.textContent).not.toContain('Gérer les zonages');
    expect(fixture.nativeElement.querySelector('[aria-label="Modifier Vidéoprojecteur"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Ajouter une photo de Vidéoprojecteur"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).not.toBeNull();
    admin.parametrer();
    admin.modifier(disponible);
    admin.gererZonages();
    expect(TestBed.inject(NgbModal).open).not.toHaveBeenCalled();
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
    const futur: Materiel = { ...disponible, emprunts: emprunte.emprunts!.map(e => ({
      ...e, dateDebut: '9999-01-01', dateFinPrevue: '9999-01-02', reservation: true
    })) };
    service.getAll.and.returnValue(of([futur]));
    const component = afficher();
    const text = fixture.nativeElement.querySelector('.materiel-card').textContent;
    expect(text).toContain('1 à venir');
    expect(text).toContain('1 disponible(s)');
    expect(text).not.toContain('1 emprunté(s)');
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]').disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('[aria-label="Restituer Vidéoprojecteur"]')).toBeNull();
    component.statut = 'emprunte';
    expect(component.materielsFiltres).toEqual([]);
    component.statut = 'a-venir';
    expect(component.materielsFiltres).toEqual([futur]);
  });

  it('actualise la carte et le calendrier après une annulation dans le journal', async () => {
    const ouvrir = TestBed.inject(NgbModal).open as jasmine.Spy;
    ouvrir.and.returnValue({ componentInstance: {}, result: Promise.resolve({ materiel: disponible, message: 'Réservation annulée.' }) });
    const reservation = { ...emprunte.emprunts![0], reservation: true, dateDebut: '9999-01-01', dateFinPrevue: '9999-01-02' };
    service.getAll.and.returnValue(of([{ ...disponible, emprunts: [reservation] }]));
    afficher();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('.calendrier-panneau summary').click();
    service.getCalendrier.calls.reset();

    fixture.nativeElement.querySelector('[aria-label="Journal d’emprunt de Vidéoprojecteur"]').click();
    expect(ouvrir).toHaveBeenCalledWith(HistoriqueMaterielModalComponent, jasmine.any(Object));
    await fixture.whenStable(); fixture.detectChanges();
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Réservation annulée.', type: 'success' }));
    expect(fixture.nativeElement.querySelector('.materiel-card').textContent).not.toContain('1 à venir');
    expect(service.getCalendrier).toHaveBeenCalledTimes(1);
  });

  it('permet à la Logistique seule de gérer le stock et de consulter le statut HS', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const reference = { componentInstance: {}, result: Promise.resolve({ ...disponible, id: 8, nom: 'Enceinte' }) };
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    open.and.returnValue(reference);
    const component = afficher();
    expect(fixture.nativeElement.textContent).toContain('Ajouter du matériel');
    expect(fixture.nativeElement.querySelector('[aria-label="Modifier Vidéoprojecteur"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Emprunter Vidéoprojecteur"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('#materielHS2').disabled).toBeTrue();
    component.emprunter(disponible);
    component.changerHorsService(disponible);
    expect(open).not.toHaveBeenCalled();
    expect(service.emprunter).not.toHaveBeenCalled();
    component.parametrer();
    expect(open).toHaveBeenCalledWith(MaterielModalComponent, jasmine.any(Object));
    await reference.result;
    expect(component.materiels.map(item => item.id)).toEqual([8, 2]);
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Matériel ajouté.', type: 'success' }));
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
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Matériel modifié.', type: 'success' }));
    const suppression = { componentInstance: {}, result: Promise.resolve({ supprimeId: 2 }) };
    open.and.returnValue(suppression);
    component.modifier(modifie);
    await suppression.result;
    expect(component.materiels).toEqual([]);
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'Matériel supprimé.', type: 'success' }));
  });

  it('refuse l’ouverture de la modification au CA et conserve le matériel après annulation', async () => {
    const component = afficher();
    component.modifier(disponible);
    const open = TestBed.inject(NgbModal).open as jasmine.Spy;
    expect(open).not.toHaveBeenCalled();
    fixture.destroy();
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const annulation = { componentInstance: {}, result: Promise.reject('annulation') };
    open.and.returnValue(annulation);
    const logistique = afficher();
    logistique.modifier(disponible);
    await annulation.result.catch(() => {});
    expect(logistique.materiels).toEqual([disponible]);
    expect(logistique.actionEnCours).toBeNull();
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
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_LOGISTIQUE'] });
    const reference = { componentInstance: {}, result: Promise.reject('Échap') };
    open.and.returnValue(reference);
    const logistique = afficher();
    logistique.zonageId = zone.id;
    service.getZonages.and.returnValue(of([]));
    service.getAll.and.returnValue(of([]));
    logistique.gererZonages();
    await reference.result.catch(() => {});
    expect(logistique.zonages).toEqual([]);
    expect(logistique.zonageId).toBeNull();
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

  it('permet à un membre du CA de signaler un problème sur du matériel emprunté par un autre membre', async () => {
    storage.getUser.and.returnValue({ id: 9, roles: ['ROLE_MEMBRECA'] });
    const stock = { ...emprunte, quantiteTotale: 2, quantiteDisponible: 1 };
    service.getAll.and.returnValue(of([stock]));
    let terminer!: (materiel: Materiel) => void;
    const reference = { componentInstance: { materiel: disponible }, result: new Promise<Materiel>(resolve => terminer = resolve) };
    const modal = TestBed.inject(NgbModal);
    (modal.open as jasmine.Spy).and.returnValue(reference);
    const component = afficher();
    const boutonHs: HTMLButtonElement = fixture.nativeElement.querySelector('#materielHS2');
    expect(boutonHs.disabled).toBeFalse();
    boutonHs.click();
    expect(modal.open).toHaveBeenCalledWith(HorsServiceMaterielModalComponent, jasmine.any(Object));
    expect(reference.componentInstance.materiel).toEqual(stock);
    expect(component.materiels[0].horsService).toBeFalse();
    expect(component.actionEnCours).toBe(2);
    terminer({ ...stock, quantiteDisponible: 0, quantiteHorsService: 1, horsService: true, probleme: 'Lampe cassée' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.actionEnCours).toBeNull();
    expect(component.materiels[0].emprunteurNom).toBe('Camille Martin');
    expect(TestBed.inject(ToastService).toasts).toContain(jasmine.objectContaining({ message: 'État du matériel enregistré.', type: 'success' }));
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
