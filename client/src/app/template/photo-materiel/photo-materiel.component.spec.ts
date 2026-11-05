import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PhotoMaterielComponent } from './photo-materiel.component';

describe('Photo du matériel', () => {
  let fixture: ComponentFixture<PhotoMaterielComponent>;
  let input: HTMLInputElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [PhotoMaterielComponent] });
    fixture = TestBed.createComponent(PhotoMaterielComponent);
    fixture.detectChanges();
    input = fixture.nativeElement.querySelector('input[type="file"]');
  });

  function choisir(fichier: File): void {
    const transfert = new DataTransfer();
    transfert.items.add(fichier);
    input.files = transfert.files;
    input.dispatchEvent(new Event('change'));
  }

  function photo(): string {
    const canvas = document.createElement('canvas');
    canvas.width = 1200; canvas.height = 600;
    const dessin = canvas.getContext('2d')!;
    for (const [x, couleur] of [[0, '#f00'], [400, '#0f0'], [800, '#00f']] as const) {
      dessin.fillStyle = couleur;
      dessin.fillRect(x, 0, 400, 600);
    }
    return canvas.toDataURL('image/png');
  }

  // Observe le chargement réel des images, y compris les erreurs de décodage.
  function prochainDecodage(): Promise<void> {
    const constructeur = window.Image;
    return new Promise(resolve => {
      spyOn(window, 'Image').and.callFake(function () {
        const image = new constructeur();
        image.addEventListener('load', () => setTimeout(resolve, 0), { once: true });
        image.addEventListener('error', () => setTimeout(resolve, 0), { once: true });
        return image;
      });
    });
  }

  it('attend la validation du recadrage et exporte la zone choisie au format 640 × 480', async () => {
    const component = fixture.componentInstance;
    const etats: boolean[] = [];
    component.chargementChange.subscribe(valeur => etats.push(valeur));
    const changement = spyOn(component.imageChange, 'emit');
    const decodage = prochainDecodage();
    const donnees = Uint8Array.from(atob(photo().split(',')[1]), caractere => caractere.charCodeAt(0));
    choisir(new File([donnees], 'photo.png', { type: 'image/png' }));
    expect(component.chargement).toBeTrue();
    await decodage;
    fixture.detectChanges();
    expect(component.chargement).toBeFalse();
    expect(component.source).toBeDefined();
    expect(etats).toEqual([true]);
    expect(changement).not.toHaveBeenCalled();
    const zoom = fixture.nativeElement.querySelector('#photoZoom') as HTMLInputElement;
    zoom.value = '2'; zoom.dispatchEvent(new Event('input'));
    const position = fixture.nativeElement.querySelector('#photoHorizontal') as HTMLInputElement;
    position.value = '100'; position.dispatchEvent(new Event('input'));
    component.valider();
    expect(etats).toEqual([true, false]);
    expect(component.source).toBeUndefined();
    const image = document.createElement('img');
    image.src = changement.calls.mostRecent().args[0]!;
    await image.decode();
    expect(image.naturalWidth).toBe(640);
    expect(image.naturalHeight).toBe(480);
    const canvas = document.createElement('canvas');
    canvas.width = 640; canvas.height = 480;
    const dessin = canvas.getContext('2d')!;
    dessin.drawImage(image, 0, 0);
    const pixel = dessin.getImageData(320, 240, 1, 1).data;
    expect(pixel[0]).toBeLessThan(10);
    expect(pixel[1]).toBeLessThan(10);
    expect(pixel[2]).toBeGreaterThan(245);
  });

  it('permet de recadrer une photo existante et d’annuler sans la modifier', async () => {
    const component = fixture.componentInstance;
    fixture.componentRef.setInput('image', photo());
    fixture.detectChanges();
    const changement = spyOn(component.imageChange, 'emit');
    const etats = spyOn(component.chargementChange, 'emit');
    const decodage = prochainDecodage();
    fixture.nativeElement.querySelector('.btn-outline-primary').click();
    await decodage;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.recadrage')).not.toBeNull();
    component.zoom = 3;
    component.annuler();
    fixture.detectChanges();
    expect(changement).not.toHaveBeenCalled();
    expect(etats).toHaveBeenCalledWith(false);
    expect(fixture.nativeElement.querySelector('.recadrage')).toBeNull();
    expect(fixture.nativeElement.querySelector('.photo-preview')).not.toBeNull();
  });

  it('déplace la photo au pointeur et limite le cadre aux bords de l’image', async () => {
    const component = fixture.componentInstance;
    fixture.componentRef.setInput('image', photo());
    const decodage = prochainDecodage();
    component.recadrer(); await decodage;
    fixture.detectChanges();
    component.zoom = 2;
    const cadre = fixture.nativeElement.querySelector('.recadrage') as HTMLElement;
    spyOn(cadre, 'setPointerCapture');
    spyOn(cadre, 'getBoundingClientRect').and.returnValue({ width: 400, height: 300 } as DOMRect);
    cadre.dispatchEvent(new PointerEvent('pointerdown', { pointerId: 1, isPrimary: true, button: 0, clientX: 200, clientY: 150 }));
    cadre.dispatchEvent(new PointerEvent('pointermove', { pointerId: 1, clientX: -1000, clientY: -1000 }));
    expect(component.horizontal).toBe(100);
    expect(component.vertical).toBe(100);
    cadre.dispatchEvent(new PointerEvent('pointerup', { pointerId: 1 }));
    cadre.dispatchEvent(new PointerEvent('pointermove', { pointerId: 1, clientX: 1000, clientY: 1000 }));
    expect(component.horizontal).toBe(100);
    expect(component.vertical).toBe(100);
  });

  it('refuse les formats non pris en charge et les fichiers de plus de 5 Mo', () => {
    const component = fixture.componentInstance;
    const changement = spyOn(component.imageChange, 'emit');
    choisir(new File(['<svg/>'], 'image.svg', { type: 'image/svg+xml' }));
    expect(component.erreur).toContain('JPEG ou PNG');
    choisir(new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'image.jpg', { type: 'image/jpeg' }));
    expect(component.erreur).toContain('5 Mo');
    expect(component.chargement).toBeFalse();
    expect(changement).not.toHaveBeenCalled();
  });

  it('signale une photo illisible sans modifier l’image enregistrée et libère le formulaire', async () => {
    const component = fixture.componentInstance;
    const changement = spyOn(component.imageChange, 'emit');
    const etats = spyOn(component.chargementChange, 'emit');
    const decodage = prochainDecodage();
    choisir(new File(['invalide'], 'photo.png', { type: 'image/png' }));
    await decodage;
    expect(component.erreur).toContain('illisible');
    expect(component.chargement).toBeFalse();
    expect(component.source).toBeUndefined();
    expect(changement).not.toHaveBeenCalled();
    expect(etats).toHaveBeenCalledWith(false);
  });

  it('affiche la photo existante et émet une suppression explicite', () => {
    fixture.componentRef.setInput('image', photo());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).not.toBeNull();
    const changement = spyOn(fixture.componentInstance.imageChange, 'emit');
    fixture.nativeElement.querySelector('.btn-outline-secondary').click();
    expect(changement).toHaveBeenCalledWith('');
  });
});
