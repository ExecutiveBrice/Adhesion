import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShopImageCropperComponent } from './shop-image-cropper.component';
import { SHOP_IMAGE_TARGET_BYTES } from './shop-image-optimization';

describe('ShopImageCropperComponent', () => {
  let fixture: ComponentFixture<ShopImageCropperComponent>;
  let decoded: Promise<void>;

  async function imageFile(width = 1200, height = 600): Promise<File> {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d')!;
    for (const [x, color] of [[0, '#f00'], [width / 3, '#0f0'], [width * 2 / 3, '#00f']] as const) {
      context.fillStyle = color;
      context.fillRect(x, 0, width / 3, height);
    }
    const blob = await new Promise<Blob>(resolve => canvas.toBlob(blob => resolve(blob!), 'image/png'));
    return new File([blob], 'produit.png', { type: 'image/png' });
  }

  async function open(file?: File): Promise<void> {
    const NativeImage = window.Image;
    decoded = new Promise(resolve => {
      spyOn(window, 'Image').and.callFake(function () {
        const image = new NativeImage();
        image.addEventListener('load', () => setTimeout(resolve, 0), { once: true });
        image.addEventListener('error', () => setTimeout(resolve, 0), { once: true });
        return image;
      });
    });
    TestBed.configureTestingModule({ imports: [ShopImageCropperComponent] });
    fixture = TestBed.createComponent(ShopImageCropperComponent);
    fixture.componentRef.setInput('file', file ?? await imageFile());
    fixture.detectChanges();
    await decoded;
    fixture.detectChanges();
  }

  it('ouvre une fenêtre modale et exporte uniquement la zone validée en JPEG 800 × 600', async () => {
    await open();
    const component = fixture.componentInstance;
    const emitted = spyOn(component.cropped, 'emit').and.callThrough();
    expect(fixture.nativeElement.querySelector('dialog').matches(':modal')).toBeTrue();
    expect(emitted).not.toHaveBeenCalled();
    const zoom = fixture.nativeElement.querySelector('#shop-crop-zoom') as HTMLInputElement;
    zoom.value = '2';
    zoom.dispatchEvent(new Event('input'));
    const horizontal = fixture.nativeElement.querySelector('#shop-crop-horizontal') as HTMLInputElement;
    horizontal.value = '100';
    horizontal.dispatchEvent(new Event('input'));
    const result = new Promise<File>(resolve => component.cropped.subscribe(resolve));
    component.confirm();
    const file = await result;
    expect(file.type).toBe('image/jpeg');
    expect(file.name).toBe('produit-recadree.jpg');
    expect(file.size).toBeLessThanOrEqual(SHOP_IMAGE_TARGET_BYTES);
    const image = document.createElement('img');
    const url = URL.createObjectURL(file);
    try {
      image.src = url;
      await image.decode();
      expect(image.naturalWidth).toBe(800);
      expect(image.naturalHeight).toBe(600);
      const canvas = document.createElement('canvas');
      canvas.width = 800;
      canvas.height = 600;
      const context = canvas.getContext('2d')!;
      context.drawImage(image, 0, 0);
      const pixel = context.getImageData(400, 300, 1, 1).data;
      expect(pixel[0]).toBeLessThan(10);
      expect(pixel[1]).toBeLessThan(10);
      expect(pixel[2]).toBeGreaterThan(245);
    } finally {
      URL.revokeObjectURL(url);
    }
  });

  it('limite le déplacement au pointeur aux bords de l’image', async () => {
    await open();
    const component = fixture.componentInstance;
    component.zoom = 2;
    const frame = fixture.nativeElement.querySelector('.crop-frame') as HTMLElement;
    spyOn(frame, 'setPointerCapture');
    spyOn(frame, 'getBoundingClientRect').and.returnValue({ width: 400, height: 300 } as DOMRect);
    frame.dispatchEvent(new PointerEvent('pointerdown', { pointerId: 1, isPrimary: true, button: 0, clientX: 200, clientY: 150 }));
    frame.dispatchEvent(new PointerEvent('pointermove', { pointerId: 1, clientX: -1000, clientY: -1000 }));
    expect(component.horizontal).toBe(100);
    expect(component.vertical).toBe(100);
    expect(component.crop.x + component.crop.width).toBeLessThanOrEqual(component.source!.naturalWidth);
    expect(component.crop.y + component.crop.height).toBeLessThanOrEqual(component.source!.naturalHeight);
    frame.dispatchEvent(new PointerEvent('pointerup', { pointerId: 1 }));
    frame.dispatchEvent(new PointerEvent('pointermove', { pointerId: 1, clientX: 1000, clientY: 1000 }));
    expect(component.horizontal).toBe(100);
  });

  for (const portrait of [false, true]) {
    it(`dézoome une image ${portrait ? 'portrait' : 'paysage'} entière et exporte les marges en blanc`, async () => {
      await open(await imageFile(portrait ? 600 : 1200, portrait ? 1200 : 600));
      const component = fixture.componentInstance;
      component.horizontal = 0;
      component.vertical = 100;
      const zoom = fixture.nativeElement.querySelector('#shop-crop-zoom') as HTMLInputElement;
      zoom.value = String((1 + component.minZoom) / 2);
      zoom.dispatchEvent(new Event('input'));
      expect(component.zoom).toBeLessThan(1);
      expect(component.zoom).toBeGreaterThan(component.minZoom);
      fixture.nativeElement.querySelector('.fit-image').click();
      fixture.detectChanges();
      expect(component.zoom).toBeLessThan(1);
      expect(component.horizontal).toBe(50);
      expect(component.vertical).toBe(50);
      expect(Number(zoom.min)).toBeCloseTo(portrait ? 0.375 : 2 / 3);
      expect(Number(zoom.value)).toBeCloseTo(component.minZoom);
      expect(component.crop.width).toBeGreaterThanOrEqual(component.source!.naturalWidth);
      expect(component.crop.height).toBeGreaterThanOrEqual(component.source!.naturalHeight);
      const result = new Promise<File>(resolve => component.cropped.subscribe(resolve));
      component.confirm();
      const url = URL.createObjectURL(await result);
      try {
        const image = document.createElement('img');
        image.src = url;
        await image.decode();
        const canvas = document.createElement('canvas');
        canvas.width = 800;
        canvas.height = 600;
        const context = canvas.getContext('2d')!;
        context.drawImage(image, 0, 0);
        for (const [x, y] of portrait ? [[100, 300], [700, 300]] : [[400, 50], [400, 550]]) {
          const pixel = context.getImageData(x, y, 1, 1).data;
          expect(Array.from(pixel).every(value => value > 245)).toBeTrue();
        }
        // Les deux extrémités de l’image sont présentes, ainsi que le centre.
        for (const [index, x] of (portrait ? [300, 400, 500] : [100, 400, 700]).entries()) {
          const pixel = context.getImageData(x, 300, 1, 1).data;
          expect(pixel[index]).toBeGreaterThan(245);
          expect(pixel[(index + 1) % 3]).toBeLessThan(10);
        }
      } finally {
        URL.revokeObjectURL(url);
      }
    });
  }

  it('permet de déplacer l’image dans les marges blanches après un dézoom', async () => {
    await open();
    const component = fixture.componentInstance;
    component.fitImage();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#shop-crop-horizontal').disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('#shop-crop-vertical').disabled).toBeFalse();
    const frame = fixture.nativeElement.querySelector('.crop-frame') as HTMLElement;
    spyOn(frame, 'setPointerCapture');
    spyOn(frame, 'getBoundingClientRect').and.returnValue({ width: 400, height: 300 } as DOMRect);
    frame.dispatchEvent(new PointerEvent('pointerdown', { pointerId: 1, isPrimary: true, button: 0, clientX: 200, clientY: 150 }));
    frame.dispatchEvent(new PointerEvent('pointermove', { pointerId: 1, clientX: 200, clientY: 200 }));
    expect(component.vertical).toBe(100);
    expect(component.horizontal).toBe(50);
    expect(component.crop.y).toBe(-300);
  });

  it('permet d’annuler avec Échap et libère l’URL temporaire', async () => {
    await open();
    const component = fixture.componentInstance;
    const cropped = spyOn(component.cropped, 'emit');
    const cancelled = spyOn(component.cancelled, 'emit');
    const revoke = spyOn(URL, 'revokeObjectURL');
    const url = component.source!.src;
    fixture.nativeElement.querySelector('dialog').dispatchEvent(new Event('cancel', { cancelable: true }));
    expect(cancelled).toHaveBeenCalled();
    expect(cropped).not.toHaveBeenCalled();
    fixture.destroy();
    expect(revoke).toHaveBeenCalledWith(url);
  });

  it('signale une image illisible et laisse la validation désactivée', async () => {
    await open(new File(['invalide'], 'produit.png', { type: 'image/png' }));
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('illisible');
    expect(fixture.nativeElement.querySelector('.btn-primary').disabled).toBeTrue();
    expect(fixture.componentInstance.loading).toBeFalse();
  });

  it('accepte les grandes dimensions sans limite de mégapixels', async () => {
    spyOnProperty(HTMLImageElement.prototype, 'naturalWidth', 'get').and.returnValue(10_000);
    spyOnProperty(HTMLImageElement.prototype, 'naturalHeight', 'get').and.returnValue(5_000);
    await open();
    expect(fixture.componentInstance.source).toBeDefined();
    expect(fixture.componentInstance.error).toBe('');
    expect(fixture.nativeElement.querySelector('.btn-primary').disabled).toBeFalse();
  });

  it('permet de réessayer si l’export échoue sans émettre de fichier', async () => {
    await open();
    const cropped = spyOn(fixture.componentInstance.cropped, 'emit');
    spyOn(HTMLCanvasElement.prototype, 'toBlob').and.callFake(callback => callback(null));
    await fixture.componentInstance.confirm();
    fixture.detectChanges();
    expect(cropped).not.toHaveBeenCalled();
    expect(fixture.componentInstance.exporting).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('n’a pas pu être appliqué');
    expect(fixture.nativeElement.querySelector('.btn-primary').disabled).toBeFalse();
  });

  it('n’applique pas une compression terminée après l’annulation du recadrage', async () => {
    await open();
    const cropped = spyOn(fixture.componentInstance.cropped, 'emit');
    let complete!: BlobCallback;
    spyOn(HTMLCanvasElement.prototype, 'toBlob').and.callFake(callback => complete = callback);
    const exporting = fixture.componentInstance.confirm();
    fixture.destroy();
    complete(new Blob(['image'], { type: 'image/jpeg' }));
    await exporting;
    expect(cropped).not.toHaveBeenCalled();
  });
});
