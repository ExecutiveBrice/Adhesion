import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IconeTacheSelectComponent } from './icone-tache-select.component';

describe('IconeTacheSelectComponent', () => {
  let fixture: ComponentFixture<IconeTacheSelectComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [IconeTacheSelectComponent] });
    fixture = TestBed.createComponent(IconeTacheSelectComponent);
  });

  it('propose les icônes Font Awesome et émet la sélection avec un aperçu coloré', async () => {
    fixture.componentRef.setInput('icone', 'box-open');
    fixture.componentRef.setInput('couleur', '#AB47BC');
    const selection = jasmine.createSpy('selection');
    fixture.componentInstance.iconeChange.subscribe(selection);
    fixture.detectChanges();
    const bouton = fixture.nativeElement.querySelector('[ngbDropdownToggle]') as HTMLButtonElement;
    expect(bouton.textContent).toContain('Matériel');
    expect(bouton.querySelector('svg')?.getAttribute('data-icon')).toBe('box-open');
    expect((bouton.querySelector('fa-icon') as HTMLElement).style.color).toBe('rgb(171, 71, 188)');
    bouton.click();
    await fixture.whenStable();
    const choix = Array.from(document.querySelectorAll('.icones-taches-menu button') as NodeListOf<HTMLButtonElement>);
    expect(choix.length).toBe(28);
    choix.find(element => element.textContent?.includes('Buvette'))!.click();
    expect(selection).toHaveBeenCalledWith('mug-hot');
    fixture.componentRef.setInput('icone', 'mug-hot');
    fixture.detectChanges();
    expect(bouton.querySelector('svg')?.getAttribute('data-icon')).toBe('mug-hot');
  });

  it('désactive le sélecteur pendant la sauvegarde', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('button') as HTMLButtonElement).disabled).toBeTrue();
  });
});
