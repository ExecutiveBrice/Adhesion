import { TestBed } from '@angular/core/testing';
import { PaiementEditorComponent } from './paiement-editor.component';

describe('PaiementEditorComponent', () => {
  const paiement = { id: 1, montant: 20, dateReglement: '2026-09-30', typeReglement: 'HelloAsso' };

  function editor() {
    const fixture = TestBed.createComponent(PaiementEditorComponent);
    fixture.componentRef.setInput('paiements', [{ ...paiement }]);
    fixture.componentRef.setInput('canDelete', true);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [PaiementEditorComponent] }));

  it('saves a changed amount on blur without mutating the persisted value', () => {
    const fixture = editor();
    const component = fixture.componentInstance;
    spyOn(component.save, 'emit');
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="number"]');
    input.value = '35';
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    expect(component.save.emit).toHaveBeenCalledWith(jasmine.objectContaining({ id: 1, montant: 35 }));
    expect(component.paiements[0].montant).toBe(20);
  });

  it('saves the selected payment type, including CB, and prevents unchanged writes', () => {
    const component = editor().componentInstance;
    spyOn(component.save, 'emit');
    component.savePaiement(component.rows[0]);
    expect(component.save.emit).not.toHaveBeenCalled();
    component.selectType(component.rows[0], 'CB');
    expect(component.save.emit).toHaveBeenCalledWith(jasmine.objectContaining({ typeReglement: 'CB' }));
  });

  it('rejects a fractional amount and an impossible date', () => {
    const component = editor().componentInstance;
    spyOn(component.save, 'emit');
    component.draft(component.rows[0]).montant = 2.5;
    component.savePaiement(component.rows[0]);
    component.draft(component.rows[0]).montant = 20;
    component.draft(component.rows[0]).dateReglement = '2026-02-30';
    component.savePaiement(component.rows[0]);
    expect(component.save.emit).not.toHaveBeenCalled();
    expect(component.erreur).toBeTruthy();
  });

  it('adds a complete draft using the period date and can cancel it locally', () => {
    const fixture = editor();
    fixture.componentRef.setInput('addAsDraft', true);
    fixture.componentRef.setInput('defaultDate', '2026-09-01');
    fixture.detectChanges();
    const component = fixture.componentInstance;
    spyOn(component.save, 'emit');
    spyOn(component.remove, 'emit');
    component.addPaiement();
    const draft = component.rows[1];
    expect(draft.dateReglement).toBe('2026-09-01');
    component.savePaiement(draft);
    expect(component.save.emit).not.toHaveBeenCalled();
    component.draft(draft).montant = 15;
    component.savePaiement(draft);
    expect(component.save.emit).toHaveBeenCalledWith(jasmine.objectContaining({ montant: 15, dateReglement: '2026-09-01' }));
    component.removePaiement(draft);
    expect(component.rows.length).toBe(1);
    expect(component.remove.emit).not.toHaveBeenCalled();
  });

  it('retains add and delete actions for the adhesion page', () => {
    const component = editor().componentInstance;
    spyOn(component.add, 'emit');
    spyOn(component.remove, 'emit');
    component.addPaiement();
    component.removePaiement(component.rows[0]);
    expect(component.add.emit).toHaveBeenCalled();
    expect(component.remove.emit).toHaveBeenCalledWith(1);
    component.disabled = true;
    component.removePaiement(component.rows[0]);
    expect(component.remove.emit).toHaveBeenCalledTimes(1);
  });
});
