import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
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
  private readonly router = inject(Router);
  readonly orders = signal<ShopOrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cancellingOrderNumber = signal<string | null>(null);
  readonly payingOrderNumber = signal<string | null>(null);
  readonly refundRequestOrderNumber = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly money = formatShopMoney;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true); this.error.set(null); this.actionError.set(null);
    this.api.orders().subscribe({
      next: orders => { this.orders.set(orders); this.loading.set(false); },
      error: () => { this.error.set('Vos commandes sont indisponibles pour le moment.'); this.loading.set(false); }
    });
  }

  cancel(order: ShopOrderDto): void {
    if (order.status !== 'PENDING_PAYMENT' || this.cancellingOrderNumber()) return;
    if (!window.confirm(`Annuler la commande ${order.orderNumber} ?`)) return;

    this.cancellingOrderNumber.set(order.orderNumber);
    this.actionError.set(null);
    this.api.cancelOrder(order.orderNumber).subscribe({
      next: cancelledOrder => {
        this.orders.update(orders => orders.map(candidate =>
          candidate.orderNumber === cancelledOrder.orderNumber ? cancelledOrder : candidate));
        this.cancellingOrderNumber.set(null);
      },
      error: () => {
        this.actionError.set('Cette commande ne peut plus être annulée. Actualisez la page pour voir son statut.');
        this.cancellingOrderNumber.set(null);
      }
    });
  }

  async pay(order: ShopOrderDto): Promise<void> {
    if (order.status !== 'PENDING_PAYMENT' || this.payingOrderNumber()) return;
    this.payingOrderNumber.set(order.orderNumber);
    this.actionError.set(null);
    try {
      const base = `${window.location.origin}${window.location.pathname}`;
      const pending = `${base}#/boutique/commandes/${encodeURIComponent(order.orderNumber)}/paiement`;
      const failed = `${base}#/boutique/commandes/${encodeURIComponent(order.orderNumber)}/paiement-echoue`;
      const session = await firstValueFrom(this.api.createPaymentSession(order.orderNumber, this.idempotencyKey(), pending, failed));
      if (!session.redirectUrl) throw new Error('URL de paiement absente');
      window.location.assign(session.redirectUrl);
    } catch (error) {
      this.actionError.set(error instanceof HttpErrorResponse && error.error?.code === 'INSECURE_CHECKOUT_URL'
        ? 'HelloAsso exige une adresse HTTPS pour revenir après le paiement. Ouvrez la boutique en HTTPS.'
        : 'Le paiement ne peut pas être démarré pour le moment. Aucun débit n’a été effectué.');
      this.payingOrderNumber.set(null);
    }
  }

  requestRefund(order: ShopOrderDto): void {
    if (!this.canRequestRefund(order) || this.refundRequestOrderNumber()) return;
    if (!window.confirm(`Envoyer une demande de remboursement pour la commande ${order.orderNumber} ?`)) return;
    this.refundRequestOrderNumber.set(order.orderNumber);
    this.actionError.set(null);
    this.api.requestRefund(order.orderNumber).subscribe({
      next: updatedOrder => {
        this.orders.update(orders => orders.map(candidate =>
          candidate.orderNumber === updatedOrder.orderNumber ? updatedOrder : candidate));
        this.refundRequestOrderNumber.set(null);
        void this.router.navigate(['/boutique/commandes', order.orderNumber, 'messages']);
      },
      error: () => {
        this.actionError.set('La demande de remboursement ne peut pas être envoyée pour le moment.');
        this.refundRequestOrderNumber.set(null);
      }
    });
  }

  canRequestRefund(order: ShopOrderDto): boolean {
    return !order.refundRequested && ['PAID', 'PROCESSING', 'COMPLETED'].includes(order.status);
  }

  private idempotencyKey(): string {
    return typeof crypto?.randomUUID === 'function' ? crypto.randomUUID() : `payment-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}
