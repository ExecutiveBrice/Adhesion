import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShopProductDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { CartStore } from '../services/cart.store';
import { ToastService } from '../../_services/toast.service';
import { ShopProductDetailComponent } from './shop-product-detail.component';

describe('ShopProductDetailComponent gallery', () => {
  let fixture: ComponentFixture<ShopProductDetailComponent>;
  const product: ShopProductDto = {
    id: 7, name: 'T-shirt', slug: 't-shirt', description: null,
    imageUrl: '/main.jpg', additionalImageUrls: ['/front.jpg', '/back.jpg'],
    categories: [], variants: [
      { id: 1, sku: 'TS-M', label: 'M', price: { amountInCents: 1500, currency: 'EUR' }, available: true, imageUrl: '/detail.jpg' },
      { id: 2, sku: 'TS-L', label: 'L', price: { amountInCents: 1500, currency: 'EUR' }, available: true },
      { id: 3, sku: 'TS-S', label: 'S', price: { amountInCents: 1500, currency: 'EUR' }, available: true, imageUrl: '/small.jpg' }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ShopProductDetailComponent],
      providers: [provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '7']]) } } },
        { provide: ShopApiService, useValue: { product: () => of(product) } },
        { provide: CartStore, useValue: {} },
        { provide: ToastService, useValue: {} }
      ]
    });
    fixture = TestBed.createComponent(ShopProductDetailComponent);
    fixture.detectChanges();
  });

  it('sélectionne la principale et affiche les quatre miniatures', () => {
    expect(fixture.nativeElement.querySelector('.shop-product-visual img').getAttribute('src')).toBe('/main.jpg');
    const thumbnails = fixture.nativeElement.querySelectorAll('.shop-thumbnails button');
    expect(thumbnails.length).toBe(4);
    expect(thumbnails[0].getAttribute('aria-pressed')).toBe('true');
    thumbnails[2].click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shop-product-visual img').getAttribute('src')).toBe('/back.jpg');
    expect(thumbnails[2].getAttribute('aria-pressed')).toBe('true');
  });

  it('permet de défiler au clavier et conserve la sélection en plein écran', () => {
    (fixture.nativeElement.querySelector('.shop-gallery-stage .next') as HTMLButtonElement).click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.shop-product-image-button') as HTMLButtonElement).click();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    expect(dialog.matches(':modal')).toBeTrue();
    expect(dialog.querySelector('img')!.getAttribute('src')).toBe('/front.jpg');
    dialog.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(dialog.querySelector('img')!.getAttribute('src')).toBe('/back.jpg');
    (dialog.querySelector('.shop-image-close') as HTMLButtonElement).click();
    expect(dialog.open).toBeFalse();
    expect(fixture.nativeElement.querySelector('.shop-product-visual img').getAttribute('src')).toBe('/back.jpg');
  });

  it('boucle entre les images et distingue le balayage horizontal du défilement vertical', () => {
    const component = fixture.componentInstance;
    component.moveImage(-1);
    expect(component.selectedImage()).toBe('/detail.jpg');
    const touch = (x: number, y: number) => ({ clientX: x, clientY: y });
    component.startSwipe({ touches: [touch(150, 10)] } as unknown as TouchEvent);
    component.endSwipe({ changedTouches: [touch(50, 15)] } as unknown as TouchEvent);
    expect(component.selectedImage()).toBe('/main.jpg');
    component.startSwipe({ touches: [touch(150, 10)] } as unknown as TouchEvent);
    component.endSwipe({ changedTouches: [touch(90, 150)] } as unknown as TouchEvent);
    expect(component.selectedImage()).toBe('/main.jpg');
  });

  it('affiche la photo propre à la variante choisie et revient à la principale sans photo de variante', () => {
    fixture.componentInstance.selectVariant(3);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shop-product-visual img').getAttribute('src')).toBe('/small.jpg');
    expect(fixture.componentInstance.images()).toEqual(['/main.jpg', '/front.jpg', '/back.jpg', '/small.jpg']);
    fixture.componentInstance.selectVariant(2);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shop-product-visual img').getAttribute('src')).toBe('/main.jpg');
    expect(fixture.componentInstance.images()).toEqual(['/main.jpg', '/front.jpg', '/back.jpg']);
  });

  it('accepte les produits existants sans galerie et les produits sans image', () => {
    fixture.componentInstance.product.set({ ...product, additionalImageUrls: undefined });
    fixture.componentInstance.selectVariant(2);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.shop-thumbnails button').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.shop-gallery-arrow')).toBeNull();
    fixture.componentInstance.product.set({ ...product, imageUrl: null, additionalImageUrls: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.shop-product-image-button')).toBeNull();
    expect(fixture.nativeElement.querySelector('.shop-product-visual').textContent.trim()).toBe('T');
  });
});
