import { TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of, Subject, throwError } from 'rxjs';
import { TeamEventModalComponent } from './team-event-modal.component';
import { TeamService } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';

describe('Modale indépendante de création d’événement', () => {
  let component: TeamEventModalComponent;
  let api: jasmine.SpyObj<TeamService>;
  let modal: jasmine.SpyObj<NgbActiveModal>;
  beforeEach(() => {
    api = jasmine.createSpyObj('TeamService', ['eventOptions', 'createEvents']);
    api.eventOptions.and.returnValue(of({ equipes: [], sections: [{ id: 7, nom: 'Danse' }] }));
    modal = jasmine.createSpyObj('NgbActiveModal', ['close', 'dismiss']);
    TestBed.configureTestingModule({ imports: [TeamEventModalComponent], providers: [
      { provide: TeamService, useValue: api }, { provide: NgbActiveModal, useValue: modal },
      { provide: ParamService, useValue: { getSalles: () => of([]) } }
    ] });
    const fixture = TestBed.createComponent(TeamEventModalComponent);
    component = fixture.componentInstance; fixture.detectChanges();
  });
  it('positionne le rendez-vous une heure avant, y compris la veille, et conserve un choix manuel', () => {
    component.startChanged('2026-11-05T00:30');
    expect(component.form.rendezVous).toBe('2026-11-04T23:30');
    component.meetingChanged('2026-11-04T22:00');
    component.startChanged('2026-11-05T01:00');
    expect(component.form.rendezVous).toBe('2026-11-04T22:00');
  });
  it('précoche Sans invitation et transmet ce choix aux événements répétés', async () => {
    const fixture = TestBed.createComponent(TeamEventModalComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const checkbox = fixture.nativeElement.querySelector('input[name="sansInvitation"]') as HTMLInputElement;
    expect(checkbox.checked).toBeTrue();
    const editor = fixture.componentInstance;
    editor.toggleSection(7, true); editor.repeat = true; editor.weeks = 3;
    api.createEvents.and.returnValue(of({ nombre: 3, evenements: [] }));
    editor.save();
    expect(api.createEvents.calls.mostRecent().args[0].sansInvitation).toBeTrue();
    expect(api.createEvents.calls.mostRecent().args.slice(1)).toEqual([[7], 3]);
  });
  it('réactive explicitement les invitations lorsque Sans invitation est décochée', async () => {
    const fixture = TestBed.createComponent(TeamEventModalComponent);
    fixture.detectChanges(); await fixture.whenStable();
    const checkbox = fixture.nativeElement.querySelector('input[name="sansInvitation"]') as HTMLInputElement;
    checkbox.click(); fixture.detectChanges();
    expect(fixture.componentInstance.form.sansInvitation).toBeFalse();
    expect(fixture.nativeElement.querySelector('#sans-invitation-help').textContent).toContain('Les invitations et les réponses');
    const editor = fixture.componentInstance; editor.toggleSection(7, true);
    api.createEvents.and.returnValue(of({ nombre: 1, evenements: [] }));
    editor.save();
    expect(api.createEvents.calls.mostRecent().args[0].sansInvitation).toBeFalse();
  });
  for (const sansInvitation of [false, true, undefined]) {
    it(`conserve le mode de l’événement existant (${sansInvitation}) à la modification`, async () => {
      const fixture = TestBed.createComponent(TeamEventModalComponent);
      fixture.componentInstance.event = { id: 20, type: 'ACTIVITE', debut: '3000-01-01T18:00:00', fin: '3000-01-01T19:00:00',
        etat: 'PROGRAMMEE', lieu: '', salleId: null, rendezVous: '', adversaire: '', commentaire: '', equipes: [],
        canManage: true, convocations: [], emails: {}, sansInvitation };
      fixture.detectChanges(); await fixture.whenStable();
      const checkbox = fixture.nativeElement.querySelector('input[name="sansInvitation"]') as HTMLInputElement;
      expect(checkbox.checked).toBe(sansInvitation ?? false);
      expect(fixture.componentInstance.form.sansInvitation).toBe(sansInvitation ?? false);
    });
  }
  it('transmet les équipes, les sections et la période de répétition et bloque les doubles envois', () => {
    const response = new Subject<any>(); api.createEvents.and.returnValue(response);
    component.toggleSection(7, true); component.toggleTeam(10, true); component.toggleTeam(11, true);
    component.repeat = true; component.weeks = 8; component.save(); component.save();
    expect(api.createEvents).toHaveBeenCalledTimes(1);
    expect(api.createEvents.calls.mostRecent().args.slice(1)).toEqual([[7], 8]);
    expect(api.createEvents.calls.mostRecent().args[0].equipes).toEqual([10, 11]);
    response.next({ nombre: 6, evenements: [] });
    expect(modal.close).toHaveBeenCalledWith({ nombre: 6, evenements: [] });
  });
  it('conserve le formulaire après un échec du calendrier et valide le nombre de semaines', () => {
    component.toggleSection(7, true); component.repeat = true; component.weeks = 2.5;
    component.save(); expect(api.createEvents).not.toHaveBeenCalled();
    component.weeks = 8;
    api.createEvents.and.returnValue(throwError(() => ({ error: { detail: 'Calendrier indisponible' } })));
    component.save();
    expect(component.error).toBe('Calendrier indisponible'); expect(component.busy).toBeFalse();
    expect(component.selectedSections).toEqual([7]); expect(modal.close).not.toHaveBeenCalled();
  });
});
