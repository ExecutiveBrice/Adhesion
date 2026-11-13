import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminProductDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopManagementComponent } from './shop-management.component';

describe('ShopManagementComponent', () => {
  beforeAll(() => registerLocaleData(localeFr));

  function imageSelection(file: File): Event {
    const input = document.createElement('input');
    input.type = 'file';
    const transfer = new DataTransfer();
    transfer.items.add(file);
    input.files = transfer.files;
    return { target: input } as unknown as Event;
  }

  for (const productId of [null, 7]) {
    it(`attend le recadrage avant d’importer l’image ${productId === null ? 'du nouveau produit' : 'du produit existant'}`, () => {
      const product: ShopAdminProductDto = {
        id: 7, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: '/ancienne.jpg',
        active: true, displayOrder: 0, categories: [], variants: []
      };
      const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
        ['products', 'categories', 'uploadProductImage', 'createProduct', 'updateProduct']);
      api.products.and.returnValue(of([product]));
      api.categories.and.returnValue(of([]));
      api.uploadProductImage.and.returnValue(of('/recadree.jpg'));
      api.createProduct.and.returnValue(of(product));
      api.updateProduct.and.returnValue(of(product));
      TestBed.configureTestingModule({
        imports: [ShopManagementComponent],
        providers: [{ provide: ShopAdminApiService, useValue: api }]
      });
      TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });
      const fixture = TestBed.createComponent(ShopManagementComponent);
      const component = fixture.componentInstance;
      const original = new File([new Uint8Array(5 * 1_024 * 1_024)], 'original.png', { type: 'image/png' });
      const cropped = new File(['recadree'], 'recadree.jpg', { type: 'image/jpeg' });
      const select = (file: File) => productId === null
        ? component.onNewProductImageSelected(imageSelection(file))
        : component.onProductImageSelected(productId, imageSelection(file));
      const save = () => productId === null ? component.createProduct() : component.updateProduct(product);
      select(original);
      expect(component.imageCrop).toEqual({ file: original, productId });
      expect(component.newProductImagePreview).toBeNull();
      expect(component.previewFor(product)).toBe('/ancienne.jpg');
      save();
      expect(api.uploadProductImage).not.toHaveBeenCalled();
      expect(api.createProduct).not.toHaveBeenCalled();
      expect(api.updateProduct).not.toHaveBeenCalled();
      component.applyImageCrop(cropped);
      const preview = productId === null ? component.newProductImagePreview : component.previewFor(product);
      expect(preview).toMatch(/^blob:/);
      expect(component.imageCrop).toBeNull();
      // Annuler un nouvel import conserve le dernier recadrage validé.
      select(original);
      component.imageCrop = null;
      expect(productId === null ? component.newProductImagePreview : component.previewFor(product)).toBe(preview);
      const revoke = spyOn(URL, 'revokeObjectURL');
      save();
      expect(api.uploadProductImage).toHaveBeenCalledOnceWith(cropped);
      const request = productId === null ? api.createProduct.calls.mostRecent().args[0] : api.updateProduct.calls.mostRecent().args[1];
      expect(request.imageUrl).toBe('/recadree.jpg');
      expect(revoke).toHaveBeenCalledWith(preview!);
      fixture.destroy();
    });
  }

  for (const productId of [null, 7]) {
    it(`enregistre les deux images supplémentaires après recadrage (${productId ?? 'nouveau'})`, () => {
      const product: ShopAdminProductDto = {
        id: 7, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: '/main.jpg',
        additionalImageUrls: ['/old.jpg', '/keep.jpg'], active: true, displayOrder: 0, categories: [], variants: []
      };
      const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
        ['products', 'categories', 'uploadProductImage', 'createProduct', 'updateProduct']);
      api.products.and.returnValue(of([product]));
      api.categories.and.returnValue(of([]));
      api.uploadProductImage.and.callFake(file => of('/' + file.name));
      api.createProduct.and.returnValue(of(product));
      api.updateProduct.and.returnValue(of(product));
      TestBed.configureTestingModule({ imports: [ShopManagementComponent],
        providers: [{ provide: ShopAdminApiService, useValue: api }] });
      TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });
      const fixture = TestBed.createComponent(ShopManagementComponent);
      const component = fixture.componentInstance;
      const target = productId === null ? null : product;
      for (const index of [0, 1]) {
        const file = new File(['image'], `photo${index}.jpg`, { type: 'image/jpeg' });
        component.onAdditionalImageSelected(productId, index, imageSelection(file));
        expect(component.imageCrop?.additionalIndex).toBe(index);
        component.applyImageCrop(file);
        expect(component.additionalImagePreview(target, index)).toMatch(/^blob:/);
      }
      // Retirer un emplacement ne doit pas décaler les autres imports en attente.
      component.removeAdditionalImage(target, 0);
      const revoke = spyOn(URL, 'revokeObjectURL');
      productId === null ? component.createProduct() : component.updateProduct(product);
      const request = productId === null ? api.createProduct.calls.mostRecent().args[0] : api.updateProduct.calls.mostRecent().args[1];
      expect(request.additionalImageUrls).toEqual(['/photo1.jpg']);
      expect(request.imageUrl).toBe(productId === null ? null : '/main.jpg');
      expect(api.uploadProductImage).toHaveBeenCalledTimes(1);
      expect(revoke).toHaveBeenCalledTimes(1);
      fixture.destroy();
    });
  }

  for (const existing of [false, true]) {
    it(`importe une photo propre à une variante ${existing ? 'existante' : 'nouvelle'} et permet de la retirer`, () => {
      const variant = { id: 11, sku: 'TS-M', label: 'M', imageUrl: '/old-variant.jpg', priceAmountInCents: 1500,
        currency: 'EUR', active: true, displayOrder: 0, stockTracked: true, stockOnHand: 0, stockReserved: 0, version: 1 };
      const product: ShopAdminProductDto = {
        id: 7, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: '/main.jpg',
        active: true, displayOrder: 0, categories: [], variants: [variant]
      };
      const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
        ['products', 'categories', 'uploadProductImage', 'createVariant', 'updateVariant']);
      api.products.and.returnValue(of([product]));
      api.categories.and.returnValue(of([]));
      api.uploadProductImage.and.returnValue(of('/variant.jpg'));
      api.createVariant.and.returnValue(of(variant));
      api.updateVariant.and.returnValue(of(variant));
      TestBed.configureTestingModule({ imports: [ShopManagementComponent],
        providers: [{ provide: ShopAdminApiService, useValue: api }] });
      TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });
      const fixture = TestBed.createComponent(ShopManagementComponent);
      const component = fixture.componentInstance;
      const key = existing ? component.variantImageKey(variant.id) : component.newVariantImageKey(product.id);
      const target = existing ? variant : component.draftFor(product.id);
      const file = new File(['photo'], 'variant.jpg', { type: 'image/jpeg' });
      const save = () => existing ? component.updateVariant(variant) : component.createVariant(product);
      component.onVariantImageSelected(key, imageSelection(file));
      save();
      expect(api.uploadProductImage).not.toHaveBeenCalled();
      component.applyImageCrop(file);
      expect(component.variantImagePreview(key, target)).toMatch(/^blob:/);
      const revoke = spyOn(URL, 'revokeObjectURL');
      save();
      const request = existing ? api.updateVariant.calls.mostRecent().args[1] : api.createVariant.calls.mostRecent().args[1];
      expect(request.imageUrl).toBe('/variant.jpg');
      expect(revoke).toHaveBeenCalledTimes(1);
      expect(component.previewFor(product)).toBe('/main.jpg');
      const removalTarget = existing ? variant : component.draftFor(product.id);
      component.removeVariantImage(key, removalTarget);
      save();
      const removal = existing ? api.updateVariant.calls.mostRecent().args[1] : api.createVariant.calls.mostRecent().args[1];
      expect(removal.imageUrl).toBeNull();
      expect(api.uploadProductImage).toHaveBeenCalledTimes(1);
      fixture.destroy();
    });
  }

  it('refuse les formats invalides et accepte les images volumineuses avant le recadrage', () => {
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [{ provide: ShopAdminApiService, useValue: {} }]
    });
    TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    const component = fixture.componentInstance;
    component.onNewProductImageSelected(imageSelection(new File(['<svg/>'], 'image.svg', { type: 'image/svg+xml' })));
    expect(component.imageUploadError).toContain('PNG, JPEG, GIF ou WebP');
    expect(component.imageCrop).toBeNull();
    const file = new File([new Uint8Array(5 * 1_024 * 1_024)], 'image.png', { type: 'image/png' });
    component.onProductImageSelected(7, imageSelection(file));
    expect(component.imageUploadError).toBe('');
    expect(component.imageCrop).toEqual({ file, productId: 7 });
    fixture.destroy();
  });

  it('ouvre le tableau de stock et charge le catalogue à la demande', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'categories']);
    api.products.and.returnValue(of([]));
    api.orders.and.returnValue(of([]));
    api.categories.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [provideRouter([]), { provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#stocks-tab')?.getAttribute('aria-selected')).toBe('true');
    expect(fixture.nativeElement.querySelector('#stock-overview-title')?.textContent).toContain('État des stocks');
    expect(api.products).toHaveBeenCalledTimes(1);
    expect(api.categories).not.toHaveBeenCalled();

    (fixture.nativeElement.querySelector('#catalogue-tab') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#catalogue-tab')?.getAttribute('aria-selected')).toBe('true');
    expect(api.categories).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('ouvre le recadrage depuis le formulaire et ferme la fenêtre à l’annulation', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'categories']);
    api.products.and.returnValue(of([]));
    api.orders.and.returnValue(of([]));
    api.categories.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [provideRouter([]), { provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.selectTab('catalogue');
    component.newProduct.name = 'T-shirt';
    component.newProduct.slug = 't-shirt';
    fixture.detectChanges();
    const file = new File(['original'], 'original.png', { type: 'image/png' });
    const transfer = new DataTransfer();
    transfer.items.add(file);
    const input = fixture.nativeElement.querySelector('#newProductImage') as HTMLInputElement;
    input.files = transfer.files;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    expect(dialog.matches(':modal')).toBeTrue();
    expect(input.disabled).toBeTrue();
    expect(input.value).toBe('');
    expect(component.imageCrop?.file).toBe(file);
    (dialog.querySelector('.btn-outline-secondary') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(component.imageCrop).toBeNull();
    expect(fixture.nativeElement.querySelector('dialog')).toBeNull();
    expect(input.disabled).toBeFalse();
    expect(component.newProductImagePreview).toBeNull();
    fixture.destroy();
  });

  it('crée une variante sans choix de quantité ni de suivi du stock', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 41, sku: 'TS-S', label: 'S', priceAmountInCents: 1500, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 0, stockReserved: 0, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
      ['products', 'orders', 'categories', 'createVariant']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(of([]));
    api.categories.and.returnValue(of([]));
    api.createVariant.and.returnValue(of({
      id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1500, currency: 'EUR',
      active: true, displayOrder: 0, stockTracked: true, stockOnHand: 0, stockReserved: 0, version: 0
    }));
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [provideRouter([]), { provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.detectChanges();
    fixture.componentInstance.selectTab('catalogue');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('.new-variant') as HTMLFormElement;
    expect(form.querySelector('input[name^="newVariantStock"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name^="variantStock"]')).toBeNull();
    expect(form.textContent).toContain('Stock initial : 0');
    Object.assign(fixture.componentInstance.draftFor(product.id), {
      sku: ' TS-M ', label: ' M ', priceAmountInCents: 1500
    });
    fixture.componentInstance.createVariant(product);

    expect(api.createVariant).toHaveBeenCalledWith(1, {
      sku: 'TS-M', label: 'M', priceAmountInCents: 1500,
      currency: 'EUR', active: true, displayOrder: 0
    });
    fixture.destroy();
  });

  it('sépare les commandes actives des commandes clôturées', () => {
    const statuses: ShopAdminOrderDto['status'][] = [
      'DRAFT', 'PENDING_PAYMENT', 'PAID', 'PROCESSING',
      'COMPLETED', 'CANCELLED', 'EXPIRED', 'REFUNDED'
    ];
    const orders = statuses.map((status, index) => ({
      orderNumber: `CMD-${index}`, status,
      total: { amountInCents: 1000, currency: 'EUR' }, createdAt: '2026-09-25T10:00:00Z',
      customerEmail: 'client@example.test', customerTribeId: 17, refundRequested: false, items: []
    }));
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['orders', 'products']);
    api.orders.and.returnValue(of(orders));
    api.products.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });

    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.componentInstance.selectTab('orders');

    expect(fixture.componentInstance.ordersInProgress.map(order => order.status))
      .toEqual(statuses.slice(0, 4));
    expect(fixture.componentInstance.completedOrders.map(order => order.status))
      .toEqual(statuses.slice(4));
    fixture.componentInstance.ordersInProgress[3].status = 'COMPLETED';
    fixture.componentInstance.reclassifyOrders();
    expect(fixture.componentInstance.ordersInProgress.length).toBe(3);
    expect(fixture.componentInstance.completedOrders.length).toBe(5);
    expect(api.orders).toHaveBeenCalledTimes(1);
    fixture.componentInstance.selectTab('stocks');
    fixture.componentInstance.selectTab('orders');
    expect(api.orders).toHaveBeenCalledTimes(2);

    fixture.destroy();
  });
});
