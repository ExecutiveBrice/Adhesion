import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, timeout } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminProductDto, ShopAdminVariantDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';

type StockFilter = 'all' | 'out' | 'watch' | 'prepare' | 'untracked';
type StockStatus = 'out' | 'watch' | 'ok' | 'inactive' | 'untracked';

export interface StockRow {
  product: ShopAdminProductDto;
  variant: ShopAdminVariantDto;
  available: number | null;
  toPrepare: number;
  outflow30Days: number;
  status: StockStatus;
}

/** Le stock des commandes réglées est déjà sorti de stockOnHand. */
export function buildStockRows(products: ShopAdminProductDto[], orders: ShopAdminOrderDto[], now = Date.now()): StockRow[] {
  const toPrepare = new Map<number, number>();
  const outflow30Days = new Map<number, number>();
  const start = now - 30 * 24 * 60 * 60 * 1000;

  for (const order of orders) {
    const paid = ['PAID', 'PROCESSING', 'COMPLETED', 'REFUNDED'].includes(order.status);
    if (!paid) continue;
    const needsPreparation = order.status === 'PAID' || order.status === 'PROCESSING';
    const createdRecently = new Date(order.createdAt).getTime() >= start;
    for (const item of order.items) {
      if (item.productVariantId == null) continue;
      if (needsPreparation && item.status !== 'COMPLETED' && item.status !== 'CANCELLED') {
        toPrepare.set(item.productVariantId, (toPrepare.get(item.productVariantId) ?? 0) + item.quantity);
      }
      if (createdRecently) {
        outflow30Days.set(item.productVariantId, (outflow30Days.get(item.productVariantId) ?? 0) + item.quantity);
      }
    }
  }

  return products.flatMap(product => product.variants.map(variant => {
    const available = variant.stockTracked ? (variant.stockOnHand ?? 0) - variant.stockReserved : null;
    const recent = outflow30Days.get(variant.id) ?? 0;
    const status: StockStatus = !product.active || !variant.active ? 'inactive'
      : !variant.stockTracked ? 'untracked'
      : available === 0 ? 'out'
      : available !== null && recent > 0 && available <= recent ? 'watch' : 'ok';
    return {
      product, variant, available, status,
      toPrepare: toPrepare.get(variant.id) ?? 0,
      outflow30Days: recent
    };
  })).sort((a, b) => {
    const priority: Record<StockStatus, number> = { out: 0, watch: 1, ok: 2, untracked: 3, inactive: 4 };
    return priority[a.status] - priority[b.status]
      || a.product.name.localeCompare(b.product.name, 'fr')
      || a.variant.displayOrder - b.variant.displayOrder
      || a.variant.sku.localeCompare(b.variant.sku, 'fr');
  });
}

@Component({
  selector: 'app-shop-stock-overview',
  imports: [DatePipe, FormsModule],
  templateUrl: './shop-stock-overview.component.html',
  styleUrl: './shop-stock-overview.component.css'
})
export class ShopStockOverviewComponent implements OnInit {
  private readonly api = inject(ShopAdminApiService);
  private loadRequestId = 0;
  rows: StockRow[] = [];
  loading = false;
  loaded = false;
  error = '';
  message = '';
  updatedAt: Date | null = null;
  query = '';
  filter: StockFilter = 'all';
  readonly stockDrafts: Record<number, number | null> = {};
  readonly purchaseDrafts: Record<number, number | null> = {};
  readonly rowErrors: Record<number, string> = {};
  readonly savingVariantIds = new Set<number>();

  ngOnInit(): void { this.load(); }

  load(preserveRowErrors = false): void {
    const requestId = ++this.loadRequestId;
    this.loading = true;
    this.error = '';
    if (!preserveRowErrors) {
      for (const id of Object.keys(this.rowErrors)) delete this.rowErrors[Number(id)];
    }
    forkJoin({
      products: this.api.products().pipe(timeout({ first: 10_000 })),
      orders: this.api.orders().pipe(timeout({ first: 10_000 }))
    }).subscribe({
      next: ({ products, orders }) => {
        if (requestId !== this.loadRequestId) return;
        this.rows = buildStockRows(this.collection<ShopAdminProductDto>(products), this.collection<ShopAdminOrderDto>(orders));
        for (const row of this.rows) this.stockDrafts[row.variant.id] = row.variant.stockOnHand;
        this.updatedAt = new Date();
        this.loaded = true;
        this.loading = false;
      },
      error: () => {
        if (requestId !== this.loadRequestId) return;
        this.error = 'Impossible de charger l’état des stocks et des commandes. Réessayez.';
        this.loaded = false;
        this.loading = false;
      }
    });
  }

  get filteredRows(): StockRow[] {
    const search = this.query.trim().toLocaleLowerCase('fr');
    return this.rows.filter(row => {
      if (this.filter === 'out' && row.status !== 'out') return false;
      if (this.filter === 'watch' && row.status !== 'watch') return false;
      if (this.filter === 'prepare' && row.toPrepare === 0) return false;
      if (this.filter === 'untracked' && row.variant.stockTracked) return false;
      return !search || [row.product.name, row.variant.label, row.variant.sku]
        .some(value => value?.toLocaleLowerCase('fr').includes(search));
    });
  }

