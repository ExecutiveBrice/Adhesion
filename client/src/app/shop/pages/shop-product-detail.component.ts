import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ShopProductDto, ShopVariantDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { CartStore } from '../services/cart.store';
import { ToastService } from '../../_services/toast.service';
import { formatShopMoney } from '../shop-format';

@Component({
  imports: [RouterLink, FormsModule],
  templateUrl: './shop-product-detail.component.html',
  styleUrl: './shop-product-detail.component.css'
})
export class ShopProductDetailComponent {
  private readonly api = inject(ShopApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly cart = inject(CartStore);
  private readonly toast = inject(ToastService);
  readonly product = signal<ShopProductDto | null>(null);
  readonly variant = signal<ShopVariantDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly money = formatShopMoney;

  priceRange(product: ShopProductDto): string {
    const prices = product.variants.map(variant => variant.price)
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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) { this.error.set('Produit introuvable.'); this.loading.set(false); return; }
    this.api.product(id).subscribe({
      next: product => { this.product.set(product); this.variant.set(product.variants.find(value => value.available) ?? product.variants[0] ?? null); this.loading.set(false); },
      error: () => { this.error.set('Ce produit n’est plus disponible.'); this.loading.set(false); }
    });
  }

  selectVariant(variantId: number | string): void { this.variant.set(this.product()?.variants.find(value => value.id === Number(variantId)) ?? null); }
  add(): void { const product = this.product(); const variant = this.variant(); if (!product || !variant || !variant.available) return; this.cart.add(variant.id); this.toast.success(`${product.name} a été ajouté au panier.`, 'Panier mis à jour'); }
}
