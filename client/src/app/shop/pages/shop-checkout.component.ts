import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';
import { ShopOrderSummaryComponent } from '../components/shop-order-summary.component';

@Component({
  imports: [RouterLink, ShopOrderSummaryComponent],
  templateUrl: './shop-checkout.component.html',
  styleUrl: './shop-checkout.component.css'
})
export class ShopCheckoutComponent {
  readonly cart = inject(CartStore);
  private readonly api = inject(ShopApiService);
  private readonly router = inject(Router);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  ngOnInit(): void { void this.cart.refreshQuote(); }
  async submit(): Promise<void> {
    if (this.submitting() || this.cart.isEmpty()) return;
    this.submitting.set(true); this.error.set(null);
    const quote = await this.cart.refreshQuote();
    if (!quote) { this.error.set(this.cart.error() ?? 'Le panier doit être actualisé avant le paiement.'); this.submitting.set(false); return; }
    try {
      const order = await firstValueFrom(this.api.createOrder(this.cart.entries(), this.idempotencyKey()));
      this.cart.clear();
      await this.router.navigate(['/boutique/commandes', order.orderNumber]);
    } catch {
      this.error.set('La commande n’a pas pu être créée. Aucun paiement n’a été lancé ; vous pouvez réessayer.');
    } finally { this.submitting.set(false); }
  }
  private idempotencyKey(): string { return typeof crypto?.randomUUID === 'function' ? crypto.randomUUID() : `checkout-${Date.now()}-${Math.random().toString(16).slice(2)}`; }
}