  get outCount(): number { return this.rows.filter(row => row.status === 'out').length; }
  get watchCount(): number { return this.rows.filter(row => row.status === 'watch').length; }
  get reservedCount(): number { return this.rows.reduce((sum, row) => sum + row.variant.stockReserved, 0); }
  get toPrepareCount(): number { return this.rows.reduce((sum, row) => sum + row.toPrepare, 0); }
  get purchaseLineCount(): number { return this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0).length; }
  get invalidPurchaseCount(): number { return this.rows.filter(row => this.isPurchaseInvalid(row.variant.id)).length; }
  get hiddenPurchaseLineCount(): number {
    const visible = new Set(this.filteredRows.map(row => row.variant.id));
    return this.rows.filter(row => !visible.has(row.variant.id) && this.validPurchaseQuantity(row.variant.id) > 0).length;
  }

  setPurchaseQuantity(id: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.purchaseDrafts[id] = input.value === '' ? null : input.valueAsNumber;
  }

  isPurchaseInvalid(id: number): boolean {
    const quantity = this.purchaseDrafts[id];
    return quantity != null && (!Number.isSafeInteger(quantity) || quantity < 0);
  }

  statusLabel(status: StockStatus): string {
    return { out: 'Rupture', watch: 'À surveiller', ok: 'Disponible',
      inactive: 'Hors vente', untracked: 'Stock non suivi' }[status];
  }

  saveStock(row: StockRow): void {
    const id = row.variant.id;
    const quantity = this.stockDrafts[id];
    delete this.rowErrors[id];
    this.message = '';
    if (!Number.isSafeInteger(quantity) || quantity! < row.variant.stockReserved) {
      this.rowErrors[id] = `Saisissez un entier supérieur ou égal aux ${row.variant.stockReserved} unités réservées.`;
      return;
    }
    if (quantity === row.variant.stockOnHand || this.savingVariantIds.has(id)) return;
    this.savingVariantIds.add(id);
    this.api.updateVariantStock(id, quantity!, row.variant.version).pipe(timeout({ first: 10_000 })).subscribe({
      next: variant => {
        Object.assign(row.variant, variant);
        row.available = (variant.stockOnHand ?? 0) - variant.stockReserved;
        row.status = !row.product.active || !variant.active ? 'inactive'
          : row.available === 0 ? 'out'
          : row.outflow30Days > 0 && row.available <= row.outflow30Days ? 'watch' : 'ok';
        this.stockDrafts[id] = variant.stockOnHand;
        this.rows.sort((a, b) => {
          const priority: Record<StockStatus, number> = { out: 0, watch: 1, ok: 2, untracked: 3, inactive: 4 };
          return priority[a.status] - priority[b.status] || a.product.name.localeCompare(b.product.name, 'fr');
        });
        this.message = `Stock de ${row.product.name} — ${variant.label || variant.sku} mis à jour.`;
        this.savingVariantIds.delete(id);
        this.load();
      },
      error: response => {
        this.rowErrors[id] = response?.status === 409
          ? 'Le stock a changé depuis votre dernière actualisation. Vérifiez la nouvelle quantité avant de réessayer.'
          : response?.error?.detail || 'Le stock n’a pas pu être enregistré. Actualisez et réessayez.';
        this.savingVariantIds.delete(id);
        if (response?.status === 409) this.load(true);
      }
    });
  }

  clearPurchaseList(): void {
    for (const id of Object.keys(this.purchaseDrafts)) delete this.purchaseDrafts[Number(id)];
  }

  exportPurchaseList(): void {
    if (this.invalidPurchaseCount) return;
    const selected = this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0);
    if (!selected.length) return;
    const columns = ['Produit', 'Variante', 'SKU', 'Quantité à commander', 'En stock', 'Réservé', 'Disponible', 'À préparer', 'Cmd. créées 30 j'];
    const lines = selected.map(row => [row.product.name, row.variant.label || '', row.variant.sku,
      this.validPurchaseQuantity(row.variant.id), row.variant.stockOnHand, row.variant.stockReserved,
      row.available, row.toPrepare, row.outflow30Days]);
    const csv = '\uFEFF' + [columns, ...lines].map(line => line.map(value => this.csvCell(value)).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `reapprovisionnement-boutique-${new Date().toISOString().slice(0, 10)}.csv`;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  private validPurchaseQuantity(id: number): number {
    const quantity = this.purchaseDrafts[id];
    return Number.isSafeInteger(quantity) && quantity! > 0 ? quantity! : 0;
  }

  private csvCell(value: string | number | null): string {
    const text = String(value ?? '').replace(/[\r\n\t]+/g, ' ');
    const safe = typeof value === 'string' && /^\s*[=+\-@]/.test(text) ? `'${text}` : text;
    return `"${safe.replaceAll('"', '""')}"`;
  }

  private collection<T>(response: unknown): T[] {
    if (Array.isArray(response)) return response as T[];
    if (response && typeof response === 'object' && Array.isArray((response as { content?: unknown }).content)) {
      return (response as { content: T[] }).content;
    }
    return [];
  }
}
