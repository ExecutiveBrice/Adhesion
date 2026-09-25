import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { ShopOrderSummaryComponent } from '../components/shop-order-summary.component';

@Component({
  imports: [RouterLink, DatePipe, ShopOrderSummaryComponent],
  templateUrl: './shop-order-detail.component.html',
  styleUrl: './shop-order-detail.component.css'
})
export class ShopOrderDetailComponent {
  private readonly api = inject(ShopApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly order = signal<ShopOrderDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly startingPayment = signal(false);
  private orderNumber = '';
  ngOnInit(): void { this.orderNumber = this.route.snapshot.paramMap.get('orderNumber') ?? ''; this.load(); }
  load(): void { this.loading.set(true); this.api.order(this.orderNumber).subscribe({ next: order => { this.order.set(order); this.loading.set(false); }, error: () => { this.error.set('Cette commande est introuvable ou ne vous appartient pas.'); this.loading.set(false); } }); }
  async pay(): Promise<void> {
    if (this.startingPayment() || !this.order()) return;
    this.startingPayment.set(true); this.error.set(null);
    try {
      const base = `${window.location.origin}${window.location.pathname}`;
      const pending = `${base}#/boutique/commandes/${encodeURIComponent(this.orderNumber)}/paiement`;
      const failed = `${base}#/boutique/commandes/${encodeURIComponent(this.orderNumber)}/paiement-echoue`;
      const session = await firstValueFrom(this.api.createPaymentSession(this.orderNumber, this.idempotencyKey(), pending, failed));
      if (!session.redirectUrl) throw new Error('URL de paiement absente');
      window.location.assign(session.redirectUrl);
    } catch (error) {
      this.error.set(error instanceof HttpErrorResponse && error.error?.code === 'INSECURE_CHECKOUT_URL'
        ? 'HelloAsso exige une adresse HTTPS pour revenir après le paiement. Ouvrez la boutique en HTTPS.'
        : 'Le paiement ne peut pas être démarré pour le moment. Aucun débit n’a été effectué.');
      this.startingPayment.set(false);
    }
  }
  private idempotencyKey(): string { return typeof crypto?.randomUUID === 'function' ? crypto.randomUUID() : `payment-${Date.now()}-${Math.random().toString(16).slice(2)}`; }
}
