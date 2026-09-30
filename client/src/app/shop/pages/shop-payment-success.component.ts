import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ShopApiService } from '../services/shop-api.service';
import { ShopOrderDto } from '../models/shop.models';

@Component({
  imports: [RouterLink],
  templateUrl: './shop-payment-success.component.html',
  styles: [`.shop-page{max-width:680px}.shop-success{background:#fff;border:1px solid #b7dfc9;border-radius:.75rem;padding:2.5rem 1rem;text-align:center}.shop-success div{align-items:center;background:#d8f1e5;border-radius:50%;color:#17633f;display:flex;font-size:2rem;font-weight:800;height:4rem;justify-content:center;margin:auto;width:4rem}.shop-success h1{margin:.75rem 0}`]
})
export class ShopPaymentSuccessComponent {
  private readonly api = inject(ShopApiService); private readonly route = inject(ActivatedRoute); readonly order = signal<ShopOrderDto | null>(null);
  ngOnInit(): void { const number = this.route.snapshot.paramMap.get('orderNumber') ?? ''; this.api.order(number).subscribe({ next: order => this.order.set(order) }); }
}
