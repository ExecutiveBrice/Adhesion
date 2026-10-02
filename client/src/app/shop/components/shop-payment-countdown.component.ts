import { DatePipe } from '@angular/common';
import { Component, DestroyRef, Input, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';

@Component({
  selector: 'app-shop-payment-countdown',
  imports: [DatePipe],
  template: `
    @if (status === 'PENDING_PAYMENT' && paymentExpiresAt) {
      <p class="shop-payment-countdown" [class.elapsed]="isExpired()">
        @if (isExpired()) {
          Délai de paiement écoulé : vérification finale en cours avant la libération du stock.
        } @else {
          Temps restant pour payer : <strong>{{ remainingLabel() }}</strong>
          <small>Jusqu’au {{ paymentExpiresAt | date:'dd/MM/yyyy à HH:mm':'':'fr-FR' }}.</small>
        }
      </p>
    }
  `,
  styles: [`
    .shop-payment-countdown{background:#fff7dd;border:1px solid #e6cf83;border-radius:.55rem;color:#5b4918;
      display:block;font-size:.9rem;margin:.5rem 0 1rem;padding:.5rem .75rem}
    .shop-payment-countdown strong{font-variant-numeric:tabular-nums}
    .shop-payment-countdown small{display:block}
    .shop-payment-countdown.elapsed{background:#f3f4f4;border-color:#d1d5d5;color:#4a5552}
  `]
})
export class ShopPaymentCountdownComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly now = signal(Date.now());
  @Input() status: ShopOrderDto['status'] = 'PENDING_PAYMENT';
  @Input() paymentExpiresAt?: string | null;

  constructor() {
    timer(0, 1000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.now.set(Date.now()));
  }

  isExpired(): boolean {
    return !!this.paymentExpiresAt && this.remainingMilliseconds() <= 0;
  }

  remainingLabel(): string {
    const seconds = Math.ceil(this.remainingMilliseconds() / 1000);
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const remainder = seconds % 60;
    return [hours, minutes, remainder].map(value => String(value).padStart(2, '0')).join(':');
  }

  private remainingMilliseconds(): number {
    if (!this.paymentExpiresAt) return 0;
    const deadline = Date.parse(this.paymentExpiresAt);
    return Number.isFinite(deadline) ? Math.max(0, deadline - this.now()) : 0;
  }
}
