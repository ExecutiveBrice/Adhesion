import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCartShopping } from '@fortawesome/free-solid-svg-icons';
import { CartStore } from '../services/cart.store';

@Component({
  selector: 'app-shop-cart-link',
  imports: [RouterLink, FaIconComponent],
  template: `<a class="shop-cart-link" routerLink="/boutique/panier" aria-label="Ouvrir le panier">
    <fa-icon [icon]="cartIcon" aria-hidden="true"></fa-icon><span class="d-none d-sm-inline">Panier</span>
    @if (cart.itemCount() > 0) { <span class="shop-cart-badge" aria-label="{{ cart.itemCount() }} article{{ cart.itemCount() > 1 ? 's' : '' }}">{{ cart.itemCount() }}</span> }
  </a>`,
  styles: [`.shop-cart-link{align-items:center;border:1px solid rgba(255,255,255,.45);border-radius:.375rem;color:#fff;display:inline-flex;gap:.45rem;min-height:44px;padding:.45rem .65rem;position:relative;text-decoration:none}.shop-cart-link:hover,.shop-cart-link:focus-visible{background:rgba(255,255,255,.15);color:#fff}.shop-cart-badge{align-items:center;background:#ed6567;border:2px solid #212529;border-radius:999px;color:#111;display:inline-flex;font-size:.72rem;font-weight:800;height:1.35rem;justify-content:center;min-width:1.35rem;padding:0 .25rem}`]
})
export class ShopCartLinkComponent {
  readonly cart = inject(CartStore);
  readonly cartIcon = faCartShopping;
}
