import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActiviteService } from 'src/app/_services/activite.service';
import { SeanceCalendrier } from 'src/app/models/seance';
import { ProchainsEvenementsFamilleComponent } from './prochains-evenements-famille.component';

registerLocaleData(localeFr);

describe('ProchainsEvenementsFamilleComponent', () => {
  const maintenant = new Date('2026-09-02T12:00:00.000Z');
  let fixture: ComponentFixture<ProchainsEvenementsFamilleComponent>;
  let component: ProchainsEvenementsFamilleComponent;
  let activiteService: jasmine.SpyObj<ActiviteService>;

  beforeEach(async () => {
    activiteService = jasmine.createSpyObj<ActiviteService>('ActiviteService', ['getCalendrier']);

    await TestBed.configureTestingModule({
      imports: [ProchainsEvenementsFamilleComponent],
      providers: [{ provide: ActiviteService, useValue: activiteService }]
    }).compileComponents();
  });

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(maintenant);
    fixture = TestBed.createComponent(ProchainsEvenementsFamilleComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => jasmine.clock().uninstall());

  it('regroupe les séances par activité, écarte les passées ou annulées, les trie et en conserve cinq', () => {
    activiteService.getCalendrier.and.returnValue(of([
      seance(106, 12, '2026-09-08T09:00:00.000Z'),
      seance(201, 99, '2026-09-04T18:00:00.000Z'),
      seance(102, 12, '2026-09-04T09:00:00.000Z'),
      seance(104, 12, '2026-09-06T09:00:00.000Z'),
      seance(90, 12, '2026-09-02T11:59:59.999Z'),
      seance(103, 12, '2026-09-05T09:00:00.000Z'),
      seance(105, 12, '2026-09-07T09:00:00.000Z'),
      seance(100, 12, '2026-09-03T08:00:00.000Z', 'ANNULEE'),
      seance(101, 12, '2026-09-03T09:00:00.000Z'),
      seance(202, 99, '2026-09-05T18:00:00.000Z')
    ]));

    fixture.componentRef.setInput('tribuUuid', 'tribu-1');
    fixture.detectChanges();
    fixture.detectChanges();

    expect(activiteService.getCalendrier).toHaveBeenCalled();
    expect(component.activites.map(activite => activite.id)).toEqual([12, 99]);
    expect(component.activites[0].evenements.map(evenement => evenement.id)).toEqual([101, 102, 103, 104, 105]);
    expect(component.activites[1].evenements.map(evenement => evenement.id)).toEqual([201, 202]);
    expect(component.activites.flatMap(activite => activite.evenements.map(evenement => evenement.id)))
      .not.toContain(90);
    expect(component.activites.flatMap(activite => activite.evenements.map(evenement => evenement.id)))
      .not.toContain(100);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('.activite-evenements').length).toBe(2);
    expect(element.querySelectorAll('.evenement-card').length).toBe(7);
  });
});

function seance(
  id: number,
  activiteId: number,
  debut: string,
  etatSeance: SeanceCalendrier['etatSeance'] = 'PROGRAMMEE'
): SeanceCalendrier {
  return {
    id,
    activiteId,
    activiteNom: 'Activité commune',
    horaireActivite: '',
    salle: '',
    adresseSalle: null,
    couleurSalle: null,
    commentaire: null,
    lien: null,
    debut,
    fin: debut,
    etatSeance
  };
}
