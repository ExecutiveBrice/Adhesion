import { Injectable, computed, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CartEntry, CartQuoteDto } from '../models/shop.models';
import { ShopApiService } from './shop-api.service';

const STORAGE_KEY = 'alod.shop.cart.v1';

@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly entriesState = signal<CartEntry[]>(this.restore());
  readonly entries = this.entriesState.asReadonly();
  readonly quote = signal<CartQuoteDto | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly itemCount = computed(() => this.entriesState().reduce((count, entry) => count + entry.quantity, 0));
  readonly isEmpty = computed(() => this.entriesState().length === 0);

  constructor(private readonly api: ShopApiService) {}

  add(variantId: number): void {
    this.change(variantId, current => current + 1);
  }

  setQuantity(variantId: number, quantity: number): void {
    const safeQuantity = Math.floor(quantity);
    if (!Number.isFinite(safeQuantity) || safeQuantity <= 0) {
      this.remove(variantId);
      return;
    }
    this.replace(this.entriesState().map(entry => entry.variantId === variantId ? { ...entry, quantity: safeQuantity } : entry));
  }

  remove(variantId: number): void {
    this.replace(this.entriesState().filter(entry => entry.variantId !== variantId));
  }

  clear(): void {
    this.replace([]);
    this.quote.set(null);
  }

  async refreshQuote(): Promise<CartQuoteDto | null> {
    if (this.isEmpty()) {
      this.quote.set(null);
      return null;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      const quote = await firstValueFrom(this.api.quote(this.entriesState()));
      this.quote.set(quote);
      return quote;
    } catch {
      this.quote.set(null);
      this.error.set('Le panier a changé ou un article n’est plus disponible. Vérifiez vos articles puis réessayez.');
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  private change(variantId: number, update: (quantity: number) => number): void {
    const existing = this.entriesState().find(entry => entry.variantId === variantId);
    this.replace(existing
      ? this.entriesState().map(entry => entry.variantId === variantId ? { ...entry, quantity: update(entry.quantity) } : entry)
      : [...this.entriesState(), { variantId, quantity: update(0) }]);
  }

  private replace(entries: CartEntry[]): void {
    this.entriesState.set(entries);
    this.quote.set(null);
    this.error.set(null);
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
    } catch {
      // Le panier continue de fonctionner lorsque le stockage navigateur est indisponible.
    }
  }

  private restore(): CartEntry[] {
    try {
      const candidate: unknown = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]');
      if (!Array.isArray(candidate)) return [];
      return candidate.filter((item): item is CartEntry => typeof item?.variantId === 'number'
        && Number.isInteger(item.variantId) && typeof item?.quantity === 'number' && Number.isInteger(item.quantity)
        && item.quantity > 0);
    } catch {
      return [];
    }
  }
}
