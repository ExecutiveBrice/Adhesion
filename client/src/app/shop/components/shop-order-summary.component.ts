import { Component, Input } from '@angular/core';
import { CartQuoteDto, ShopOrderDto } from '../models/shop.models';
import { formatShopMoney } from '../shop-format';

@Component({
  selector: 'app-shop-order-summary',
  templateUrl: './shop-order-summary.component.html',
  styles: [`.shop-summary{background:#fff;border:1px solid #dce5e3;border-radius:.75rem;padding:1rem}.shop-summary h2{font-size:1.1rem}.shop-summary li,.shop-summary dl>div{display:flex;gap:1rem;justify-content:space-between;padding:.5rem 0}.shop-summary li+li{border-top:1px solid #edf1f0}.shop-summary small{color:#596663}.shop-summary dl{margin:1rem 0 0}.shop-summary dt{font-weight:400}.shop-summary dd{font-weight:600;margin:0}.shop-summary-total{border-top:2px solid #bcded8;font-size:1.1rem}.shop-summary-total dt,.shop-summary-total dd{font-weight:800}`]
})
export class ShopOrderSummaryComponent {
  @Input({ required: true }) summary!: CartQuoteDto | ShopOrderDto;
  @Input() title = 'Récapitulatif';
  readonly money = formatShopMoney;
  get lines() { return this.summary.items; }
}
