import { Component, inject, Input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCartShopping, faCircleExclamation, faReceipt, faStore } from '@fortawesome/free-solid-svg-icons';
import { filter } from 'rxjs';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';

@Component({
  selector: 'app-shop-cart-link',
  imports: [RouterLink, FaIconComponent],
  templateUrl: './shop-cart-link.component.html',
  styles: [`.shop-cart-link{align-items:center;border:1px solid #17633f;border-radius:.375rem;color:#17633f;display:inline-flex;gap:.45rem;min-height:44px;padding:.45rem .65rem;position:relative;text-decoration:none}.shop-cart-link:hover,.shop-cart-link:focus-visible{background:#e7f4f1;color:#17564e}.shop-cart-link.shop-link{border-color:rgba(255,255,255,.45);color:#fff}.shop-cart-link.shop-link:hover,.shop-cart-link.shop-link:focus-visible{background:rgba(255,255,255,.15);color:#fff}.shop-cart-badge{align-items:center;background:#ed6567;border:2px solid currentColor;border-radius:999px;color:#111;display:inline-flex;font-size:.72rem;font-weight:800;height:1.35rem;justify-content:center;min-width:1.35rem;padding:0 .25rem}.shop-action-alert{align-items:center;background:#ed6567;border:2px solid #fff;border-radius:50%;color:#111;display:flex;font-size:.8rem;height:1.35rem;justify-content:center;position:absolute;right:-.4rem;top:-.4rem;width:1.35rem}.shop-cart-link:not(.shop-link) .shop-action-alert{border-color:#17633f}@media(max-width:1399.98px){.navigation-link .shop-link-label{display:none}.navigation-link{min-width:44px;justify-content:center}.navigation-link fa-icon{font-size:1.25rem}}`]
})
export class ShopCartLinkComponent {
  @Input() navigation = false;
  @Input() link: 'cart' | 'shop' | 'orders' = 'cart';
  readonly cart = inject(CartStore);
  private readonly api = inject(ShopApiService);
  private readonly router = inject(Router);
  private readonly pendingPayment = signal(false);
  readonly cartIcon = faCartShopping;
  readonly shopIcon = faStore;
  readonly ordersIcon = faReceipt;
  readonly alertIcon = faCircleExclamation;

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(() => this.refreshPendingPayment());
  }

  ngOnInit(): void {
    this.refreshPendingPayment();
  }

  private refreshPendingPayment(): void {
    if (this.link !== 'shop' && this.link !== 'orders') return;
    this.api.orders().subscribe({
      next: orders => this.pendingPayment.set(orders.some(order => order.status === 'PENDING_PAYMENT')),
      error: () => this.pendingPayment.set(false)
    });
  }

  ariaLabel(): string {
    return this.hasPendingAction()
      ? `${this.label} : une action est en attente`
      : this.label;
  }

  get label(): string {
    return this.link === 'shop' ? 'Boutique' : this.link === 'orders' ? 'Mes commandes' : 'Panier';
  }

  get route(): string {
    return this.link === 'shop' ? '/boutique' : this.link === 'orders' ? '/boutique/commandes' : '/boutique/panier';
  }

  get icon() {
    return this.link === 'shop' ? this.shopIcon : this.link === 'orders' ? this.ordersIcon : this.cartIcon;
  }

  hasPendingAction(): boolean {
    if (this.link === 'cart') return this.cart.itemCount() > 0;
    if (this.link === 'orders') return this.pendingPayment();
    return this.cart.itemCount() > 0 || this.pendingPayment();
  }
}
