import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { formatShopMoney } from '../shop-format';

@Component({
  imports: [DatePipe, RouterLink],
  templateUrl: './shop-orders.component.html',
  styleUrl: './shop-orders.component.css'
})
export class ShopOrdersComponent {
  private readonly api = inject(ShopApiService);
  readonly orders = signal<ShopOrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly money = formatShopMoney;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true); this.error.set(null);
    this.api.orders().subscribe({
      next: orders => { this.orders.set(orders); this.loading.set(false); },
      error: () => { this.error.set('Vos commandes sont indisponibles pour le moment.'); this.loading.set(false); }
    });
  }
}
