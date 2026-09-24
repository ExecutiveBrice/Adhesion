import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShopProductDto, ShopVariantDto } from '../models/shop.models';
import { formatShopMoney } from '../shop-format';

@Component({
  selector: 'app-shop-product-card',
  imports: [RouterLink, FormsModule],
  template: `<article class="shop-product-card">
    <a class="shop-product-image" [routerLink]="['/boutique/produits', product.id]" [attr.aria-label]="'Voir ' + product.name">
      <span aria-hidden="true">{{ product.name.slice(0, 1) }}</span><span class="visually-hidden">Illustration à venir pour {{ product.name }}</span>
    </a>
    <div class="shop-product-content">
      <div class="shop-category-list">@for (category of product.categories; track category.id) { <span>{{ category.name }}</span> }</div>
      <h2><a [routerLink]="['/boutique/produits', product.id]">{{ product.name }}</a></h2>
      <p>{{ product.description || 'Découvrez ce produit de la boutique ALOD.' }}</p>
      @if (variant()) {
        @if (product.variants.length > 1) { <label class="visually-hidden" [for]="'variant-' + product.id">Variante</label><select [id]="'variant-' + product.id" class="form-select form-select-sm" [ngModel]="variant()?.id" (ngModelChange)="selectVariant($event)">@for (choice of product.variants; track choice.id) { <option [value]="choice.id" [disabled]="!choice.available">{{ choice.label }} — {{ money(choice.price) }}{{ choice.available ? '' : ' (indisponible)' }}</option> }</select> }
        <div class="shop-card-footer"><strong>{{ money(variant()!.price) }}</strong><span class="shop-availability" [class.unavailable]="!variant()!.available">{{ variant()!.available ? 'Disponible' : 'Indisponible' }}</span></div>
        <button type="button" class="btn btn-primary w-100" [disabled]="!variant()!.available" (click)="add.emit(variant()!)">Ajouter au panier</button>
      } @else { <p class="shop-availability unavailable">Indisponible</p> }
    </div>
  </article>`,
  styles: [`.shop-product-card{background:#fff;border:1px solid #dce5e3;border-radius:.75rem;box-shadow:0 .25rem .75rem rgba(0,0,0,.08);display:flex;flex-direction:column;height:100%;overflow:hidden}.shop-product-image{align-items:center;background:linear-gradient(135deg,#e7f4f1,#bcded8);color:#17564e;display:flex;font-size:3rem;font-weight:800;justify-content:center;min-height:9rem;text-decoration:none}.shop-product-content{display:flex;flex:1;flex-direction:column;gap:.75rem;padding:1rem}.shop-product-content h2{font-size:1.15rem;margin:0}.shop-product-content h2 a{color:#1d2d2a;text-decoration:none}.shop-product-content h2 a:hover{text-decoration:underline}.shop-product-content p{color:#4d5a57;font-size:.92rem;margin:0}.shop-category-list{display:flex;flex-wrap:wrap;gap:.35rem}.shop-category-list span{background:#edf6f4;border-radius:999px;color:#17564e;font-size:.75rem;padding:.2rem .5rem}.shop-card-footer{align-items:center;display:flex;justify-content:space-between;margin-top:auto}.shop-card-footer strong{font-size:1.15rem}.shop-availability{color:#17633f;font-size:.84rem;font-weight:600}.shop-availability.unavailable{color:#9b2c2c}.form-select{font-size:.9rem}`]
})
export class ShopProductCardComponent {
  @Input({ required: true }) product!: ShopProductDto;
  @Output() readonly add = new EventEmitter<ShopVariantDto>();
  readonly variant = signal<ShopVariantDto | null>(null);
  readonly money = formatShopMoney;

  ngOnChanges(): void {
    this.variant.set(this.product.variants.find(variant => variant.available) ?? this.product.variants[0] ?? null);
  }

  selectVariant(variantId: number | string): void {
    this.variant.set(this.product.variants.find(variant => variant.id === Number(variantId)) ?? null);
  }
}
