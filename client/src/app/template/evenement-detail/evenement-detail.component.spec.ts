import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { PresencePrevueService } from '../../_services/presence-prevue.service';
import { Evenement } from '../../models/evenement';
import { PresencePrevue } from '../../models/seance';
import { EvenementDetailComponent } from './evenement-detail.component';

describe('EvenementDetailComponent', () => {
  let fixture: ComponentFixture<EvenementDetailComponent>;
  let component: EvenementDetailComponent;
  let presences: jasmine.SpyObj<PresencePrevueService>;
  const reponses = (): PresencePrevue[] => [
    { id: 1, adherentId: 42, prenom: 'Alice', nom: 'Dupont', presencePrevue: null },
    { id: 2, adherentId: 43, prenom: 'Paul', nom: 'Martin', presencePrevue: true }
  ];
  const seance = (): Evenement => ({
    id: 'seance-15', source: 'SEANCE', seanceId: 15, personnelle: true,
    titre: 'Basket – Match', activiteNom: 'Basket', descriptif: 'Match',
    debut: '3000-10-07T18:00:00', fin: '3000-10-07T19:30:00', journeeEntiere: false,
    lieu: 'Gymnase', adresseSalle: '1 rue des Sports', couleurSalle: '#AB47BC',
    sectionNom: 'Sports', sectionType: 'COMPETITION', etatSeance: 'PROGRAMMEE'
  });

  beforeEach(() => {
    registerLocaleData(localeFr);
    presences = jasmine.createSpyObj('PresencePrevueService', ['getPresences', 'updatePresence']);
    presences.getPresences.and.returnValue(of(reponses()));
    TestBed.configureTestingModule({
      imports: [EvenementDetailComponent],
      providers: [
        { provide: NgbActiveModal, useValue: jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']) },
        { provide: PresencePrevueService, useValue: presences }
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
    for (const texte of ['Basket', 'Match', 'Sports', '19:30', 'Mercredi 18h', 'Annulée', 'Gymnase indisponible', 'Report à venir', 'Accueil : Paul Martin']) {
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
    expect(presences.getPresences).not.toHaveBeenCalled();
  });

  it('ne demande aucune présence pour une séance d’une autre section ou d’un autre adhérent', () => {
    component.evenement.personnelle = false;
    fixture.detectChanges();
    expect(presences.getPresences).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.seance-presences-totaux')).toBeNull();
    component.evenement = { ...seance(), sectionType: 'NON_COMPETITIVE' };
    component.chargerPresences();
    expect(presences.getPresences).not.toHaveBeenCalled();
  });

  it('enregistre la réponse et recharge toutes les présences dans le même objet que la carte', () => {
    const partage = component.evenement;
    const notifier = jasmine.createSpy('notifier');
    component.presenceChange.subscribe(notifier);
    fixture.detectChanges();
    presences.updatePresence.and.returnValue(of({ ...reponses()[0], presencePrevue: true }));
    presences.getPresences.and.returnValue(of(reponses().map(p => ({ ...p, presencePrevue: true }))));
    fixture.nativeElement.querySelector('.btn-group button').click();
    fixture.detectChanges();
    expect(presences.updatePresence).toHaveBeenCalledOnceWith(15, true);
    expect(presences.getPresences).toHaveBeenCalledTimes(2);
    expect(partage.presences![0].presencePrevue).toBeTrue();
    expect(partage.enregistrementPresence).toBeFalse();
    expect(fixture.nativeElement.querySelector('.seance-presences-totaux').textContent).toContain('Présents : 2');
    expect(fixture.nativeElement.querySelector('.evenement-participants').textContent).toContain('Alice Dupont : Présent');
    expect(notifier).toHaveBeenCalled();
  });

  it('conserve les réponses en cas d’échec et permet de réessayer', () => {
    fixture.detectChanges();
    presences.updatePresence.and.returnValue(throwError(() => ({ error: { detail: 'Enregistrement indisponible' } })));
    component.definirPresence(false);
    fixture.detectChanges();
    expect(component.maPresence!.presencePrevue).toBeNull();
    expect(component.evenement.enregistrementPresence).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Enregistrement indisponible');
    presences.updatePresence.and.returnValue(of({ ...reponses()[0], presencePrevue: false }));
    presences.getPresences.and.returnValue(of(reponses().map(p => p.id === 1 ? { ...p, presencePrevue: false } : p)));
    component.definirPresence(false);
    expect(component.maPresence!.presencePrevue).toBeFalse();
    expect(component.evenement.erreurPresence).toBe('');
  });

  it('libère l’état partagé si la modale est fermée pendant un enregistrement', () => {
    const enregistrement = new Subject<PresencePrevue>();
    presences.updatePresence.and.returnValue(enregistrement);
    fixture.detectChanges();
    component.definirPresence(true);
    component.definirPresence(false);
    expect(presences.updatePresence).toHaveBeenCalledTimes(1);
    const partage = component.evenement;
    expect(partage.enregistrementPresence).toBeTrue();
    fixture.destroy();
    expect(partage.enregistrementPresence).toBeFalse();
    expect(partage.presences).toBeUndefined();
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
