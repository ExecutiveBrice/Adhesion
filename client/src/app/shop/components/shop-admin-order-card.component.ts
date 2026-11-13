import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { timeout } from 'rxjs';
import { RouterLink } from '@angular/router';
import {
  ShopAdminOrderDto, ShopAdminOrderItemDto, ShopAdminOrderItemStatus
} from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { ShopPaymentCountdownComponent } from './shop-payment-countdown.component';
import { ShopItemAvailability } from '../models/shop-order-availability';

@Component({
  selector: 'app-shop-admin-order-card',
  imports: [CurrencyPipe, DatePipe, RouterLink, ShopPaymentCountdownComponent],
  templateUrl: './shop-admin-order-card.component.html',
  styleUrl: './shop-admin-order-card.component.css'
})
export class ShopAdminOrderCardComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly api = inject(ShopAdminApiService);
  private readonly changeDetector = inject(ChangeDetectorRef);

  @Input({ required: true }) order!: ShopAdminOrderDto;
  @Input({ required: true }) statusLabel!: string;
  @Input() availability: ReadonlyMap<number, ShopItemAvailability> = new Map();
  @Output() statusChanged = new EventEmitter<void>();

  expanded = false;

  itemAvailability(item: ShopAdminOrderItemDto): ShopItemAvailability {
    if (item.status === 'COMPLETED') return { state: 'completed', label: 'Article livré', missingQuantity: 0 };
    if (item.status === 'CANCELLED' || ['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(this.order.status)) {
      return { state: 'cancelled', label: 'Aucune livraison attendue', missingQuantity: 0 };
    }
    return this.availability.get(item.id) ?? { state: 'unknown', label: 'Stock à vérifier', missingQuantity: 0 };
  }

  get deliveryAvailability(): { state: string; label: string } {
    if (this.order.status === 'COMPLETED') return { state: 'completed', label: 'Commande livrée' };
    if (['CANCELLED', 'EXPIRED', 'REFUNDED'].includes(this.order.status)) return { state: 'cancelled', label: 'Aucune livraison attendue' };
    const items = this.order.items.filter(item => item.status !== 'CANCELLED' && item.status !== 'COMPLETED');
    const states = items.map(item => this.itemAvailability(item));
    const missing = states.reduce((sum, item) => sum + item.missingQuantity, 0);
    if (missing) return { state: 'missing', label: `${missing} article${missing > 1 ? 's' : ''} manquant${missing > 1 ? 's' : ''}` };
    if (states.some(item => item.state === 'unknown') || !this.order.items.length) return { state: 'unknown', label: 'Stock à vérifier' };
    if (this.order.status === 'PENDING_PAYMENT' || this.order.status === 'DRAFT') return { state: 'unknown', label: 'En attente de paiement' };
    if (!items.length) return { state: 'completed', label: 'Aucune livraison restante' };
    return { state: 'ready', label: 'Peut être livrée' };
  }
  savingOrderStatus = false;
  orderStatusSaved = false;
  orderStatusError = '';
  verifyingPayment = false;
  paymentVerificationMessage = '';
  paymentVerificationError = '';
  customerEmailCopied = false;
  customerEmailCopyError = '';
  readonly savingItemIds = new Set<number>();
  readonly savedItemIds = new Set<number>();
  itemErrors: Record<number, string> = {};
  readonly statuses: { value: ShopAdminOrderItemStatus; label: string }[] = [
    { value: 'PENDING', label: 'À traiter' },
    { value: 'PROCESSING', label: 'En préparation' },
    { value: 'COMPLETED', label: 'Terminée' },
    { value: 'CANCELLED', label: 'Annulée' }
  ];

  availableItemStatuses(item: ShopAdminOrderItemDto): { value: ShopAdminOrderItemStatus; label: string }[] {
    if (this.order.status === 'PENDING_PAYMENT' && this.order.items.length === 1) {
      return this.statuses.filter(option => option.value === item.status || option.value === 'CANCELLED');
    }
    if (this.order.status !== 'PAID' && this.order.status !== 'PROCESSING') {
      return this.statuses.filter(option => option.value === item.status);
    }
    return this.statuses.filter(option => option.value === item.status
      || (item.status === 'PENDING' && (option.value === 'PROCESSING' || option.value === 'COMPLETED'))
      || (item.status === 'PROCESSING' && option.value === 'COMPLETED'));
  }

  verifyPayment(): void {
    if (this.verifyingPayment || this.savingOrderStatus) return;
    this.verifyingPayment = true;
    this.paymentVerificationMessage = '';
    this.paymentVerificationError = '';
    this.changeDetector.markForCheck();
    this.api.verifyPayment(this.order.orderNumber).pipe(timeout({ first: 30_000 })).subscribe({
      next: updated => {
        Object.assign(this.order, updated);
        this.verifyingPayment = false;
        this.changeDetector.markForCheck();
        this.paymentVerificationMessage = updated.status === 'PENDING_PAYMENT'
          ? 'Vérification effectuée : le paiement n’est pas encore confirmé par HelloAsso.'
          : 'Vérification effectuée : le statut de la commande est à jour.';
        this.statusChanged.emit();
      },
      error: response => {
        this.verifyingPayment = false;
        this.paymentVerificationError = response?.error?.detail || 'La vérification du paiement a échoué. Réessayez.';
        this.changeDetector.markForCheck();
      }
    });
  }

  async copyCustomerEmail(): Promise<void> {
    if (!this.order.customerEmail) return;
    this.customerEmailCopied = false;
    this.customerEmailCopyError = '';
    try {
      await navigator.clipboard.writeText(this.order.customerEmail);
      this.customerEmailCopied = true;
    } catch {
      this.customerEmailCopyError = 'La copie de l’adresse e-mail a échoué.';
    }
  }

  setOrderStatus(status: ShopAdminOrderDto['status'], select: HTMLSelectElement): void {
    if (this.savingOrderStatus || this.verifyingPayment) return;
    this.orderStatusSaved = false;
    this.orderStatusError = '';
    if (status === this.order.status) return;

    this.savingOrderStatus = true;
    this.api.updateOrderStatus(this.order.orderNumber, status)
      .pipe(timeout({ first: 10_000 })).subscribe({
        next: updated => {
          Object.assign(this.order, updated);
          select.value = updated.status;
          this.orderStatusSaved = true;
          this.savingOrderStatus = false;
          this.statusChanged.emit();
        },
        error: () => {
          select.value = this.order.status;
          this.orderStatusError = 'Le statut de la commande n’a pas pu être enregistré. Réessayez.';
          this.savingOrderStatus = false;
        }
      });
  }

  setStatus(item: ShopAdminOrderItemDto, status: ShopAdminOrderItemStatus, select: HTMLSelectElement): void {
    if (this.savingItemIds.has(item.id)) return;
    this.savedItemIds.delete(item.id);
    delete this.itemErrors[item.id];
    if (status === item.status) return;

    this.savingItemIds.add(item.id);
    this.api.updateOrderItemStatus(this.order.orderNumber, item.id, status)
      .pipe(timeout({ first: 10_000 })).subscribe({
        next: updated => {
          Object.assign(this.order, updated);
          select.value = updated.items.find(updatedItem => updatedItem.id === item.id)?.status ?? status;
          this.savedItemIds.add(item.id);
          this.savingItemIds.delete(item.id);
          this.statusChanged.emit();
        },
        error: () => {
          select.value = item.status;
          this.itemErrors[item.id] = 'Le statut n’a pas pu être enregistré. Réessayez.';
          this.savingItemIds.delete(item.id);
        }
      });
  }
}
