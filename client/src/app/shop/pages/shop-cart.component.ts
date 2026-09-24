import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartStore } from '../services/cart.store';
import { ShopOrderSummaryComponent } from '../components/shop-order-summary.component';
import { formatShopMoney } from '../shop-format';

@Component({
  imports: [RouterLink, FormsModule, ShopOrderSummaryComponent],
  templateUrl: './shop-cart.component.html',
  styleUrl: './shop-cart.component.css'
})
export class ShopCartComponent {
  readonly cart = inject(CartStore);
  readonly refreshing = signal(false);
  readonly money = formatShopMoney;
  ngOnInit(): void { void this.refresh(); }
  async refresh(): Promise<void> { this.refreshing.set(true); await this.cart.refreshQuote(); this.refreshing.set(false); }
  quantity(variantId: number, value: string): void { this.cart.setQuantity(variantId, Number(value)); void this.refresh(); }
  remove(variantId: number): void { this.cart.remove(variantId); void this.refresh(); }
}
