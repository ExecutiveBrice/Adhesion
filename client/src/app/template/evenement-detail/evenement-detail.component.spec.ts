import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { Evenement } from '../../models/evenement';
import { EvenementDetailComponent } from './evenement-detail.component';

describe('EvenementDetailComponent', () => {
  let fixture: ComponentFixture<EvenementDetailComponent>;
  let component: EvenementDetailComponent;
  const seance = (): Evenement => ({
    id: 'seance-15', source: 'SEANCE', seanceId: 15, personnelle: true,
    titre: 'Basket – Match', activiteNom: 'Basket', descriptif: 'Match',
    debut: '3000-10-07T18:00:00', fin: '3000-10-07T19:30:00', journeeEntiere: false,
    lieu: 'Gymnase', adresseSalle: '1 rue des Sports', couleurSalle: '#AB47BC',
    sectionNom: 'Sports', sectionType: 'COMPETITION', etatSeance: 'PROGRAMMEE'
  });

  beforeEach(() => {
    registerLocaleData(localeFr);
    TestBed.configureTestingModule({
      imports: [EvenementDetailComponent],
      providers: [provideRouter([]),
        { provide: NgbActiveModal, useValue: jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']) },

      ]
    });
    fixture = TestBed.createComponent(EvenementDetailComponent);
    component = fixture.componentInstance;
    component.evenement = seance();
    component.adherentId = 42;
  });

  it('affiche les détails d’une séance annulée et son motif sans proposer de réponse', () => {
    component.evenement = { ...seance(), etatSeance: 'ANNULEE', causeAnnulation: 'Gymnase indisponible',
      commentaire: '<p>Report à venir</p>', lien: 'example.org/basket', horaireActivite: 'Mercredi 18h',
      responsabilites: [{ tache: { nom: 'Accueil', icone: 'handshake', couleur: '#AB47BC' }, adherent: { id: 43, prenom: 'Paul', nom: 'Martin' } }]
    };
    fixture.detectChanges();
    for (const texte of ['Basket', 'Match', 'Sports', '19:30', 'Annulée', 'Gymnase indisponible', 'Report à venir', 'Accueil : Paul Martin']) {
      expect(fixture.nativeElement.textContent).toContain(texte);
    }
    expect(fixture.nativeElement.querySelector('.btn-group')).toBeNull();
    expect(component.lienGoogleAgenda).toBeNull();
    expect(component.lienActivite).toBe('https://example.org/basket');
    expect(component.destination!.waze).toContain('1%20rue%20des%20Sports');
  });

  it('respecte la fin exclusive d’un événement Google sur plusieurs jours et ses liens', () => {
    component.evenement = { id: 'google-fete', source: 'GOOGLE', titre: 'Fête',
      debut: '2026-10-30', fin: '2026-11-02', journeeEntiere: true, lieu: 'Maison des associations', agenda: 'Vie associative' };
    fixture.detectChanges();
    expect(component.dernierJour).toBe('2026-11-01');
    expect(fixture.nativeElement.textContent).toContain('Toute la journée');
    expect(fixture.nativeElement.textContent).toContain('Vie associative');
    expect(component.destination!.adresse).toBe('Maison des associations');
    expect(new URL(component.lienGoogleAgenda!).searchParams.get('dates')).toBe('20261030/20261102');

  });

  it('oriente les convocations vers Mes équipes quelle que soit la section', () => {
    for (const sectionType of ['COMPETITION', 'NON_COMPETITIVE']) {
      fixture.componentRef.setInput('evenement', { ...seance(), sectionType });
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('a[href="/equipes"]')).not.toBeNull();

    }
  });

  it('exporte les journées entières en conservant la date de fin exclusive', async () => {
    component.evenement = { id: 'google-fete', source: 'GOOGLE', titre: 'Fête, ALOD',
      debut: '2026-10-30', fin: '2026-11-02', journeeEntiere: true, lieu: null };
    const creer = spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');
    component.telechargerEvenement();
    const contenu = await (creer.calls.mostRecent().args[0] as Blob).text();
    expect(contenu).toContain('DTSTART;VALUE=DATE:20261030');
    expect(contenu).toContain('DTEND;VALUE=DATE:20261102');
    expect(contenu).toContain('SUMMARY:Fête\\, ALOD');
    expect(URL.revokeObjectURL).toHaveBeenCalledOnceWith('blob:test');
  });

  it('conserve les horaires de Paris fournis par l’API lors de l’ajout et de l’export', async () => {
    component.evenement = { ...seance(), debut: '2026-10-07T18:00:00', fin: '2026-10-07T19:30:00' };
    expect(new URL(component.lienGoogleAgenda!).searchParams.get('dates')).toBe('20261007T180000/20261007T193000');
    expect(new URL(component.lienGoogleAgenda!).searchParams.get('ctz')).toBe('Europe/Paris');
    const creer = spyOn(URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');
    component.telechargerEvenement();
    const contenu = await (creer.calls.mostRecent().args[0] as Blob).text();
    expect(contenu).toContain('DTSTART;TZID=Europe/Paris:20261007T180000');
    expect(contenu).toContain('DTEND;TZID=Europe/Paris:20261007T193000');
  });
});
