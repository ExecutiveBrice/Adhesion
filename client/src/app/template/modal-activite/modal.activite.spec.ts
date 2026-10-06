import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, throwError } from 'rxjs';
import { ActiviteService } from '../../_services/activite.service';
import { AdherentService } from '../../_services/adherent.service';
import { ParamService } from '../../_services/param.service';
import { ToastService } from '../../_services/toast.service';
import { Activite } from '../../models';
import { ModalActivite } from './modal.activite';
import { Seance } from '../../models/seance';

describe('ModalActivite : section obligatoire', () => {
  let fixture: ComponentFixture<ModalActivite>;
  let component: ModalActivite;
  let activites: jasmine.SpyObj<ActiviteService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  let toasts: jasmine.SpyObj<ToastService>;
  const section = { id: 7, nom: 'Yoga', type: 'NON_COMPETITIVE' as const };

  beforeEach(() => {
    activites = jasmine.createSpyObj('ActiviteService', ['save']);
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    toasts = jasmine.createSpyObj('ToastService', ['warning', 'success', 'error']);
    TestBed.configureTestingModule({
      imports: [ModalActivite],
      providers: [
        { provide: ActiviteService, useValue: activites },
        { provide: NgbActiveModal, useValue: modal },
        { provide: ToastService, useValue: toasts },
        { provide: AdherentService, useValue: { getByRole: () => of([]) } },
        { provide: ParamService, useValue: { getSections: () => of([section]), getSalles: () => of([]), getTachesSeance: () => of([]) } }
      ]
    });
    fixture = TestBed.createComponent(ModalActivite);
    component = fixture.componentInstance;
    component.activite = new Activite();
    activites.save.and.callFake(activite => of(activite));
  });

  it('bloque le bouton tant qu’aucune section n’est choisie puis enregistre la section sélectionnée', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const bouton = fixture.nativeElement.querySelector('.modal-footer .btn-success') as HTMLButtonElement;
    const select = fixture.nativeElement.querySelector('#sectionActivite') as HTMLSelectElement;
    expect(bouton.disabled).toBeTrue();
    expect(select.required).toBeTrue();
    expect(select.options[0].disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Choisissez une section pour enregistrer l’activité.');
    bouton.click();
    expect(activites.save).not.toHaveBeenCalled();

    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(bouton.disabled).toBeFalse();
    bouton.click();
    expect(activites.save).toHaveBeenCalledOnceWith(component.activite);
    expect(component.activite.section).toEqual(section);
    expect(modal.close).toHaveBeenCalledWith('valider');
  });

  it('refuse aussi un appel direct à l’enregistrement pour une création ou une modification sans section', () => {
    for (const id of [undefined, 12]) {
      component.activite.id = id!;
      component.enregistrer();
    }
    expect(activites.save).not.toHaveBeenCalled();
    expect(modal.close).not.toHaveBeenCalled();
    expect(toasts.warning).toHaveBeenCalledWith('Sélectionnez une section avant d’enregistrer l’activité.', 'Section obligatoire');
  });
});

describe('ModalActivite : responsabilités des séances', () => {
  let fixture: ComponentFixture<ModalActivite>;
  let component: ModalActivite;
  let activites: jasmine.SpyObj<ActiviteService>;
  let seance: Seance;
  const tache = { id: 3, nom: 'Matériel' };
  const adherent = { id: 12, nom: 'Dupont', prenom: 'Alice' };

  beforeEach(async () => {
    activites = jasmine.createSpyObj('ActiviteService', ['getSeances', 'getReferentsCandidates',
      'getResponsabiliteCandidates', 'modifierResponsabilitesSeance']);
    seance = Object.assign(new Seance(), { id: 9, debut: '2026-10-06T18:30:00', fin: '2026-10-06T19:30:00',
      etatSeance: 'PROGRAMMEE', responsabilites: [] });
    activites.getSeances.and.returnValue(of([seance]));
    activites.getReferentsCandidates.and.returnValue(of([]));
    activites.getResponsabiliteCandidates.and.returnValue(of([adherent]));
    activites.modifierResponsabilitesSeance.and.returnValue(of({ ...seance, responsabilites: [{ tache, adherent }] }));
    TestBed.configureTestingModule({
      imports: [ModalActivite],
      providers: [
        { provide: ActiviteService, useValue: activites },
        { provide: NgbActiveModal, useValue: jasmine.createSpyObj('modal', ['close', 'dismiss']) },
        { provide: ToastService, useValue: jasmine.createSpyObj('toasts', ['warning', 'success', 'error']) },
        { provide: AdherentService, useValue: { getByRole: () => of([]) } },
        { provide: ParamService, useValue: { getSections: () => of([]), getSalles: () => of([]), getTachesSeance: () => of([tache]) } }
      ]
    });
    fixture = TestBed.createComponent(ModalActivite);
    component = fixture.componentInstance;
    component.activite = Object.assign(new Activite(), { id: 5, section: { id: 7, nom: 'Yoga', type: 'NON_COMPETITIVE' } });
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('sélectionne une tâche du paramétrage et un adhérent de la section puis les enregistre', async () => {
    component.ajouterResponsabilite(seance);
    fixture.detectChanges();
    await fixture.whenStable();
    const selectTache = fixture.nativeElement.querySelector('[aria-label="Tâche"]') as HTMLSelectElement;
    const selectAdherent = fixture.nativeElement.querySelector('[aria-label="Adhérent de la section"]') as HTMLSelectElement;
    selectTache.value = selectTache.options[1].value;
    selectTache.dispatchEvent(new Event('change'));
    selectAdherent.value = selectAdherent.options[1].value;
    selectAdherent.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    const bouton = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find(element => element.textContent?.includes('Enregistrer les responsabilités'))!;
    bouton.click();
    await fixture.whenStable();
    expect(activites.modifierResponsabilitesSeance).toHaveBeenCalledOnceWith(5, 9, [{ tacheId: 3, adherentId: 12 }]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Matériel : Alice Dupont');
  });

  it('refuse les lignes incomplètes et les doublons, et permet une liste vide', () => {
    component.ajouterResponsabilite(seance);
    component.enregistrerResponsabilites(seance);
    expect(activites.modifierResponsabilitesSeance).not.toHaveBeenCalled();
    component.responsabilitesEdition[9] = [{ tacheId: 3, adherentId: 12 }, { tacheId: 3, adherentId: 12 }];
    component.enregistrerResponsabilites(seance);
    expect(activites.modifierResponsabilitesSeance).not.toHaveBeenCalled();
    component.retirerResponsabilite(seance, 1);
    component.retirerResponsabilite(seance, 0);
    component.enregistrerResponsabilites(seance);
    expect(activites.modifierResponsabilitesSeance).toHaveBeenCalledOnceWith(5, 9, []);
  });

  it('conserve les choix lorsque le serveur refuse une affectation', () => {
    activites.modifierResponsabilitesSeance.and.returnValue(throwError(() => ({ error: { detail: 'Adhérent hors section' } })));
    component.responsabilitesEdition[9] = [{ tacheId: 3, adherentId: 12 }];
    component.enregistrerResponsabilites(seance);
    expect(component.responsabilitesErreur[9]).toBe('Adhérent hors section');
    expect(component.responsabilitesEdition[9]).toEqual([{ tacheId: 3, adherentId: 12 }]);
    expect(seance.responsabilites).toEqual([]);
  });
});
