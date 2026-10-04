import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { ParamService } from '../../_services/param.service';
import { TribuService } from '../../_services/tribu.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { SeanceCalendrier } from '../../models/seance';
import { AgendaComponent } from './agenda.component';

describe('AgendaComponent', () => {
  let fixture: ComponentFixture<AgendaComponent>;
  let component: AgendaComponent;
  let activites: jasmine.SpyObj<ActiviteService>;
  let modales: jasmine.SpyObj<NgbModal>;
  const seance = (id: number, debut = '2026-10-05T10:00:00', fin = '2026-10-05T11:00:00'): SeanceCalendrier => ({
    id, activiteId: id, activiteNom: `Activité ${id}`, horaireActivite: '', salle: 'Salle',
    adresseSalle: null, couleurSalle: null, commentaire: null, lien: null,
    debut, fin, etatSeance: 'PROGRAMMEE'
  });

  beforeEach(() => {
    registerLocaleData(localeFr);
    activites = jasmine.createSpyObj('ActiviteService', ['getCalendrier', 'getCalendrierAdherent', 'getCalendrierGoogle']);
    modales = jasmine.createSpyObj('NgbModal', ['open']);
    activites.getCalendrier.and.returnValue(of([seance(1), seance(2)]));
    activites.getCalendrierAdherent.and.returnValue(of([seance(1)]));
    activites.getCalendrierGoogle.and.returnValue(of({ evenements: [{
      id: 'fete', titre: 'Fête', debut: '2026-10-10', fin: '2026-10-12', journeeEntiere: true,
      lieu: null, commentaire: null, agenda: 'ALOD', agendaSource: 'alod'
    }], erreurs: [] }));
    TestBed.configureTestingModule({
      imports: [AgendaComponent],
      providers: [
        { provide: ActiviteService, useValue: activites },
        { provide: NgbModal, useValue: modales },
        { provide: ParamService, useValue: { getAgendasGoogle: () => of([{ source: 'alod', nom: 'ALOD', isVisisbleApp: true }]) } },
        { provide: TribuService, useValue: { getConnected: () => of({ adherents: [{ id: 42, user: { id: 7 } }] }) } },
        { provide: TokenStorageService, useValue: { getUser: () => ({ id: 7 }) } }
      ]
    });
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
    const template = {} as Parameters<AgendaComponent['ouvrirEvenement']>[1];
    component.ouvrirEvenement(evenement, template);
    expect(component.evenementSelectionne).toBe(evenement);
    expect(modales.open).toHaveBeenCalledWith(template, jasmine.objectContaining({ ariaLabelledBy: 'agenda-detail-titre', scrollable: true }));
  });

  it('adapte le contenu à 1600, 900 et 375 pixels sans débordement horizontal', () => {
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
      }
    } finally {
      iframe.remove();
    }
    const bouton = fixture.nativeElement.querySelector('.agenda-evenement') as HTMLButtonElement;
    bouton.click();
    expect(modales.open).toHaveBeenCalled();
  });
});
