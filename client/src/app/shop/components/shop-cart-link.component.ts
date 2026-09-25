import { Component, inject, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCartShopping, faStore } from '@fortawesome/free-solid-svg-icons';
import { CartStore } from '../services/cart.store';

@Component({
  selector: 'app-shop-cart-link',
  imports: [RouterLink, FaIconComponent],
  template: `<a class="shop-cart-link" [class.shop-link]="link === 'shop'" [routerLink]="link === 'shop' ? '/boutique' : '/boutique/panier'" [attr.aria-label]="link === 'shop' ? 'Ouvrir la boutique' : 'Ouvrir le panier'">
    <fa-icon [icon]="link === 'shop' ? shopIcon : cartIcon" aria-hidden="true"></fa-icon><span>{{ link === 'shop' ? 'Boutique' : 'Panier' }}</span>
    @if (cart.itemCount() > 0) { <span class="shop-cart-badge" aria-label="{{ cart.itemCount() }} article{{ cart.itemCount() > 1 ? 's' : '' }}">{{ cart.itemCount() }}</span> }
  </a>`,
  styles: [`.shop-cart-link{align-items:center;border:1px solid #17633f;border-radius:.375rem;color:#17633f;display:inline-flex;gap:.45rem;min-height:44px;padding:.45rem .65rem;position:relative;text-decoration:none}.shop-cart-link:hover,.shop-cart-link:focus-visible{background:#e7f4f1;color:#17564e}.shop-cart-link.shop-link{border-color:rgba(255,255,255,.45);color:#fff}.shop-cart-link.shop-link:hover,.shop-cart-link.shop-link:focus-visible{background:rgba(255,255,255,.15);color:#fff}.shop-cart-badge{align-items:center;background:#ed6567;border:2px solid currentColor;border-radius:999px;color:#111;display:inline-flex;font-size:.72rem;font-weight:800;height:1.35rem;justify-content:center;min-width:1.35rem;padding:0 .25rem}`]
})
export class ShopCartLinkComponent {
  @Input() link: 'cart' | 'shop' = 'cart';
  readonly cart = inject(CartStore);
  readonly cartIcon = faCartShopping;
  readonly shopIcon = faStore;
}
