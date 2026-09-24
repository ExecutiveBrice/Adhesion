import { Component, Input } from '@angular/core';
import { CartQuoteDto, ShopOrderDto } from '../models/shop.models';
import { formatShopMoney } from '../shop-format';

@Component({
  selector: 'app-shop-order-summary',
  template: `<section class="shop-summary" aria-label="Récapitulatif de la commande"><h2>{{ title }}</h2><ul class="list-unstyled">@for (line of lines; track line.variantId) { <li><span>{{ line.productName }} @if (line.variantName) { <small>— {{ line.variantName }}</small> } <small>× {{ line.quantity }}</small></span><strong>{{ money(line.lineTotal) }}</strong></li> }</ul><dl><div><dt>Sous-total</dt><dd>{{ money(summary.subtotal) }}</dd></div>@if (summary.discountTotal.amountInCents > 0) { <div><dt>Réductions</dt><dd>−{{ money(summary.discountTotal) }}</dd></div> }@if (summary.feesTotal.amountInCents > 0) { <div><dt>Frais</dt><dd>{{ money(summary.feesTotal) }}</dd></div> }<div class="shop-summary-total"><dt>Total</dt><dd>{{ money(summary.total) }}</dd></div></dl></section>`,
  styles: [`.shop-summary{background:#fff;border:1px solid #dce5e3;border-radius:.75rem;padding:1rem}.shop-summary h2{font-size:1.1rem}.shop-summary li,.shop-summary dl>div{display:flex;gap:1rem;justify-content:space-between;padding:.5rem 0}.shop-summary li+li{border-top:1px solid #edf1f0}.shop-summary small{color:#596663}.shop-summary dl{margin:1rem 0 0}.shop-summary dt{font-weight:400}.shop-summary dd{font-weight:600;margin:0}.shop-summary-total{border-top:2px solid #bcded8;font-size:1.1rem}.shop-summary-total dt,.shop-summary-total dd{font-weight:800}`]
})
export class ShopOrderSummaryComponent {
  @Input({ required: true }) summary!: CartQuoteDto | ShopOrderDto;
  @Input() title = 'Récapitulatif';
  readonly money = formatShopMoney;
  get lines() { return this.summary.items; }
}
