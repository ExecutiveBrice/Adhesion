import { Component, computed, inject, signal } from '@angular/core';
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
  readonly imageIndex = signal(0);
  readonly productImages = computed(() => {
    const product = this.product();
    return [product?.imageUrl, ...(product?.additionalImageUrls ?? []).slice(0, 2)]
      .filter((url): url is string => !!url);
  });
  readonly images = computed(() => {
    const images = this.productImages();
    const variantImage = this.variant()?.imageUrl;
    return variantImage && !images.includes(variantImage) ? [...images, variantImage] : images;
  });
  readonly selectedImage = computed(() => this.images()[this.imageIndex()] ?? this.images()[0]);
  private touchStart: { x: number; y: number } | null = null;

  selectImage(index: number): void {
    if (index >= 0 && index < this.images().length) this.imageIndex.set(index);
  }

  moveImage(offset: number): void {
    const count = this.images().length;
    if (count > 1) this.imageIndex.set((this.imageIndex() + offset + count) % count);
  }

  startSwipe(event: TouchEvent): void {
    this.touchStart = event.touches.length === 1
      ? { x: event.touches[0].clientX, y: event.touches[0].clientY } : null;
  }

  cancelSwipe(): void { this.touchStart = null; }

  endSwipe(event: TouchEvent): void {
    const start = this.touchStart;
    this.touchStart = null;
    const end = event.changedTouches[0];
    if (!start || !end) return;
    const dx = end.clientX - start.x;
    if (Math.abs(dx) >= 50 && Math.abs(dx) > Math.abs(end.clientY - start.y)) {
      if (event.cancelable) event.preventDefault();
      this.moveImage(dx < 0 ? 1 : -1);
    }
  }

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

  selectVariant(variantId: number | string): void {
    this.variant.set(this.product()?.variants.find(value => value.id === Number(variantId)) ?? null);
    const image = this.variant()?.imageUrl;
    this.imageIndex.set(image ? Math.max(0, this.images().indexOf(image)) : 0);
  }
  add(): void { const product = this.product(); const variant = this.variant(); if (!product || !variant || !variant.available) return; this.cart.add(variant.id); this.toast.success(`${product.name} a été ajouté au panier.`, 'Panier mis à jour'); }
}
