import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShopProductCardComponent } from './shop-product-card.component';
import { ShopProductDto } from '../models/shop.models';

describe('ShopProductCardComponent', () => {
  let fixture: ComponentFixture<ShopProductCardComponent>;
  const product: ShopProductDto = { id: 1, name: 'T-shirt ALOD', slug: 't-shirt', description: 'Coton', categories: [], variants: [
    { id: 4, sku: 'TS-M', label: 'M', price: { amountInCents: 1500, currency: 'EUR' }, available: true }
  ]};

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ShopProductCardComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(ShopProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
  });

  it('annonce clairement le prix et ajoute uniquement une variante disponible', () => {
    const add = jasmine.createSpy('add');
    fixture.componentInstance.add.subscribe(add);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(fixture.nativeElement.textContent).toContain('15,00');
    expect(button.disabled).toBeFalse();
    button.click();
    expect(add).toHaveBeenCalledWith(product.variants[0]);
  });
});
