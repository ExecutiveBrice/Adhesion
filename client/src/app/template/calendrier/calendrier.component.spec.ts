import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { of, Subject, throwError } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { PresencePrevue, SeanceCalendrier } from '../../models/seance';
import { PresencePrevueService } from '../../_services/presence-prevue.service';
import { CalendrierComponent } from './calendrier.component';
import { NgbConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';

describe('CalendrierComponent — présences prévues dans l’accueil', () => {
  it('limite la carte aux informations demandées et aux pictos personnels puis ouvre tous les détails', async () => {
    registerLocaleData(localeFr);
    const debut = new Date();
    debut.setDate(debut.getDate() + 1);
    const date = `${debut.getFullYear()}-${String(debut.getMonth() + 1).padStart(2, '0')}-${String(debut.getDate()).padStart(2, '0')}`;
    const seance: SeanceCalendrier = {
      id: 15, activiteId: 1, activiteNom: 'Basket – Équipe U15', activiteNomCourt: 'Basket', descriptif: 'Équipe U15',
      horaireActivite: '', salle: 'Gymnase', adresseSalle: '1 rue des Sports', couleurSalle: '#AB47BC',
      commentaire: 'Prévoir une gourde', lien: 'https://example.org/basket', debut: `${date}T18:00:00`, fin: `${date}T19:00:00`,
      etatSeance: 'MODIFIEE', responsabilites: [
        { tache: { id: 3, nom: 'Accueil', icone: 'handshake', couleur: '#AB47BC' }, adherent: { id: 12, prenom: 'Alice', nom: 'Dupont' } },
        { tache: { id: 4, nom: 'Matériel', icone: 'box-open', couleur: '#4285F4' }, adherent: { id: 13, prenom: 'Bob', nom: 'Martin' } }
      ]
    };
    TestBed.configureTestingModule({
      imports: [CalendrierComponent],
      providers: [
        { provide: ActiviteService, useValue: { getCalendrierAdherent: () => of([seance]) } },
        { provide: ParamService, useValue: { getAgendasGoogle: () => of([]) } },
        { provide: PresencePrevueService, useValue: {} }
      ]
    });
    const fixture = TestBed.createComponent(CalendrierComponent);
    fixture.componentInstance.adherentId = 12;
    fixture.componentInstance.affichage = 'liste';
    fixture.componentInstance.joursVisibles = 15;
    fixture.detectChanges();
    const carte = fixture.nativeElement.querySelector('.evenement-card') as HTMLElement;
    expect(carte.querySelector('time')?.getAttribute('datetime')).toBe(`${date}T18:00:00`);
    expect(carte.querySelector('.evenement-titre')?.textContent).toBe('Basket');
    expect(carte.querySelector('.evenement-descriptif')?.textContent).toBe('Équipe U15');
    expect(carte.querySelector('.evenement-heure')?.textContent).toBe('18:00');
    for (const texte of ['19:00', 'Gymnase', '1 rue des Sports', 'Prévoir une gourde', 'Modifiée', 'Alice', 'Bob']) {
      expect(carte.textContent).not.toContain(texte);
    }
    expect(carte.querySelector('a')).toBeNull();
    expect(carte.querySelectorAll('.responsabilite-icone').length).toBe(1);
    const icone = carte.querySelector('.responsabilite-icone') as HTMLElement;
    expect(icone.style.color).toBe('rgb(171, 71, 188)');
    expect(icone.querySelector('svg')?.getAttribute('data-icon')).toBe('handshake');
    expect(carte.querySelector('.responsabilite-seance')?.getAttribute('title')).toBe('Accueil : Alice Dupont');
    const modales = TestBed.inject(NgbModal);
    TestBed.inject(NgbConfig).animation = false;
    try {
      carte.querySelector<HTMLButtonElement>('.evenement-ouvrir')!.click();
      await fixture.whenStable();
      const modale = document.querySelector('.modal-body') as HTMLElement;
      for (const texte of ['19:00', 'Gymnase', '1 rue des Sports', 'Prévoir une gourde', 'Modifiée', 'Accueil : Alice Dupont', 'Matériel : Bob Martin']) {
        expect(modale.textContent).toContain(texte);
      }
      expect(modale.querySelectorAll('.responsabilite-icone').length).toBe(2);
      expect(modale.querySelector('a[href="https://example.org/basket"]')).not.toBeNull();
      expect(document.querySelector('.modal')?.getAttribute('aria-labelledby')).toBe('evenement-detail-titre');
    } finally {
      modales.dismissAll();
    }
  });

  it('propose les boutons directs uniquement pour les séances compétitives à venir', () => {
    registerLocaleData(localeFr);
    const debut = new Date();
    debut.setDate(debut.getDate() + 1);
    debut.setHours(18, 0, 0, 0);
    const date = `${debut.getFullYear()}-${String(debut.getMonth() + 1).padStart(2, '0')}-${String(debut.getDate()).padStart(2, '0')}`;
    const base: SeanceCalendrier = {
      id: 15, activiteId: 1, activiteNom: 'Basket', sectionType: 'COMPETITION', horaireActivite: '',
      salle: '', adresseSalle: null, couleurSalle: null, commentaire: null, lien: null,
      debut: `${date}T18:00:00`, fin: `${date}T19:00:00`, etatSeance: 'PROGRAMMEE'
    };
    TestBed.configureTestingModule({
      imports: [CalendrierComponent],
      providers: [
        { provide: ActiviteService, useValue: { getCalendrierAdherent: () => of([
          base, { ...base, id: 16, sectionType: 'NON_COMPETITIVE' }, { ...base, id: 17, etatSeance: 'ANNULEE' }
        ]) } },
        { provide: ParamService, useValue: { getAgendasGoogle: () => of([]) } },
        { provide: PresencePrevueService, useValue: { getPresences: () => of([]) } }
      ]
    });
    const fixture = TestBed.createComponent(CalendrierComponent);
    const component = fixture.componentInstance;
    component.adherentId = 9;
    component.affichage = 'liste';
    component.joursVisibles = 15;
    component.presencesPrevues = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.seance-presences .btn-group button').length).toBe(2);
    expect(fixture.nativeElement.querySelector('h3 button')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Voir les présences prévues');
    const event = component.evenementsAVenir[0];
    component.presencesPrevues = false;
    expect(component.peutRenseignerPresence(event)).toBeFalse();
    component.presencesPrevues = true;
    expect(component.peutRenseignerPresence({ ...event, debut: '2000-01-01T18:00:00' })).toBeFalse();
    component.adherentId = undefined;
    expect(component.peutRenseignerPresence(event)).toBeFalse();
  });
});

describe('CalendrierComponent : réponses et totaux sur les séances', () => {
  let fixture: ComponentFixture<CalendrierComponent>;
  let component: CalendrierComponent;
  let service: jasmine.SpyObj<PresencePrevueService>;
  let modales: NgbModal;
  const presences = (): PresencePrevue[] => [
    { id: 1, adherentId: 9, nom: 'Dupont', prenom: 'Alice', presencePrevue: null },
    { id: 2, adherentId: 10, nom: 'Martin', prenom: 'Paul', presencePrevue: true },
    { id: 3, adherentId: 11, nom: 'Bernard', prenom: 'Jean', presencePrevue: false },
    { id: 4, adherentId: 12, nom: 'Durand', prenom: 'Marie', presencePrevue: null }
  ];

  beforeEach(() => {
    registerLocaleData(localeFr);
    const date = new Date();
    date.setDate(date.getDate() + 1);
    const iso = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    const seance: SeanceCalendrier = {
      id: 15, activiteId: 1, activiteNom: 'Basket', sectionType: 'COMPETITION', horaireActivite: '',
      salle: '', adresseSalle: null, couleurSalle: null, commentaire: null, lien: null,
      debut: `${iso}T18:00:00`, fin: `${iso}T19:00:00`, etatSeance: 'PROGRAMMEE'
    };
    service = jasmine.createSpyObj('PresencePrevueService', ['getPresences', 'updatePresence']);
    service.getPresences.and.returnValue(of(presences()));
    TestBed.configureTestingModule({
      imports: [CalendrierComponent],
      providers: [
        { provide: ActiviteService, useValue: { getCalendrierAdherent: () => of([
          seance, { ...seance, id: 16, sectionType: 'NON_COMPETITIVE' }, { ...seance, id: 17, etatSeance: 'ANNULEE' }
        ]) } },
        { provide: ParamService, useValue: { getAgendasGoogle: () => of([]) } },
        { provide: PresencePrevueService, useValue: service }
      ]
    });
    fixture = TestBed.createComponent(CalendrierComponent);
    component = fixture.componentInstance;
    component.adherentId = 9;
    component.affichage = 'liste';
    component.joursVisibles = 15;
    component.presencesPrevues = true;
    modales = TestBed.inject(NgbModal);
    TestBed.inject(NgbConfig).animation = false;
  });

  afterEach(() => modales.dismissAll());

  async function ouvrirDetails(): Promise<void> {
    fixture.nativeElement.querySelector('.evenement-ouvrir').click();
    await fixture.whenStable();
  }

  function totaux(): HTMLElement {
    return document.querySelector('.modal-body .seance-presences-totaux') as HTMLElement;
  }

  function boutons(): NodeListOf<HTMLButtonElement> {
    return fixture.nativeElement.querySelectorAll('.seance-presences .btn-group button');
  }

  it('affiche les deux boutons sur la carte et réserve les totaux à la modale', async () => {
    fixture.detectChanges();
    expect(service.getPresences).toHaveBeenCalledOnceWith(15);
    expect(boutons().length).toBe(2);
    expect(boutons()[0].textContent).toBe('Présent');
    expect(boutons()[1].textContent).toBe('Absent');
    expect(boutons()[0].getAttribute('aria-pressed')).toBe('false');
    expect(boutons()[1].getAttribute('aria-pressed')).toBe('false');
    expect(fixture.nativeElement.querySelector('.seance-presences-totaux')).toBeNull();
    await ouvrirDetails();
    expect(totaux().textContent).toContain('Présents : 1');
    expect(totaux().textContent).toContain('Absents : 1');
    component.adherentId = 10;
    fixture.detectChanges();
    expect(component.maPresence(component.evenementsAVenir[0])!.presencePrevue).toBeTrue();
    expect(totaux().textContent).toContain('Présents : 1');
    expect(totaux().textContent).toContain('Absents : 1');
  });

  it('enregistre depuis la carte et actualise les totaux pour chaque changement de réponse', () => {
    const response = new Subject<PresencePrevue>();
    service.updatePresence.and.returnValue(response);
    fixture.detectChanges();
    const event = component.evenementsAVenir[0];
    boutons()[0].click();
    fixture.detectChanges();
    expect(boutons()[0].disabled).toBeTrue();
    expect(boutons()[1].disabled).toBeTrue();
    component.definirPresencePrevue(event, false);
    expect(service.updatePresence).toHaveBeenCalledOnceWith(15, true);
    expect(component.evenementSelectionne).toBeNull();
    expect(component.nombrePresences(event, true)).toBe(1);
    service.getPresences.and.returnValue(of(presences().map(presence => presence.id === 1 ? { ...presence, presencePrevue: true } : presence)));
    response.next({ ...presences()[0], presencePrevue: true });
    response.complete();
    fixture.detectChanges();
    expect(component.nombrePresences(event, true)).toBe(2);
    expect(boutons()[0].getAttribute('aria-pressed')).toBe('true');
    service.updatePresence.and.returnValue(of({ ...presences()[0], presencePrevue: false }));
    service.getPresences.and.returnValue(of(presences().map(presence => presence.id === 1 ? { ...presence, presencePrevue: false } : presence)));
    boutons()[1].click();
    fixture.detectChanges();
    expect(service.updatePresence).toHaveBeenCalledWith(15, false);
    expect(component.nombrePresences(event, true)).toBe(1);
    expect(component.nombrePresences(event, false)).toBe(2);
    expect(boutons()[1].getAttribute('aria-pressed')).toBe('true');
    boutons()[1].click();
    expect(service.updatePresence).toHaveBeenCalledTimes(2);
  });

  it('conserve les totaux si l’enregistrement échoue et permet de réessayer', async () => {
    service.updatePresence.and.returnValue(throwError(() => ({ error: { detail: 'Séance annulée' } })));
    fixture.detectChanges();
    boutons()[1].click();
    fixture.detectChanges();
    const event = component.evenementsAVenir[0];
    expect(component.maPresence(event)!.presencePrevue).toBeNull();
    expect(component.nombrePresences(event, false)).toBe(1);
    await ouvrirDetails();
    expect(document.querySelector('.modal-body')!.textContent).toContain('Séance annulée');
    expect(boutons()[1].disabled).toBeFalse();
    service.updatePresence.and.returnValue(of({ ...presences()[0], presencePrevue: false }));
    service.getPresences.and.returnValue(of(presences().map(presence => presence.id === 1 ? { ...presence, presencePrevue: false } : presence)));
    boutons()[1].click();
    expect(component.nombrePresences(event, false)).toBe(2);
    expect(event.erreurPresence).toBe('');
  });

  it('désactive les boutons pendant le chargement et permet de recharger après une erreur depuis la modale', async () => {
    const response = new Subject<PresencePrevue[]>();
    service.getPresences.and.returnValue(response);
    fixture.detectChanges();
    expect(boutons()[0].disabled).toBeTrue();
    await ouvrirDetails();
    expect(totaux().textContent).toContain('Chargement');
    response.error(new Error('network'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(totaux().textContent).not.toContain('Absents : 0');
    expect(boutons()[0].disabled).toBeTrue();
    service.getPresences.and.returnValue(of(presences()));
    document.querySelector<HTMLButtonElement>('.modal-body .alert button')!.click();
    fixture.detectChanges();
    expect(boutons()[0].disabled).toBeFalse();
    expect(component.nombrePresences(component.evenementsAVenir[0], true)).toBe(1);
  });

  it('recharge toutes les réponses de la séance après un choix pour inclure les changements des autres inscrits', () => {
    fixture.detectChanges();
    const event = component.evenementsAVenir[0];
    service.updatePresence.and.returnValue(of({ ...presences()[0], presencePrevue: true }));
    service.getPresences.and.returnValue(of(presences().map(presence =>
      presence.id === 1 || presence.id === 4 ? { ...presence, presencePrevue: true } : presence)));
    boutons()[0].click();
    fixture.detectChanges();
    expect(service.getPresences).toHaveBeenCalledTimes(2);
    expect(service.getPresences.calls.mostRecent().args).toEqual([15]);
    expect(component.nombrePresences(event, true)).toBe(3);
    expect(component.nombrePresences(event, false)).toBe(1);
    expect(boutons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(fixture.nativeElement.querySelector('.seance-presences-totaux')).toBeNull();
  });
});

describe('CalendrierComponent : périmètre adhérent', () => {
  let fixture: ComponentFixture<CalendrierComponent>;
  let component: CalendrierComponent;
  let activites: jasmine.SpyObj<ActiviteService>;
  let params: jasmine.SpyObj<ParamService>;

  beforeEach(() => {
    activites = jasmine.createSpyObj('ActiviteService', ['getCalendrier', 'getCalendrierAdherent', 'getCalendrierGoogle']);
    params = jasmine.createSpyObj('ParamService', ['getAgendasGoogle']);
    activites.getCalendrier.and.returnValue(of([]));
    activites.getCalendrierAdherent.and.returnValue(of([]));
    activites.getCalendrierGoogle.and.returnValue(of({ evenements: [], erreurs: [] }));
    params.getAgendasGoogle.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [CalendrierComponent],
      providers: [
        { provide: ActiviteService, useValue: activites },
        { provide: ParamService, useValue: params },
        { provide: PresencePrevueService, useValue: {} }
      ]
    });
    fixture = TestBed.createComponent(CalendrierComponent);
    component = fixture.componentInstance;
  });

  for (const affichage of ['liste', 'calendrier'] as const) {
    it(`charge les séances de l’adhérent et uniquement les agendas Google visibles en ${affichage}`, () => {
      component.adherentId = 42;
      component.affichage = affichage;
      params.getAgendasGoogle.and.returnValue(of([
        { nom: 'Association', source: 'agenda-public', couleur: '#123456', isVisisbleApp: true },
        { nom: 'Masqué', source: 'agenda-masque', couleur: '#654321', isVisisbleApp: false }
      ]));
      component.ngOnInit();

      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith(jasmine.any(String), jasmine.any(String), 42);
      expect(activites.getCalendrier).not.toHaveBeenCalled();
      expect(activites.getCalendrierGoogle).toHaveBeenCalledOnceWith(jasmine.any(String), jasmine.any(String), ['agenda-public']);
      expect(params.getAgendasGoogle).toHaveBeenCalledTimes(1);
    });
  }

  it('affiche les événements Google avec les séances dans l’accueil, sur la même période de 15 jours', () => {
    registerLocaleData(localeFr);
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 9, 6, 12));
      params.getAgendasGoogle.and.returnValue(of([
        { nom: 'Vie associative', source: 'agenda-public', couleur: '#123456', isVisisbleApp: true }
      ]));
      activites.getCalendrierAdherent.and.returnValue(of([{
        id: 15, activiteId: 1, activiteNom: 'Basket', sectionType: 'COMPETITION', horaireActivite: '',
        salle: '', adresseSalle: null, couleurSalle: null, commentaire: null, lien: null,
        debut: '2026-10-08T18:00:00', fin: '2026-10-08T19:00:00', etatSeance: 'PROGRAMMEE'
      }]));
      activites.getCalendrierGoogle.and.returnValue(of({ evenements: [{
        id: 'fete', titre: 'Fête de l’association', lieu: 'Maison des associations', commentaire: null,
        debut: '2026-10-07T00:00:00', fin: '2026-10-08T00:00:00', journeeEntiere: true,
        agenda: 'Nom Google', agendaSource: 'agenda-public'
      }], erreurs: [] }));
      component.adherentId = 42;
      component.affichage = 'liste';
      component.joursVisibles = 15;
      component.presencesPrevues = true;
      const presences = jasmine.createSpy('getPresences').and.returnValue(of([]));
      TestBed.inject(PresencePrevueService).getPresences = presences;
      fixture.detectChanges();

      expect(activites.getCalendrierAdherent).toHaveBeenCalledOnceWith('2026-10-06', '2026-10-20', 42);
      expect(activites.getCalendrierGoogle).toHaveBeenCalledOnceWith('2026-10-06', '2026-10-20', ['agenda-public']);
      const cartes = fixture.nativeElement.querySelectorAll('.evenement-card') as NodeListOf<HTMLElement>;
      expect(cartes.length).toBe(2);
      expect(cartes[0].textContent).toContain('Fête de l’association');
      expect(cartes[0].textContent).not.toContain('Google · Vie associative');
      expect(cartes[0].textContent).not.toContain('Maison des associations');
      expect(cartes[0].textContent).toContain('Toute la journée');
      expect(cartes[0].style.getPropertyValue('--agenda-color')).toBe('#123456');
      expect(cartes[0].querySelector('.seance-presences')).toBeNull();
      expect(cartes[1].textContent).toContain('Basket');
      expect(presences).toHaveBeenCalledOnceWith(15);
    } finally {
      jasmine.clock().uninstall();
    }
  });

  it('colore les séances par section et réserve la couleur de salle au détail du jour', () => {
    registerLocaleData(localeFr);
    component.dateAffichee = new Date(2026, 9, 6);
    activites.getCalendrier.and.returnValue(of([
      {
        id: 15, activiteId: 1, activiteNom: 'Basket', horaireActivite: '',
        salle: 'Gymnase', adresseSalle: '1 rue des Sports', couleurSalle: '#ef93ab', couleurSection: '#AB47BC',
        commentaire: null, lien: null, debut: '2026-10-06T18:00:00', fin: '2026-10-06T19:00:00', etatSeance: 'PROGRAMMEE'
      },
      {
        id: 16, activiteId: 2, activiteNom: 'Sans section', horaireActivite: '',
        salle: 'Gymnase', adresseSalle: null, couleurSalle: '#ef93ab', commentaire: null, lien: null,
        debut: '2026-10-06T19:00:00', fin: '2026-10-06T20:00:00', etatSeance: 'PROGRAMMEE'
      }
    ]));
    fixture.detectChanges();
    const resumes = fixture.nativeElement.querySelectorAll('.seance-resume') as NodeListOf<HTMLElement>;
    expect(Array.from(resumes).map(element => element.style.getPropertyValue('--agenda-color'))).toEqual(['#AB47BC', '#5CBBAF']);
    expect(fixture.nativeElement.querySelector('.detail-salle')).toBeNull();
    (fixture.nativeElement.querySelectorAll('.calendrier-jour')[1] as HTMLElement).click();
    fixture.detectChanges();
    const salle = fixture.nativeElement.querySelector('.popup-jour .detail-salle') as HTMLElement;
    expect(salle.textContent).toBe('Gymnase');
    expect(salle.style.getPropertyValue('--couleur-salle')).toBe('#ef93ab');
    expect(fixture.nativeElement.querySelector('.popup-jour').textContent).toContain('1 rue des Sports');
  });

  it('ne sollicite pas Google lorsqu’aucun agenda n’est visible', () => {
    component.adherentId = 42;
    params.getAgendasGoogle.and.returnValue(of([
      { nom: 'Masqué', source: 'agenda-masque', couleur: '#654321', isVisisbleApp: false }
    ]));
    component.ngOnInit();

    expect(activites.getCalendrierAdherent).toHaveBeenCalled();
    expect(activites.getCalendrierGoogle).not.toHaveBeenCalled();
  });

  it('conserve les séances et affiche une erreur si la configuration Google est indisponible', () => {
    component.adherentId = 42;
    params.getAgendasGoogle.and.returnValue(throwError(() => new Error('network')));
    component.ngOnInit();

    expect(activites.getCalendrierAdherent).toHaveBeenCalled();
    expect(activites.getCalendrierGoogle).not.toHaveBeenCalled();
    expect(component.googleAgendaErreur).toContain('La configuration des agendas Google');
    expect(component.chargementCalendrier).toBeFalse();
  });

  it('conserve le périmètre tribu pour les autres pages', () => {
    component.tribuUuid = 'tribu-test';
    component.ngOnInit();

    expect(params.getAgendasGoogle).toHaveBeenCalled();
    expect(activites.getCalendrier).toHaveBeenCalledWith(jasmine.any(String), jasmine.any(String), 'tribu-test');
    expect(activites.getCalendrierAdherent).not.toHaveBeenCalled();
  });

  it('exporte uniquement les séances de l’adhérent', () => {
    component.adherentId = 42;
    spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');
    component.telechargerSeancesGoogle();

    expect(activites.getCalendrierAdherent).toHaveBeenCalledWith(jasmine.any(String), jasmine.any(String), 42);
    expect(activites.getCalendrier).not.toHaveBeenCalled();
    expect(component.exportCalendrierEnCours).toBeFalse();
  });

  it('limite la liste à 15 jours inclus et fait avancer la période chaque jour', () => {
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 9, 3, 12));
      component.adherentId = 42;
      component.joursVisibles = 15;
      component.affichage = 'liste';
      component.chargerCalendrier();
      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-03', '2026-10-17', 42);

      jasmine.clock().mockDate(new Date(2026, 9, 4, 12));
      component.chargerCalendrier();
      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-04', '2026-10-18', 42);
    } finally {
      jasmine.clock().uninstall();
    }
  });

  it('borne aussi l’agenda et l’export à la période de visibilité', () => {
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 9, 3, 12));
      component.adherentId = 42;
      component.joursVisibles = 15;
      component.dateAffichee = new Date(2026, 9, 3);
      component.chargerCalendrier();
      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-03', '2026-10-04', 42);
      component.changerPeriode(14);
      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-12', '2026-10-17', 42);
      activites.getCalendrierAdherent.calls.reset();
      component.changerPeriode(1);
      expect(activites.getCalendrierAdherent).not.toHaveBeenCalled();

      spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
      spyOn(URL, 'revokeObjectURL');
      spyOn(HTMLAnchorElement.prototype, 'click');
      component.telechargerSeancesGoogle();
      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith('2026-10-03', '2026-10-17', 42);
    } finally {
      jasmine.clock().uninstall();
    }
  });
});
