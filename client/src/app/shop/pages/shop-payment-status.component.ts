import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, timer } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';

@Component({
  imports: [RouterLink],
  templateUrl: './shop-payment-status.component.html',
  styleUrl: './shop-payment-status.component.css'
})
export class ShopPaymentStatusComponent {
  private readonly api = inject(ShopApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly order = signal<ShopOrderDto | null>(null);
  readonly verifying = signal(false);
  readonly error = signal<string | null>(null);
  readonly requestedFailure = this.route.snapshot.routeConfig?.path?.endsWith('paiement-echoue') ?? false;
  private orderNumber = '';
  private automaticVerification = true;
  ngOnInit(): void {
    this.orderNumber = this.route.snapshot.paramMap.get('orderNumber') ?? '';
    void this.verify();
    timer(5000, 5000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.automaticVerification) void this.verify();
    });
  }
  async verify(): Promise<void> {
    if (this.verifying()) return;
    this.verifying.set(true); this.error.set(null);
    try {
      this.order.set(await firstValueFrom(this.api.verifyPayment(this.orderNumber)));
      this.automaticVerification = true;
      this.redirectByStatus();
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 502) this.automaticVerification = false;
      await this.load();
      if (!this.error() && this.order()?.status === 'PENDING_PAYMENT' && !this.requestedFailure) {
        this.error.set(this.automaticVerification
          ? 'La vérification auprès de HelloAsso a échoué. Une nouvelle tentative sera lancée automatiquement.'
          : 'La vérification auprès de HelloAsso a échoué. Réessayez avec « Vérifier maintenant » après correction du problème.');
      }
    }
    finally { this.verifying.set(false); }
  }
  async load(): Promise<void> { try { this.order.set(await firstValueFrom(this.api.order(this.orderNumber))); this.redirectByStatus(); } catch { this.error.set('Impossible de vérifier le statut de la commande pour le moment.'); } }
  private redirectByStatus(): void { const status = this.order()?.status; if (status === 'PAID' || status === 'PROCESSING' || status === 'COMPLETED') void this.router.navigate(['/boutique/commandes', this.orderNumber, 'paiement-reussi']); }
}
