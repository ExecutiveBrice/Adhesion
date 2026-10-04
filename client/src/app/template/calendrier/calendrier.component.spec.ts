import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { CalendrierComponent } from './calendrier.component';

describe('CalendrierComponent : périmètre adhérent', () => {
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
        { provide: ParamService, useValue: params }
      ]
    });
    component = TestBed.createComponent(CalendrierComponent).componentInstance;
  });

  for (const affichage of ['liste', 'calendrier'] as const) {
    it(`limite ${affichage} à l’adhérent sans charger les agendas généraux`, () => {
      component.adherentId = 42;
      component.affichage = affichage;
      component.googleAgendaIds = ['agenda-public'];
      component.ngOnInit();

      expect(activites.getCalendrierAdherent).toHaveBeenCalledWith(jasmine.any(String), jasmine.any(String), 42);
      expect(activites.getCalendrier).not.toHaveBeenCalled();
      expect(activites.getCalendrierGoogle).not.toHaveBeenCalled();
      expect(params.getAgendasGoogle).not.toHaveBeenCalled();
    });
  }

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
