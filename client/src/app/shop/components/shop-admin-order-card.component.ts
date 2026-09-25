import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { timeout } from 'rxjs';
import {
  ShopAdminOrderDto, ShopAdminOrderItemDto, ShopAdminOrderItemStatus
} from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-shop-admin-order-card',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './shop-admin-order-card.component.html',
  styleUrl: './shop-admin-order-card.component.css'
})
export class ShopAdminOrderCardComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly api = inject(ShopAdminApiService);

  @Input({ required: true }) order!: ShopAdminOrderDto;
  @Input({ required: true }) statusLabel!: string;
  @Output() statusChanged = new EventEmitter<void>();

  expanded = false;
  savingOrderStatus = false;
  orderStatusSaved = false;
  orderStatusError = '';
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
    if (this.savingOrderStatus) return;
    this.orderStatusSaved = false;
    this.orderStatusError = '';
    if (status === this.order.status) return;

    this.savingOrderStatus = true;
    this.api.updateOrderStatus(this.order.orderNumber, status)
      .pipe(timeout({ first: 10_000 })).subscribe({
        next: updated => {
          this.order.status = updated.status;
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
          item.status = updated.status;
          select.value = updated.status;
          this.savedItemIds.add(item.id);
          this.savingItemIds.delete(item.id);
        },
        error: () => {
          select.value = item.status;
          this.itemErrors[item.id] = 'Le statut n’a pas pu être enregistré. Réessayez.';
          this.savingItemIds.delete(item.id);
        }
      });
  }
}
