import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ShopProductCardComponent } from '../components/shop-product-card.component';
import { ShopCartLinkComponent } from '../components/shop-cart-link.component';
import { ShopProductDto, ShopVariantDto } from '../models/shop.models';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';
import { ToastService } from '../../_services/toast.service';

@Component({
  imports: [FormsModule, RouterLink, ShopProductCardComponent, ShopCartLinkComponent],
  templateUrl: './shop-catalog.component.html',
  styleUrl: './shop-catalog.component.css'
})
export class ShopCatalogComponent {
  private readonly api = inject(ShopApiService);
  private readonly cart = inject(CartStore);
  private readonly toast = inject(ToastService);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly products = signal<ShopProductDto[]>([]);
  readonly search = signal('');
  readonly category = signal('');
  readonly categories = computed(() => [...new Map(this.products().flatMap(product => product.categories).map(value => [value.slug, value])).values()]);
  readonly filteredProducts = computed(() => {
    const words = this.search().trim().toLocaleLowerCase('fr');
    return this.products().filter(product => (!this.category() || product.categories.some(category => category.slug === this.category()))
      && (!words || `${product.name} ${product.description ?? ''}`.toLocaleLowerCase('fr').includes(words)));
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true); this.error.set(null);
    this.api.products().subscribe({
      next: products => { this.products.set(products); this.loading.set(false); },
      error: () => { this.error.set('Le catalogue est indisponible pour le moment.'); this.loading.set(false); }
    });
  }

  add(product: ShopProductDto, variant: ShopVariantDto): void {
    this.cart.add(variant.id);
    this.toast.success(`${product.name} a été ajouté au panier.`, 'Panier mis à jour');
  }
}
