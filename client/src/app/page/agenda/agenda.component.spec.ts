import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { NgbConfig, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TribuService } from '../../_services/tribu.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { SeanceCalendrier } from '../../models/seance';
import { AgendaComponent } from './agenda.component';
import { AgendaPreferences, AgendaPreferencesService } from '../../_services/agenda-preferences.service';
import { PresencePrevueService } from '../../_services/presence-prevue.service';
import { EvenementDetailComponent } from '../../template/evenement-detail/evenement-detail.component';
import { CalendrierComponent } from '../../template/calendrier/calendrier.component';

describe('AgendaComponent', () => {
  let fixture: ComponentFixture<AgendaComponent>;
  let component: AgendaComponent;
  let activites: jasmine.SpyObj<ActiviteService>;
  let modales: jasmine.SpyObj<NgbModal>;
  let preferences: jasmine.SpyObj<AgendaPreferencesService>;
  const seance = (id: number, debut = '2026-10-05T10:00:00', fin = '2026-10-05T11:00:00'): SeanceCalendrier => ({
    id, activiteId: id, activiteNom: `Activité ${id}`, horaireActivite: '', salle: 'Salle',
    adresseSalle: null, couleurSalle: null, commentaire: null, lien: null,
    debut, fin, etatSeance: 'PROGRAMMEE', sectionId: 10, sectionNom: 'Sports'
  });

  beforeEach(() => {
    registerLocaleData(localeFr);
    activites = jasmine.createSpyObj('ActiviteService', ['getCalendrier', 'getCalendrierAdherent', 'getCalendrierGoogle']);
    modales = jasmine.createSpyObj('NgbModal', ['open']);
    modales.open.and.returnValue({ componentInstance: {}, result: Promise.resolve() } as NgbModalRef);
    preferences = jasmine.createSpyObj('AgendaPreferencesService', ['get', 'update']);
    preferences.get.and.returnValue(of({ sectionsMasquees: [], agendasGoogleMasques: [] }));
    preferences.update.and.callFake(selection => of(selection));
    activites.getCalendrier.and.returnValue(of([seance(1), seance(2)]));
    activites.getCalendrierAdherent.and.returnValue(of([seance(1)]));
    activites.getCalendrierGoogle.and.returnValue(of({ evenements: [{
      id: 'fete', titre: 'Fête', debut: '2026-10-10', fin: '2026-10-12', journeeEntiere: true,
      lieu: null, commentaire: null, agenda: 'ALOD', agendaSource: 'alod'
    }], erreurs: [] }));
    TestBed.configureTestingModule({
      imports: [AgendaComponent, CalendrierComponent],
      providers: [
        { provide: ActiviteService, useValue: activites },
        { provide: NgbModal, useValue: modales },
        { provide: PresencePrevueService, useValue: { getPresences: () => of([]) } },
        { provide: AgendaPreferencesService, useValue: preferences },
        { provide: ParamService, useValue: {
          getAgendasGoogle: () => of([
            { id: 8, source: 'alod', nom: 'ALOD', couleur: '#123456', isVisisbleApp: true },
            { id: 9, source: 'prive', nom: 'Masqué dans l’application', isVisisbleApp: false }
          ]),
          getSections: () => of([
            { id: 10, nom: 'Sports', type: 'COMPETITION' },
            { id: 20, nom: 'Yoga', type: 'NON_COMPETITIVE' }
          ])
        } },
        { provide: TribuService, useValue: { getConnected: () => of({ adherents: [{ id: 42, user: { id: 7 } }] }) } },
        { provide: TokenStorageService, useValue: { getUser: () => ({ id: 7 }) } }
      ]
    });
    TestBed.inject(NgbConfig).animation = false;
    fixture = TestBed.createComponent(AgendaComponent);
    component = fixture.componentInstance;
    component.mois = new Date(2026, 9, 1);
  });

  it('affiche toutes les sources sans doubler les séances personnelles', () => {
    component.ngOnInit();
    expect(activites.getCalendrier).toHaveBeenCalledWith('2026-10-01', '2026-10-31');
    expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-01', '2026-10-31', 42);
    expect(component.nombreEvenements).toBe(3);
    expect(component.jours.find(j => j.iso === '2026-10-05')!.evenements.map(e => e.categorie)).toEqual(['adherent', 'autre']);
    expect(component.jours.find(j => j.iso === '2026-10-10')!.evenements[0].categorie).toBe('asso');
    expect(component.jours.find(j => j.iso === '2026-10-11')!.evenements.length).toBe(1);
    expect(component.jours.find(j => j.iso === '2026-10-12')!.evenements.length).toBe(0);
    expect(activites.getCalendrierGoogle).toHaveBeenCalledWith('2026-10-01', '2026-10-31', ['alod']);
  });

  it('restaure la sélection du profil et masque les séances personnelles et générales de la section', () => {
    preferences.get.and.returnValue(of({ sectionsMasquees: [10], agendasGoogleMasques: [8] }));
    activites.getCalendrier.and.returnValue(of([seance(1), seance(2), { ...seance(3), sectionId: 20 }]));
    fixture.detectChanges();

    expect(component.nombreEvenements).toBe(1);
    expect(component.jours.find(j => j.iso === '2026-10-05')!.evenements[0].titre).toBe('Activité 3');
    const choix = fixture.nativeElement.querySelectorAll('.agenda-filtre input') as NodeListOf<HTMLInputElement>;
    expect(Array.from(choix).map(input => input.checked)).toEqual([false, true]);
    expect(preferences.update).not.toHaveBeenCalled();
  });

  it('utilise la couleur de la section pour toutes ses séances et celle de l’agenda Google', () => {
    activites.getCalendrier.and.returnValue(of([
      { ...seance(1), couleurSalle: '#ef93ab', couleurSection: '#AB47BC' },
      { ...seance(2), couleurSalle: '#123456', couleurSection: '#AB47BC' }
    ]));
    activites.getCalendrierAdherent.and.returnValue(of([{ ...seance(1), couleurSalle: '#ef93ab', couleurSection: '#AB47BC' }]));
    fixture.detectChanges();
    const boutons = fixture.nativeElement.querySelectorAll('.agenda-evenement') as NodeListOf<HTMLButtonElement>;
    expect(Array.from(boutons).map(bouton => bouton.style.getPropertyValue('--couleur')))
      .toEqual(['#AB47BC', '#AB47BC', '#123456', '#123456']);
  });

  it('utilise la couleur de configuration et conserve un repli pour les séances sans section', () => {
    const params = TestBed.inject(ParamService);
    spyOn(params, 'getSections').and.returnValue(of([
      { id: 10, nom: 'Sports', type: 'COMPETITION', couleur: '#AB47BC' }
    ]));
    activites.getCalendrier.and.returnValue(of([
      { ...seance(1), couleurSalle: '#ef93ab' },
      { ...seance(2), sectionId: null, couleurSalle: '#ef93ab' },
      { ...seance(3), sectionId: null }
    ]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    fixture.detectChanges();
    const evenements = component.jours.find(j => j.iso === '2026-10-05')!.evenements;
    expect(evenements.map(e => e.couleur)).toEqual(['#AB47BC', '#5CBBAF', '#5CBBAF']);
    expect((fixture.nativeElement.querySelector('.agenda-filtre:has(input) .agenda-couleur') as HTMLElement).style.backgroundColor)
      .toBe('rgb(171, 71, 188)');
  });

  it('conserve la couleur des événements et du filtre si la configuration des sections échoue', () => {
    spyOn(TestBed.inject(ParamService), 'getSections').and.returnValue(throwError(() => new Error('network')));
    activites.getCalendrier.and.returnValue(of([{ ...seance(1), couleurSection: '#AB47BC' }]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component.sections[0].couleur).toBe('#AB47BC');
    expect(component.jours.find(j => j.iso === '2026-10-05')!.evenements[0].couleur).toBe('#AB47BC');
  });

  it('affiche les pictos des séances modifiées et annulées et réserve le picto vert aux activités personnelles', () => {
    activites.getCalendrier.and.returnValue(of([
      { ...seance(1), etatSeance: 'MODIFIEE' },
      { ...seance(2), etatSeance: 'ANNULEE' },
      seance(3)
    ]));
    activites.getCalendrierAdherent.and.returnValue(of([{ ...seance(1), etatSeance: 'MODIFIEE' }]));
    fixture.detectChanges();
    const boutons = fixture.nativeElement.querySelectorAll('.agenda-evenement') as NodeListOf<HTMLButtonElement>;
    expect(boutons[0].querySelector('.agenda-evenement-etat')?.getAttribute('title')).toBe('Modifiée');
    expect(boutons[0].querySelector('.agenda-evenement-personnel')?.getAttribute('title')).toBe('Mes activités');
    expect(boutons[0].getAttribute('aria-label')).toContain('Mes activités');
    expect(boutons[0].getAttribute('aria-label')).toContain('Modifiée');
    expect(boutons[1].querySelector('.agenda-picto-annule')?.getAttribute('title')).toBe('Annulée');
    expect(boutons[1].classList.contains('annule')).toBeTrue();
    expect(boutons[2].querySelector('.agenda-evenement-etat')).toBeNull();
    expect(Array.from(boutons).slice(1).every(bouton => !bouton.querySelector('.agenda-evenement-personnel'))).toBeTrue();
  });

  it('enregistre chaque choix, restaure les événements et conserve la sélection au changement de mois', () => {
    fixture.detectChanges();
    const choix = fixture.nativeElement.querySelectorAll('.agenda-filtre input') as NodeListOf<HTMLInputElement>;
    choix[0].click();
    fixture.detectChanges();
    expect(component.nombreEvenements).toBe(1);
    expect(preferences.update).toHaveBeenCalledWith({ sectionsMasquees: [10], agendasGoogleMasques: [] });
    component.selectionnerAgenda(8, false);
    fixture.detectChanges();
    expect(component.nombreEvenements).toBe(0);
    expect(preferences.update).toHaveBeenCalledWith({ sectionsMasquees: [10], agendasGoogleMasques: [8] });
    component.changerMois(1);
    expect(component.sectionVisible(10)).toBeFalse();
    expect(component.agendaVisible(8)).toBeFalse();
    expect(component.sections.length).toBe(2);
    component.changerMois(-1);
    component.selectionnerSection(10, true);
    component.selectionnerAgenda(8, true);
    expect(component.nombreEvenements).toBe(3);
    expect(preferences.update).toHaveBeenCalledWith({ sectionsMasquees: [], agendasGoogleMasques: [] });
    expect(fixture.nativeElement.textContent).toContain('Sélection enregistrée dans votre profil.');
  });

  it('empêche les sauvegardes concurrentes et permet de réessayer après un échec', () => {
    const sauvegarde = new Subject<AgendaPreferences>();
    preferences.update.and.returnValue(sauvegarde);
    fixture.detectChanges();
    component.selectionnerAgenda(8, false);
    fixture.detectChanges();
    expect(component.nombreEvenements).toBe(2);
    expect((fixture.nativeElement.querySelector('fieldset') as HTMLFieldSetElement).disabled).toBeTrue();
    component.selectionnerSection(10, false);
    expect(preferences.update).toHaveBeenCalledTimes(1);
    sauvegarde.error(new Error('network'));
    fixture.detectChanges();
    expect(component.erreurPreferences).toContain('n’a pas pu être enregistrée');
    expect(component.messagePreferences).toBe('');
    preferences.update.and.callFake(selection => of(selection));
    fixture.nativeElement.querySelector('.agenda-entete .alert button').click();
    fixture.detectChanges();
    expect(preferences.update).toHaveBeenCalledWith({ sectionsMasquees: [], agendasGoogleMasques: [8] });
    expect(component.erreurPreferences).toBe('');
  });

  it('ne remplace pas les préférences si leur lecture échoue et permet de les recharger', () => {
    preferences.get.and.returnValue(throwError(() => new Error('network')));
    fixture.detectChanges();
    component.selectionnerSection(10, false);
    expect(preferences.update).not.toHaveBeenCalled();
    expect(component.nombreEvenements).toBe(3);
    preferences.get.and.returnValue(of({ sectionsMasquees: [10], agendasGoogleMasques: [] }));
    fixture.nativeElement.querySelector('.agenda-entete .alert button').click();
    fixture.detectChanges();
    expect(component.nombreEvenements).toBe(1);
    expect(component.preferencesChargees).toBeTrue();
    expect(component.erreurPreferences).toBe('');
  });

  it('affiche les séances sans section sans proposer de case Sans section', () => {
    activites.getCalendrier.and.returnValue(of([{ ...seance(1), sectionId: null }]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.agenda-filtres-groupes').textContent).not.toContain('Sans section');
    component.selectionnerSection(10, false);
    expect(component.nombreEvenements).toBe(2);
    expect(preferences.update).toHaveBeenCalledWith({ sectionsMasquees: [10], agendasGoogleMasques: [] });
  });

  it('construit des semaines complètes du lundi au dimanche y compris un mois de six semaines', () => {
    component.mois = new Date(2026, 2, 1);
    component.ngOnInit();
    expect(component.jours.length).toBe(42);
    expect(component.jours[0].iso).toBe('2026-02-23');
    expect(component.jours[0].dansLeMois).toBeFalse();
    expect(component.jours[41].iso).toBe('2026-04-05');
    component.changerMois(-3);
    expect(activites.getCalendrier).toHaveBeenCalledWith('2025-12-01', '2025-12-31');
  });

  it('ne prolonge pas au lendemain une séance finissant à minuit', () => {
    activites.getCalendrier.and.returnValue(of([seance(3, '2026-10-05T23:00:00', '2026-10-06T00:00:00')]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    component.ngOnInit();
    expect(component.jours.find(j => j.iso === '2026-10-05')!.evenements.length).toBe(1);
    expect(component.jours.find(j => j.iso === '2026-10-06')!.evenements.length).toBe(0);
  });

  it('conserve les évènements disponibles quand une source échoue', () => {
    activites.getCalendrier.and.returnValue(throwError(() => new Error('indisponible')));
    component.ngOnInit();
    expect(component.chargement).toBeFalse();
    expect(component.erreurs.length).toBe(1);
    expect(component.nombreEvenements).toBe(2);
    expect(component.jours.find(j => j.iso === '2026-10-05')!.evenements[0].categorie).toBe('adherent');
  });

  it('ignore une réponse tardive du mois précédent', () => {
    const ancienne = new Subject<SeanceCalendrier[]>();
    activites.getCalendrier.and.returnValue(ancienne);
    component.ngOnInit();
    activites.getCalendrier.and.returnValue(of([]));
    component.changerMois(1);
    ancienne.next([seance(99)]);
    ancienne.complete();
    expect(component.jours.find(j => j.dansLeMois)!.iso).toBe('2026-11-01');
  });

  it('ouvre les détails de chaque évènement dans une modale accessible', () => {
    component.ngOnInit();
    const evenement = component.jours.find(j => j.iso === '2026-10-05')!.evenements[0];
    component.ouvrirEvenement(evenement);
    expect(component.evenementSelectionne).toBe(evenement);
    expect(modales.open).toHaveBeenCalledWith(EvenementDetailComponent, jasmine.objectContaining({ ariaLabelledBy: 'evenement-detail-titre', scrollable: true, centered: true }));
    expect(modales.open.calls.mostRecent().returnValue.componentInstance.evenement).toBe(evenement);
    expect(modales.open.calls.mostRecent().returnValue.componentInstance.adherentId).toBe(42);
  });

  it('affiche la couleur de la salle dans la modale et la couleur de section dans le calendrier', async () => {
    activites.getCalendrier.and.returnValue(of([{
      ...seance(1), couleurSection: '#AB47BC', couleurSalle: '#ef93ab', adresseSalle: '1 rue des Sports'
    }]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    const modaleReelle = TestBed.runInInjectionContext(() => new NgbModal());
    modales.open.and.callFake((contenu, options) => modaleReelle.open(contenu, options));
    try {
      fixture.detectChanges();
      const bouton = fixture.nativeElement.querySelector('.agenda-evenement') as HTMLButtonElement;
      expect(bouton.style.getPropertyValue('--couleur')).toBe('#AB47BC');
      expect(bouton.querySelector('.agenda-lieu-salle')).toBeNull();
      bouton.click();
      await fixture.whenStable();
      const salle = document.querySelector('.modal-body .agenda-lieu-salle') as HTMLElement;
      expect(salle.textContent).toContain('Salle');
      expect(salle.style.getPropertyValue('--couleur-salle')).toBe('#ef93ab');
      expect((salle.querySelector('.agenda-couleur') as HTMLElement).style.backgroundColor).toBe('rgb(239, 147, 171)');
      expect(document.querySelector('.modal-body')!.textContent).toContain('1 rue des Sports');
    } finally {
      modaleReelle.dismissAll();
    }
  });

  it('ouvre exactement le même composant et les mêmes détails depuis l’accueil et l’agenda', async () => {
    const commune = {
      ...seance(1, '2026-10-07T18:00:00', '2026-10-07T19:30:00'),
      activiteNom: 'Basket – Entraînement', activiteNomCourt: 'Basket', descriptif: 'Entraînement',
      sectionType: 'COMPETITION', couleurSalle: '#AB47BC', adresseSalle: '1 rue des Sports',
      commentaire: '<p>Prévoir une gourde</p>', lien: 'example.org/basket',
      responsabilites: [{ tache: { nom: 'Matériel', icone: 'box-open', couleur: '#4285F4' }, adherent: { id: 42, prenom: 'Alice', nom: 'Dupont' } }]
    };
    const reponses = [{ id: 9, adherentId: 42, prenom: 'Alice', nom: 'Dupont', presencePrevue: true }];
    TestBed.inject(PresencePrevueService).getPresences = () => of(reponses);
    activites.getCalendrier.and.returnValue(of([commune]));
    activites.getCalendrierAdherent.and.returnValue(of([commune]));
    activites.getCalendrierGoogle.and.returnValue(of({ evenements: [], erreurs: [] }));
    const modaleReelle = TestBed.runInInjectionContext(() => new NgbModal());
    modales.open.and.callFake((contenu, options) => modaleReelle.open(contenu, options));
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 9, 6, 12));
      fixture.detectChanges();
      fixture.nativeElement.querySelector('.agenda-evenement').click();
      await fixture.whenStable();
      const detailAgenda = document.querySelector('.modal-body')!.innerHTML;
      modaleReelle.dismissAll();
      const accueil = TestBed.createComponent(CalendrierComponent);
      accueil.componentInstance.adherentId = 42;
      accueil.componentInstance.affichage = 'liste';
      accueil.componentInstance.joursVisibles = 15;
      accueil.componentInstance.presencesPrevues = true;
      accueil.detectChanges();
      accueil.nativeElement.querySelector('.evenement-ouvrir').click();
      await accueil.whenStable();
      expect(document.querySelector('.modal-body')!.innerHTML).toBe(detailAgenda);
      expect(modales.open.calls.allArgs().map(args => args[0])).toEqual([EvenementDetailComponent, EvenementDetailComponent]);
      const detail = document.querySelector('.modal-body')!;
      for (const texte of ['Entraînement', 'Sports', '19:30', 'Prévoir une gourde', 'Matériel : Alice Dupont', 'Présents : 1', 'Waze']) {
        expect(detail.textContent).toContain(texte);
      }
    } finally {
      modaleReelle.dismissAll();
      jasmine.clock().uninstall();
    }
  });

  it('ouvre un événement Google sans lui attribuer de couleur de salle', async () => {
    const modaleReelle = TestBed.runInInjectionContext(() => new NgbModal());
    modales.open.and.callFake((contenu, options) => modaleReelle.open(contenu, options));
    try {
      fixture.detectChanges();
      const boutons = fixture.nativeElement.querySelectorAll('.agenda-evenement') as NodeListOf<HTMLButtonElement>;
      boutons[2].click();
      await fixture.whenStable();
      expect(document.querySelector('.modal-body .agenda-lieu-salle')).toBeNull();
      expect(document.querySelector('.modal-body')!.textContent).toContain('ALOD');
      expect(boutons[2].style.getPropertyValue('--couleur')).toBe('#123456');
    } finally {
      modaleReelle.dismissAll();
    }
  });

  it('affiche uniquement les responsabilités personnelles dans l’agenda et toutes les responsabilités dans la modale', async () => {
    activites.getCalendrier.and.returnValue(of([{
      ...seance(1), couleurSection: '#5CBBAF', responsabilites: [
        { tache: { id: 3, nom: 'Accueil', icone: 'handshake', couleur: '#AB47BC' }, adherent: { id: 42, prenom: 'Alice', nom: 'Dupont' } },
        { tache: { id: 4, nom: 'Matériel', icone: 'box-open', couleur: '#4285F4' }, adherent: { id: 13, prenom: 'Bob', nom: 'Martin' } }
      ]
    }]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    const modaleReelle = TestBed.runInInjectionContext(() => new NgbModal());
    modales.open.and.callFake((contenu, options) => modaleReelle.open(contenu, options));
    try {
      fixture.detectChanges();
      const bouton = fixture.nativeElement.querySelector('.agenda-evenement') as HTMLButtonElement;
      const icones = bouton.querySelectorAll('.responsabilite-icone') as NodeListOf<HTMLElement>;
      expect(icones.length).toBe(1);
      expect(icones[0].querySelector('svg')?.getAttribute('data-icon')).toBe('handshake');
      expect(icones[0].style.color).toBe('rgb(171, 71, 188)');
      expect(bouton.getAttribute('aria-label')).toContain('Accueil : Alice Dupont');
      expect(bouton.getAttribute('aria-label')).not.toContain('Matériel');
      expect(bouton.getAttribute('title')).not.toContain('Bob Martin');
      expect(bouton.style.getPropertyValue('--couleur')).toBe('#5CBBAF');
      bouton.click();
      await fixture.whenStable();
      expect(document.querySelector('.modal-body')!.textContent).toContain('Accueil : Alice Dupont');
      expect(document.querySelector('.modal-body')!.textContent).toContain('Matériel : Bob Martin');
      expect(document.querySelectorAll('.modal-body .responsabilite-icone').length).toBe(2);
      const detail = document.querySelector('.modal-body .responsabilite-icone') as HTMLElement;
      expect(detail.style.color).toBe('rgb(171, 71, 188)');
    } finally {
      modaleReelle.dismissAll();
    }
  });

  it('ne crée aucun picto de tâche si les responsabilités sont attribuées à d’autres adhérents', () => {
    activites.getCalendrier.and.returnValue(of([{
      ...seance(2), responsabilites: [
        { tache: { nom: 'Matériel', icone: 'box-open' }, adherent: { id: 13, prenom: 'Bob', nom: 'Martin' } }
      ]
    }]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.responsabilite-icone').length).toBe(0);
    const boutons = fixture.nativeElement.querySelectorAll('.agenda-evenement') as NodeListOf<HTMLButtonElement>;
    for (const bouton of boutons) {
      expect(bouton.getAttribute('title')).not.toContain('Matériel');
      expect(bouton.getAttribute('aria-label')).not.toContain('Bob Martin');
    }
  });

  it('n’affiche aucune responsabilité directement si l’adhérent connecté ne peut pas être identifié', () => {
    TestBed.inject(TribuService).getConnected = () => throwError(() => new Error('network'));
    activites.getCalendrier.and.returnValue(of([{
      ...seance(1), responsabilites: [
        { tache: { nom: 'Accueil', icone: 'handshake' }, adherent: { id: 42, prenom: 'Alice', nom: 'Dupont' } }
      ]
    }]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.responsabilite-icone').length).toBe(0);
    expect(fixture.nativeElement.querySelector('.agenda-evenement').getAttribute('title')).not.toContain('Accueil');
  });

  it('adapte le contenu à 1600, 900 et 375 pixels sans débordement horizontal', () => {
    activites.getCalendrierAdherent.and.returnValue(of([{ ...seance(1), etatSeance: 'MODIFIEE' }]));
    fixture.detectChanges();
    const iframe = document.createElement('iframe');
    iframe.style.height = '1200px';
    document.body.appendChild(iframe);
    try {
      const doc = iframe.contentDocument!;
      document.querySelectorAll('style').forEach(style => doc.head.appendChild(style.cloneNode(true)));
      doc.body.appendChild(fixture.nativeElement.cloneNode(true));
      const titre = doc.querySelector('.agenda-evenement-titre')!;
      const detail = doc.querySelector('.agenda-evenement-detail')!;
      const bouton = doc.querySelector('.agenda-evenement')!;
      for (const largeur of [1600, 900, 375]) {
        iframe.style.width = `${largeur}px`;
        const vue = iframe.contentWindow!;
        expect(vue.getComputedStyle(titre).display).toBe(largeur < 576 ? 'none' : 'block');
        expect(vue.getComputedStyle(detail).display).toBe(largeur >= 1400 ? 'block' : 'none');
        expect(doc.documentElement.scrollWidth).toBeLessThanOrEqual(doc.documentElement.clientWidth);
        expect(bouton.getAttribute('aria-label')).toContain('Activité 1');
        const etat = bouton.querySelector('.agenda-evenement-etat')!;
        const personnel = bouton.querySelector('.agenda-evenement-personnel')!;
        const zoneBouton = bouton.getBoundingClientRect();
        const zoneEtat = etat.getBoundingClientRect();
        const zonePersonnel = personnel.getBoundingClientRect();
        expect(vue.getComputedStyle(etat).position).toBe('absolute');
        expect(vue.getComputedStyle(personnel).color).toBe('rgb(23, 107, 74)');
        expect(zoneEtat.right).toBeLessThanOrEqual(zoneBouton.right);
        expect(zonePersonnel.bottom).toBeLessThanOrEqual(zoneBouton.bottom);
        expect(zoneEtat.bottom).toBeLessThanOrEqual(zonePersonnel.top);
      }
    } finally {
      iframe.remove();
    }
    const bouton = fixture.nativeElement.querySelector('.agenda-evenement') as HTMLButtonElement;
    bouton.click();
    expect(modales.open).toHaveBeenCalled();
  });
});
