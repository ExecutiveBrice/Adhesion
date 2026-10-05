import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PublicitesComponent } from './publicites.component';
import { environment } from '../../../environments/environment';
import { ImageCroppedEvent, ImageCropperComponent } from 'ngx-image-cropper';

describe('Gestion des publicités', () => {
  let component: PublicitesComponent;
  let http: HttpTestingController;
  const url = environment.server + '/publicites';
  const sectionsUrl = environment.server + '/param/sections';
  const sections = [
    { id: 2, nom: 'Basket', type: 'NON_COMPETITIVE' as const },
    { id: 4, nom: 'Danse', type: 'NON_COMPETITIVE' as const }
  ];
  const item = { id: 7, description: 'Portes ouvertes', sectionIds: [], displayOrder: 2, active: true };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    component = TestBed.runInInjectionContext(() => new PublicitesComponent());
    http = TestBed.inject(HttpTestingController);
    component.ngOnInit();
    http.expectOne(url).flush([item]);
    http.expectOne(sectionsUrl).flush(sections);
  });

  afterEach(() => { component.ngOnDestroy(); http.verify(); });

  async function selectImage(width: number, height: number): Promise<File> {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const blob = await new Promise<Blob>(resolve => canvas.toBlob(value => resolve(value!), 'image/png'));
    const file = new File([blob], 'affiche.png', { type: 'image/png' });
    await component.selectFile({ target: { files: [file], value: 'affiche.png' } } as unknown as Event);
    return file;
  }

  async function prepareCrop(width = 160, height = 90): Promise<File> {
    const source = await selectImage(width, height);
    await component.imageCropped({ blob: source, width: 1900, height: 900 } as unknown as ImageCroppedEvent);
    return component.file!;
  }

  it('accepte une source 16/9 et prépare un fichier JPEG exactement au format 19/9', async () => {
    const source = await selectImage(160, 90);
    expect(component.sourceFile()).toBe(source);
    expect(component.file).toBeUndefined();
    expect(component.preview()).toBe('');
    const file = await prepareCrop();
    expect(file.type).toBe('image/jpeg');
    expect(file.size).toBeLessThan(1024 * 1024);
    const bitmap = await createImageBitmap(file);
    expect([bitmap.width, bitmap.height]).toEqual([1900, 900]);
    bitmap.close();
    expect(component.preview()).toContain('blob:');
    expect(component.error()).toBe('');
  });

  it('affiche un cadre 19/9 déplaçable avec un zoom réglable', async () => {
    const fixture = TestBed.createComponent(PublicitesComponent);
    fixture.detectChanges();
    http.expectOne(url).flush([]);
    http.expectOne(sectionsUrl).flush(sections);
    const canvas = document.createElement('canvas');
    canvas.width = 160;
    canvas.height = 90;
    const blob = await new Promise<Blob>(resolve => canvas.toBlob(value => resolve(value!), 'image/png'));
    fixture.componentInstance.sourceFile.set(new File([blob], 'source.png', { type: 'image/png' }));
    fixture.detectChanges();
    const cropper = fixture.debugElement.queryAll(node => node.componentInstance instanceof ImageCropperComponent)[0].componentInstance as ImageCropperComponent;
    expect(cropper.aspectRatio).toBe(19 / 9);
    expect(cropper.allowMoveImage).toBeTrue();
    fixture.componentInstance.setZoom('1.5');
    expect(fixture.componentInstance.transform.scale).toBe(1.5);
    fixture.destroy();
  });

  it('publie au clic même si le fichier recadré n’a pas été préparé en arrière-plan', async () => {
    const fixture = TestBed.createComponent(PublicitesComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    http.expectOne(url).flush({}, { status: 500, statusText: 'Erreur' });
    http.expectOne(sectionsUrl).flush(sections);
    const canvas = document.createElement('canvas');
    canvas.width = 160;
    canvas.height = 90;
    const blob = await new Promise<Blob>(resolve => canvas.toBlob(value => resolve(value!), 'image/png'));
    const file = new File([blob], 'source.png', { type: 'image/png' });
    const input = fixture.nativeElement.querySelector('#publicite-file') as HTMLInputElement;
    Object.defineProperty(input, 'files', { configurable: true, value: [file] });
    await fixture.componentInstance.selectFile({ target: input } as unknown as Event);
    for (let attempt = 0; attempt < 30 && !fixture.componentInstance.file && !fixture.componentInstance.error(); attempt++) {
      fixture.detectChanges();
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBe('');
    expect(fixture.componentInstance.file).toBeDefined();
    fixture.componentInstance.file = undefined;
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(button.disabled).toBeFalse();
    button.click();
    expect(fixture.componentInstance.error()).toContain('description');
    http.expectNone(url);
    const description = fixture.nativeElement.querySelector('#publicite-description') as HTMLInputElement;
    description.value = 'Publicité de test';
    description.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    button.click();
    let request = http.match(url)[0];
    for (let attempt = 0; attempt < 30 && !request; attempt++) {
      await new Promise(resolve => setTimeout(resolve, 100));
      request = http.match(url)[0];
    }
    expect(request).toBeDefined();
    expect(request.request.method).toBe('POST');
    request.flush({ id: 8, description: 'Publicité de test', sectionIds: [], displayOrder: 0, active: true });
    expect(fixture.componentInstance.success()).toContain('enregistrée');
    fixture.destroy();
    fixture.nativeElement.remove();
  });

  it('publie le fichier et nettoie le formulaire seulement après succès', async () => {
    const file = await prepareCrop();
    component.description = '  Nouvelle publicité  ';
    component.facebookUrl = ' https://facebook.com/alod ';
    component.snapchatUrl = 'https://snapchat.com/add/alod';
    component.websiteUrl = 'https://alod.fr';
    component.selectedSectionIds = [2, 4];
    component.displayOrder = 6;
    component.disabled = true;
    const input = { value: 'affiche.png' } as HTMLInputElement;
    component.publish(input);
    expect(component.saving()).toBeTrue();
    const request = http.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('description')).toBe('Nouvelle publicité');
    expect(request.request.body.get('file')).toBe(file);
    expect(request.request.body.get('facebookUrl')).toBe('https://facebook.com/alod');
    expect(request.request.body.get('snapchatUrl')).toBe('https://snapchat.com/add/alod');
    expect(request.request.body.get('websiteUrl')).toBe('https://alod.fr');
    expect(request.request.body.getAll('sectionIds')).toEqual(['2', '4']);
    expect(request.request.body.get('displayOrder')).toBe('6');
    expect(request.request.body.get('active')).toBe('false');
    request.flush({ id: 8, description: 'Nouvelle publicité', facebookUrl: 'https://facebook.com/alod', sectionIds: [2, 4], displayOrder: 6, active: false });
    expect(component.publicites().map(value => value.id)).toEqual([8, 7]);
    expect(component.preview()).toBe('');
    expect(component.file).toBeUndefined();
    expect(component.sourceFile()).toBeUndefined();
    expect(component.facebookUrl).toBe('');
    expect(component.snapchatUrl).toBe('');
    expect(component.websiteUrl).toBe('');
    expect(component.selectedSectionIds).toEqual([]);
    expect(component.displayOrder).toBe(0);
    expect(component.disabled).toBeFalse();
    expect(input.value).toBe('');
    expect(component.saving()).toBeFalse();
  });

  it('conserve le brouillon et la liste lorsque la publication échoue', async () => {
    await prepareCrop();
    component.description = 'À conserver';
    component.publish({ value: 'affiche.png' } as HTMLInputElement);
    http.expectOne(url).flush({}, { status: 500, statusText: 'Erreur' });
    expect(component.description).toBe('À conserver');
    expect(component.file).toBeDefined();
    expect(component.publicites()).toEqual([item]);
    expect(component.saving()).toBeFalse();
    expect(component.error()).toContain('échoué');
  });

  it('retire un visuel uniquement après confirmation du serveur', () => {
    component.remove(item);
    expect(component.publicites()).toEqual([item]);
    http.expectOne(url + '/7').flush({}, { status: 500, statusText: 'Erreur' });
    expect(component.publicites()).toEqual([item]);
    component.remove(item);
    http.expectOne(url + '/7').flush(null);
    expect(component.publicites()).toEqual([]);
  });

  it('charge puis modifie une publicité sans imposer le remplacement de l’image', () => {
    const input = { value: 'ancienne-image.png' } as HTMLInputElement;
    component.startEdit(item, input);
    expect(component.editingId()).toBe(7);
    expect(component.description).toBe('Portes ouvertes');
    expect(component.sourceFile()).toBeUndefined();
    component.description = 'Portes ouvertes modifiées';
    component.displayOrder = 4;
    component.disabled = true;
    component.publish(input);
    const request = http.expectOne(url + '/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.get('description')).toBe('Portes ouvertes modifiées');
    expect(request.request.body.get('displayOrder')).toBe('4');
    expect(request.request.body.get('active')).toBe('false');
    expect(request.request.body.get('file')).toBeNull();
    request.flush({ ...item, description: 'Portes ouvertes modifiées', displayOrder: 4, active: false });
    expect(component.editingId()).toBeNull();
    expect(component.success()).toContain('modifiée');
  });
});
