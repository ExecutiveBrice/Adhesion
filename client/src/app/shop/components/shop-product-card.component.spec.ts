import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShopProductCardComponent } from './shop-product-card.component';
import { ShopProductDto } from '../models/shop.models';

describe('ShopProductCardComponent', () => {
  let fixture: ComponentFixture<ShopProductCardComponent>;
  const product: ShopProductDto = { id: 1, name: 'T-shirt ALOD', slug: 't-shirt', description: 'Coton', imageUrl: null, categories: [], variants: [
    { id: 4, sku: 'TS-M', label: 'M', price: { amountInCents: 1500, currency: 'EUR' }, available: true, availableQuantity: 3 }
  ]};

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ShopProductCardComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(ShopProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
  });

  it('affiche le prix et la quantité disponible', () => {
    expect(fixture.nativeElement.textContent).toContain('15 €');
    expect(fixture.nativeElement.textContent).toContain('3 disponibles');
  });

  it('utilise un unique lien qui recouvre toute la carte', () => {
    const cardLink = fixture.nativeElement.querySelector('a.shop-product-card');

    expect(cardLink).toBeTruthy();
    expect(cardLink.getAttribute('href')).toBe('/boutique/produits/1');
    expect(fixture.nativeElement.querySelectorAll('a')).toHaveSize(1);
  });

  it('affiche les bornes de prix lorsque les variantes ont des prix différents', () => {
    fixture.componentRef.setInput('product', {
      ...product,
      variants: [...product.variants, { id: 5, sku: 'TS-L', label: 'L', price: { amountInCents: 2000, currency: 'EUR' }, available: true, availableQuantity: 1 }]
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('15 € – 20 €');
  });

  it('affiche les centimes lorsqu’un prix en comporte', () => {
    fixture.componentRef.setInput('product', {
      ...product,
      variants: [{ ...product.variants[0], price: { amountInCents: 1550, currency: 'EUR' } }]
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('15,50 €');
  });
});
