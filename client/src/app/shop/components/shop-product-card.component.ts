import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShopProductDto, ShopVariantDto } from '../models/shop.models';
import { formatShopArticleMoney } from '../shop-format';

@Component({
  selector: 'app-shop-product-card',
  imports: [RouterLink, FormsModule],
  templateUrl: './shop-product-card.component.html',
  styles: [`.shop-product-card{background:#fff;border:1px solid #dce5e3;border-radius:.75rem;box-shadow:0 .25rem .75rem rgba(0,0,0,.08);color:inherit;display:flex;flex-direction:column;height:100%;overflow:hidden;text-decoration:none;transition:box-shadow .15s ease,transform .15s ease}.shop-product-card:hover{box-shadow:0 .5rem 1rem rgba(0,0,0,.12);transform:translateY(-2px)}.shop-product-card:focus-visible{outline:3px solid #17633f;outline-offset:2px}.shop-product-image{align-items:center;background:linear-gradient(135deg,#e7f4f1,#bcded8);color:#17564e;display:flex;font-size:3rem;font-weight:800;justify-content:center;min-height:9rem}.shop-product-image img{height:12rem;object-fit:cover;width:100%}.shop-product-content{display:flex;flex:1;flex-direction:column;gap:.75rem;padding:1rem}.shop-product-content h2{color:#1d2d2a;font-size:1.15rem;margin:0}.shop-product-content p{color:#4d5a57;font-size:.92rem;margin:0}.shop-category-list{display:flex;flex-wrap:wrap;gap:.35rem}.shop-category-list span{background:#edf6f4;border-radius:999px;color:#17564e;font-size:.75rem;padding:.2rem .5rem}.shop-card-footer{align-items:center;display:flex;justify-content:space-between;margin-top:auto}.shop-card-footer strong{font-size:1.15rem}.shop-availability{color:#17633f;font-size:.84rem;font-weight:600}.shop-availability.unavailable{color:#9b2c2c}.form-select{font-size:.9rem}`]
})
export class ShopProductCardComponent {
  @Input({ required: true }) product!: ShopProductDto;
  @Output() readonly add = new EventEmitter<ShopVariantDto>();
  readonly variant = signal<ShopVariantDto | null>(null);
  readonly money = formatShopArticleMoney;

  priceRange(): string {
    const prices = this.product.variants.map(variant => variant.price)
      .sort((left, right) => left.amountInCents - right.amountInCents);
    if (prices.length === 0) return '';

    const lowest = prices[0];
    const highest = prices[prices.length - 1];
    return lowest.amountInCents === highest.amountInCents && lowest.currency === highest.currency
      ? this.money(lowest)
      : `${this.money(lowest)} – ${this.money(highest)}`;
  }

  availabilityLabel(variant: ShopVariantDto): string {
    return variant.available ? 'Disponible' : 'Indisponible';
  }

  ngOnChanges(): void {
    this.variant.set(this.product.variants.find(variant => variant.available) ?? this.product.variants[0] ?? null);
  }

  selectVariant(variantId: number | string): void {
    this.variant.set(this.product.variants.find(variant => variant.id === Number(variantId)) ?? null);
  }
}
