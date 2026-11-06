import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CalendrierMaterielComponent } from './calendrier-materiel.component';
import { MaterielService } from '../../_services/materiel.service';
import { EmpruntCalendrierMateriel } from '../../models/materiel';

describe('Calendrier du matériel', () => {
  let fixture: ComponentFixture<CalendrierMaterielComponent>;
  let service: jasmine.SpyObj<MaterielService>;
  const entree: EmpruntCalendrierMateriel = {
    materielId: 2, materielNom: 'Tables', zonage: { id: 1, nom: 'Réserve', couleur: '#123456' },
    emprunt: { id: 10, emprunteurId: 3, emprunteurNom: 'Camille Martin', quantite: 4, quantiteRetournee: 1,
      dateEmprunt: '2026-12-01T12:00:00Z', dateDebut: '2026-12-31', dateFinPrevue: '2027-01-02',
      dateRetour: null, enregistreParNom: null, rapporteParNom: null }
  };

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 11, 15, 12));
    service = jasmine.createSpyObj('MaterielService', ['getCalendrier']);
    service.getCalendrier.and.returnValue(of([entree]));
    TestBed.configureTestingModule({ imports: [CalendrierMaterielComponent], providers: [{ provide: MaterielService, useValue: service }] });
  });

  afterEach(() => { fixture?.destroy(); jasmine.clock().uninstall(); });

  function afficher() {
    fixture = TestBed.createComponent(CalendrierMaterielComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function jour(iso: string) {
    return fixture.nativeElement.querySelector(`[data-date="${iso}"]`) as HTMLButtonElement;
  }

  it('affiche le mois courant et le suivant, du lundi au dimanche, en passant à l’année suivante', () => {
    afficher();
    expect(service.getCalendrier).toHaveBeenCalledOnceWith('2026-12-01', '2027-01-31');
    const mois = fixture.nativeElement.querySelectorAll('.mois');
    expect(mois.length).toBe(2);
    expect(mois[0].querySelector('h2').textContent).toBe('décembre 2026');
    expect(mois[1].querySelector('h2').textContent).toBe('janvier 2027');
    expect(mois[0].querySelector('.jours-mois').children[0].classList).toContain('jour-vide');
    expect(mois[0].querySelector('.jours-mois').children[1].dataset.date).toBe('2026-12-01');
    expect(jour('2026-12-15').getAttribute('aria-current')).toBe('date');
  });

  it('navigue de mois en mois dans les deux sens et charge les emprunts de la période affichée', () => {
    const component = afficher();
    const suivant = fixture.nativeElement.querySelector('[aria-label="Mois suivant"]') as HTMLButtonElement;
    const precedent = fixture.nativeElement.querySelector('[aria-label="Mois précédent"]') as HTMLButtonElement;
    suivant.click(); fixture.detectChanges();
    expect(service.getCalendrier).toHaveBeenCalledWith('2027-01-01', '2027-02-28');
    expect(fixture.nativeElement.querySelector('.calendrier-navigation h2').textContent).toBe('janvier 2027');
    expect(jour('2027-01-01').textContent).toContain('Tables');
    expect(component.jourSelectionne()?.iso).toBe('2027-01-01');

    precedent.click(); fixture.detectChanges();
    expect(component.jourSelectionne()?.iso).toBe('2026-12-15');
    precedent.click(); fixture.detectChanges();
    expect(service.getCalendrier).toHaveBeenCalledWith('2026-11-01', '2026-12-31');
    expect(fixture.nativeElement.querySelector('.calendrier-navigation h2').textContent).toBe('novembre 2026');
    expect(component.jourSelectionne()?.iso).toBe('2026-11-01');
  });

  it('conserve le mois consulté et le jour choisi après une actualisation du matériel', () => {
    const component = afficher();
    component.changerMois(2); fixture.detectChanges();
    jour('2027-02-10').click(); fixture.detectChanges();
    service.getCalendrier.calls.reset();
    fixture.componentRef.setInput('materiels', []); fixture.detectChanges();
    expect(service.getCalendrier).toHaveBeenCalledOnceWith('2027-02-01', '2027-03-31');
    expect(component.jourSelectionne()?.iso).toBe('2027-02-10');
  });

  it('garde la navigation utilisable pendant le chargement et après une erreur', () => {
    service.getCalendrier.and.returnValue(new Subject<EmpruntCalendrierMateriel[]>());
    afficher();
    const suivant = fixture.nativeElement.querySelector('[aria-label="Mois suivant"]') as HTMLButtonElement;
    service.getCalendrier.and.returnValue(throwError(() => new Error('réseau')));
    suivant.click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Mois suivant"]')).toBe(suivant);
    service.getCalendrier.and.returnValue(of([]));
    suivant.click(); fixture.detectChanges();
    expect(service.getCalendrier).toHaveBeenCalledWith('2027-02-01', '2027-03-31');
    expect(jour('2027-02-01')).not.toBeNull();
  });

  it('affiche les emprunts sur toute leur période, bornes incluses, et détaille le jour sélectionné', () => {
    afficher();
    expect(jour('2026-12-30').querySelector('.jour-emprunt')).toBeNull();
    for (const iso of ['2026-12-31', '2027-01-01', '2027-01-02']) {
      expect(jour(iso).textContent).toContain('Tables');
    }
    expect(jour('2027-01-03').querySelector('.jour-emprunt')).toBeNull();
    jour('2027-01-01').click(); fixture.detectChanges();
    const detail = fixture.nativeElement.querySelector('.calendrier-detail').textContent;
    expect(detail).toContain('Tables × 4');
    expect(detail).toContain('Camille Martin · Réserve');
    expect(detail).toContain('31/12/2026');
    expect(detail).toContain('02/01/2027');
    expect(detail).toContain('3 restant(s) à rendre');
    expect(jour('2027-01-01').getAttribute('aria-pressed')).toBe('true');
  });

  it('conserve les emprunts rapportés jusqu’au retour réel et les retards jusqu’à aujourd’hui', () => {
    service.getCalendrier.and.returnValue(of([
      { ...entree, emprunt: { ...entree.emprunt, dateDebut: '2026-12-01', dateFinPrevue: '2026-12-20', dateRetour: '2026-12-03T12:00:00Z' } },
      { ...entree, materielNom: 'Enceinte', emprunt: { ...entree.emprunt, id: 11, dateDebut: '2026-11-29', dateFinPrevue: '2026-12-02' } },
      { ...entree, materielNom: 'Événement HS', emprunt: { ...entree.emprunt, id: 12, horsService: false } }
    ]));
    afficher();
    expect(jour('2026-12-03').textContent).toContain('Tables');
    expect(jour('2026-12-04').textContent).not.toContain('Tables');
    expect(jour('2026-12-15').textContent).toContain('Enceinte');
    expect(jour('2026-12-16').textContent).not.toContain('Enceinte');
    expect(fixture.nativeElement.textContent).not.toContain('Événement HS');
  });

  it('rend tous les emprunts consultables même lorsque la case ne montre que les deux premiers', () => {
    service.getCalendrier.and.returnValue(of([entree, { ...entree, emprunt: { ...entree.emprunt, id: 11 } },
      { ...entree, emprunt: { ...entree.emprunt, id: 12 } }]));
    afficher();
    expect(jour('2027-01-01').querySelectorAll('.jour-emprunt').length).toBe(2);
    expect(jour('2027-01-01').textContent).toContain('+1');
    jour('2027-01-01').click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.emprunt-detail').length).toBe(3);
  });

  it('affiche les emprunts programmés en bleu dans les cases et le détail', () => {
    afficher();
    const emprunt = jour('2026-12-31').querySelector('.jour-emprunt')!;
    expect(emprunt.classList).toContain('a-venir');
    expect(emprunt.getAttribute('title')).toContain('Programmé');
    jour('2026-12-31').click(); fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.emprunt-detail .badge');
    expect(badge.classList).toContain('text-bg-primary');
    expect(badge.textContent).toBe('Programmé');
  });

  it('garde un emprunt en dépassement rouge après un retour partiel puis le passe au vert à sa restitution complète', () => {
    const retard = { ...entree, emprunt: {
      ...entree.emprunt, dateDebut: '2026-12-01', dateFinPrevue: '2026-12-14', quantiteRetournee: 3
    } };
    service.getCalendrier.and.returnValue(of([retard]));
    const component = afficher();
    expect(jour('2026-12-15').querySelector('.jour-emprunt')!.classList).toContain('depassement');
    let badge = fixture.nativeElement.querySelector('.emprunt-detail .badge');
    expect(badge.classList).toContain('text-bg-danger');
    expect(badge.textContent).toBe('En dépassement');
    expect(fixture.nativeElement.querySelector('.calendrier-detail').textContent).toContain('1 restant(s) à rendre');

    service.getCalendrier.and.returnValue(of([{ ...retard, emprunt: {
      ...retard.emprunt, dateRetour: '2026-12-15T12:00:00Z', quantiteRetournee: 4
    } }]));
    component.charger(); fixture.detectChanges();
    const empruntRendu = jour('2026-12-15').querySelector('.jour-emprunt')!;
    expect(empruntRendu.classList).toContain('rapporte');
    expect(empruntRendu.classList).not.toContain('depassement');
    badge = fixture.nativeElement.querySelector('.emprunt-detail .badge');
    expect(badge.classList).toContain('text-bg-success');
    expect(badge.textContent).toBe('Terminé');
  });

  it('déclare le dépassement le lendemain de la fin prévue, même si la page reste ouverte', () => {
    jasmine.clock().mockDate(new Date(2026, 11, 15, 23, 59, 59));
    service.getCalendrier.and.returnValue(of([{ ...entree, emprunt: {
      ...entree.emprunt, dateDebut: '2026-12-01', dateFinPrevue: '2026-12-15'
    } }]));
    afficher();
    expect(jour('2026-12-15').querySelector('.jour-emprunt')!.classList).toContain('en-cours');
    jasmine.clock().tick(1000); fixture.detectChanges();
    expect(jour('2026-12-16').querySelector('.jour-emprunt')!.classList).toContain('depassement');
  });

  it('permet de réessayer après une erreur et recharge après une opération sur le matériel', () => {
    service.getCalendrier.and.returnValue(throwError(() => new Error('réseau')));
    afficher();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('n’a pas pu être chargé');
    service.getCalendrier.and.returnValue(of([entree]));
    fixture.nativeElement.querySelector('.alert button').click(); fixture.detectChanges();
    expect(jour('2026-12-31').textContent).toContain('Tables');
    fixture.componentRef.setInput('materiels', []); fixture.detectChanges();
    expect(service.getCalendrier).toHaveBeenCalledTimes(3);
  });

  it('annule la requête précédente lorsqu’une nouvelle actualisation commence', () => {
    const ancienne = new Subject<EmpruntCalendrierMateriel[]>();
    service.getCalendrier.and.returnValue(ancienne);
    const component = afficher();
    service.getCalendrier.and.returnValue(of([entree]));
    component.charger();
    ancienne.next([]); fixture.detectChanges();
    expect(jour('2026-12-31').textContent).toContain('Tables');
  });

  it('préserve les journées autour du changement d’heure et du 29 février', () => {
    jasmine.clock().mockDate(new Date(2028, 1, 29, 12));
    service.getCalendrier.and.returnValue(of([{ ...entree, emprunt: {
      ...entree.emprunt, dateDebut: '2028-03-25', dateFinPrevue: '2028-03-27'
    } }]));
    afficher();
    expect(service.getCalendrier).toHaveBeenCalledOnceWith('2028-02-01', '2028-03-31');
    expect(jour('2028-02-29')).not.toBeNull();
    for (const iso of ['2028-03-25', '2028-03-26', '2028-03-27']) {
      expect(jour(iso).textContent).toContain('Tables');
    }
  });
});
