import { Component, DestroyRef, inject, Input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCartShopping, faReceipt, faStore } from '@fortawesome/free-solid-svg-icons';
import { filter, Subscription } from 'rxjs';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';
import { TokenStorageService } from '../../_services/token-storage.service';

@Component({
  selector: 'app-shop-cart-link',
  imports: [RouterLink, FaIconComponent],
  templateUrl: './shop-cart-link.component.html',
  styles: [`.shop-cart-link{align-items:center;border:1px solid #17633f;border-radius:.375rem;color:#17633f;display:inline-flex;gap:.45rem;min-height:44px;padding:.45rem .65rem;position:relative;text-decoration:none}.shop-cart-link:hover,.shop-cart-link:focus-visible{background:#e7f4f1;color:#17564e}.shop-cart-link.shop-link,.shop-cart-link.navigation-link{border-color:rgba(255,255,255,.45);color:#fff}.shop-cart-link.shop-link:hover,.shop-cart-link.shop-link:focus-visible,.shop-cart-link.navigation-link:hover,.shop-cart-link.navigation-link:focus-visible{background:rgba(255,255,255,.15);color:#fff}.shop-cart-badge{align-items:center;background:#fd7e14;border:2px solid #fff;border-radius:999px;color:#111;display:inline-flex;font-size:.72rem;font-weight:800;height:1.35rem;justify-content:center;min-width:1.35rem;padding:0 .25rem}@media(max-width:1399.98px){.navigation-link .shop-cart-badge{position:absolute;bottom:-.4rem;right:-.4rem}.navigation-link .shop-link-label{display:none}.navigation-link{min-width:44px;justify-content:center}.navigation-link > fa-icon{font-size:1.25rem}}`]
})
export class ShopCartLinkComponent {
  @Input() navigation = false;
  @Input() link: 'cart' | 'shop' | 'orders' = 'cart';
  readonly cart = inject(CartStore);
  private readonly api = inject(ShopApiService);
  private readonly storage = inject(TokenStorageService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private ordersSubscription?: Subscription;
  readonly hasOrders = signal(false);
  readonly pendingOrderCount = signal(0);
  readonly cartIcon = faCartShopping;
  readonly shopIcon = faStore;
  readonly ordersIcon = faReceipt;

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
    this.ordersSubscription?.unsubscribe();
    this.hasOrders.set(false);
    if (!this.storage.getToken() || this.storage.isTokenExpired()) {
      this.pendingOrderCount.set(0);
      return;
    }
    this.ordersSubscription = this.api.orders().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: orders => {
        this.hasOrders.set(orders.length > 0);
        this.pendingOrderCount.set(orders.filter(order => order.status === 'PENDING_PAYMENT').length);
      },
      error: () => this.pendingOrderCount.set(0)
    });
  }

  ariaLabel(): string {
    const count = this.link === 'cart' ? this.cart.itemCount() : this.pendingOrderCount();
    if (count === 0) return this.label;
    const detail = this.link !== 'cart'
      ? `commande${count > 1 ? 's' : ''} en attente`
      : `article${count > 1 ? 's' : ''}`;
    return `${this.label} : ${count} ${detail}`;
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

}
