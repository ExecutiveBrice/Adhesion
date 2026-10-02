import { DatePipe } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of, timeout } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminOrderItemStatus, ShopAdminProductDto, ShopAdminVariantDto, ShopSupplierOrderDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

type StockFilter = 'all' | 'to-order' | 'awaiting' | 'ready' | 'surplus' | 'untracked';
type StockStatus = 'to-order' | 'awaiting' | 'ready' | 'surplus' | 'settled' | 'inactive' | 'untracked';

export type StockActivity =
  | { type: 'customer'; date: string; orderNumber: string; quantity: number;
      itemStatus: ShopAdminOrderItemStatus; orderStatus: ShopAdminOrderDto['status'];
      stockEffect: 'reserved' | 'deducted' | 'none' }
  | { type: 'supplier'; date: string; orderId: number; supplierName: string;
      reference: string | null; quantity: number; status: ShopSupplierOrderDto['status']; receivedAt: string | null };

export interface StockRow {
  product: ShopAdminProductDto;
  variant: ShopAdminVariantDto;
  available: number | null;
  customerOrders: number;
  toPrepare: number;
  awaitingSupplier: number;
  toOrder: number;
  readyToHandOver: number;
  surplus: number;
  outflow30Days: number;
  activities: StockActivity[];
  status: StockStatus;
}

/** Le stock des commandes réglées est déjà sorti de stockOnHand. */
export function buildStockRows(products: ShopAdminProductDto[], orders: ShopAdminOrderDto[], now = Date.now(),
                               supplierOrders: ShopSupplierOrderDto[] = []): StockRow[] {
  const toPrepare = new Map<number, number>();
  const immobilizedByOrders = new Map<number, number>();
  const outflow30Days = new Map<number, number>();
  const activities = new Map<number, StockActivity[]>();
  const awaitingSupplier = new Map<number, number>();
  const start = now - 30 * 24 * 60 * 60 * 1000;

  for (const order of orders) {
    const paid = ['PAID', 'PROCESSING', 'COMPLETED', 'REFUNDED'].includes(order.status);
    const needsPreparation = order.status === 'PAID' || order.status === 'PROCESSING';
    const immobilizesStock = order.status === 'PENDING_PAYMENT' || needsPreparation;
    const createdRecently = new Date(order.createdAt).getTime() >= start;
    for (const item of order.items) {
      if (item.productVariantId == null) continue;
      const stockEffect: 'reserved' | 'deducted' | 'none' = item.stockReserved === false
        || ['DRAFT', 'CANCELLED', 'EXPIRED'].includes(order.status) ? 'none'
        : order.status === 'PENDING_PAYMENT' ? 'reserved' : 'deducted';
      const lines = activities.get(item.productVariantId) ?? [];
      lines.push({ type: 'customer', date: order.createdAt, orderNumber: order.orderNumber,
        quantity: item.quantity, itemStatus: item.status, orderStatus: order.status, stockEffect });
      activities.set(item.productVariantId, lines);
      if (immobilizesStock && item.status !== 'COMPLETED' && item.status !== 'CANCELLED') {
        immobilizedByOrders.set(item.productVariantId,
          (immobilizedByOrders.get(item.productVariantId) ?? 0) + item.quantity);
      }
      if (needsPreparation && item.status !== 'COMPLETED' && item.status !== 'CANCELLED') {
        toPrepare.set(item.productVariantId, (toPrepare.get(item.productVariantId) ?? 0) + item.quantity);
      }
      if (paid && createdRecently) {
        outflow30Days.set(item.productVariantId, (outflow30Days.get(item.productVariantId) ?? 0) + item.quantity);
      }
    }
  }

  for (const order of supplierOrders) {
    for (const line of order.lines) {
      if (order.status === 'ORDERED') {
        awaitingSupplier.set(line.variantId, (awaitingSupplier.get(line.variantId) ?? 0) + line.quantity);
      }
      const lines = activities.get(line.variantId) ?? [];
      lines.push({ type: 'supplier', date: order.createdAt, orderId: order.id,
        supplierName: order.supplierName, reference: order.reference, quantity: line.quantity,
        status: order.status, receivedAt: order.receivedAt });
      activities.set(line.variantId, lines);
    }
  }

  for (const lines of activities.values()) {
    lines.sort((a, b) => (Date.parse(b.date) || 0) - (Date.parse(a.date) || 0));
  }

  return products.flatMap(product => product.variants.map(variant => {
    const available = variant.stockTracked ? (variant.stockOnHand ?? 0) - variant.stockReserved : null;
    const recent = outflow30Days.get(variant.id) ?? 0;
    const demand = toPrepare.get(variant.id) ?? 0;
    const incoming = awaitingSupplier.get(variant.id) ?? 0;
    // Les commandes payées ont déjà été déduites de stockOnHand. On les réintègre
    // pour estimer les pièces présentes, puis on retire les réservations non payées.
    const presentForPaid = variant.stockTracked
      ? Math.max(0, (variant.stockOnHand ?? 0) + demand - variant.stockReserved) : 0;
    const openCommitments = immobilizedByOrders.get(variant.id) ?? 0;
    const readyToHandOver = Math.min(demand, presentForPaid);
    // Le stock comptable a déjà été réduit des commandes payées. On les réintègre
    // pour comparer le stock physique aux commandes ouvertes, paiement en attente inclus.
    const presentForOpenCommitments = variant.stockTracked
      ? Math.max(0, (variant.stockOnHand ?? 0) + demand) : 0;
    const toOrder = variant.stockTracked ? Math.max(0, openCommitments - presentForOpenCommitments - incoming) : 0;
    const surplus = variant.stockTracked ? Math.max(0, presentForPaid - demand) : 0;
    const status: StockStatus = !product.active || !variant.active ? 'inactive'
      : !variant.stockTracked ? 'untracked'
      : toOrder > 0 ? 'to-order'
      : demand > readyToHandOver && incoming > 0 ? 'awaiting'
      : readyToHandOver > 0 ? 'ready'
      : surplus > 0 ? 'surplus' : 'settled';
    return {
      product, variant, available, status,
      customerOrders: openCommitments, toPrepare: demand, awaitingSupplier: incoming,
      toOrder, readyToHandOver, surplus,
      outflow30Days: recent,
      activities: activities.get(variant.id) ?? []
    };
  })).sort((a, b) => {
    const priority: Record<StockStatus, number> = {
      'to-order': 0, awaiting: 1, ready: 2, surplus: 3, settled: 4, untracked: 5, inactive: 6
    };
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
  @ViewChild('supplierOrderDialog') supplierOrderDialog!: ElementRef<HTMLDialogElement>;
  supplierOrderOpen = false;
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly api = inject(ShopAdminApiService);
  private loadRequestId = 0;
  rows: StockRow[] = [];
  supplierOrders: ShopSupplierOrderDto[] = [];
  loading = false;
  loaded = false;
  error = '';
  warning = '';
  message = '';
  updatedAt: Date | null = null;
  query = '';
  filter: StockFilter = 'all';
  readonly stockDrafts: Record<number, number | null> = {};
  readonly purchaseDrafts: Record<number, number | null> = {};
  readonly purchaseCostDrafts: Record<number, number | null> = {};
  readonly purchaseExtraDrafts: Record<number, boolean> = {};
  readonly rowErrors: Record<number, string> = {};
  readonly savingVariantIds = new Set<number>();
  readonly receivingOrderIds = new Set<number>();
  readonly expandedVariantIds = new Set<number>();
  supplierName = '';
  supplierReference = '';
  creatingSupplierOrder = false;
  supplierOrderError = '';
  activeDraftId: number | null = null;
  draftDirty = false;
  placingSupplierOrder = false;

  ngOnInit(): void { this.load(); }

  load(preserveRowErrors = false): void {
    const requestId = ++this.loadRequestId;
    this.loading = true;
    this.error = '';
    this.warning = '';
    if (!preserveRowErrors) {
      for (const id of Object.keys(this.rowErrors)) delete this.rowErrors[Number(id)];
    }
    let ordersUnavailable = false;
    let supplierOrdersUnavailable = false;
    forkJoin({
      products: this.api.products().pipe(timeout({ first: 10_000 })),
      orders: this.api.orders().pipe(timeout({ first: 10_000 }), catchError(() => {
        ordersUnavailable = true;
        return of([] as ShopAdminOrderDto[]);
      })),
      supplierOrders: this.api.supplierOrders().pipe(timeout({ first: 10_000 }), catchError(() => {
        supplierOrdersUnavailable = true;
        return of([] as ShopSupplierOrderDto[]);
      }))
    }).subscribe({
      next: ({ products, orders, supplierOrders }) => {
        if (requestId !== this.loadRequestId) return;
        this.supplierOrders = this.collection<ShopSupplierOrderDto>(supplierOrders);
        this.warning = ordersUnavailable && supplierOrdersUnavailable
          ? 'Les commandes client et fournisseur sont indisponibles. Les quantités de stock restent visibles.'
          : ordersUnavailable ? 'Les commandes client sont indisponibles. Les retraits ne sont pas affichés.'
          : supplierOrdersUnavailable ? 'Les commandes fournisseur sont indisponibles. Les réceptions ne sont pas affichées.' : '';
        this.rows = buildStockRows(this.collection<ShopAdminProductDto>(products),
          this.collection<ShopAdminOrderDto>(orders), Date.now(), this.supplierOrders);
        for (const row of this.rows) this.stockDrafts[row.variant.id] = row.variant.stockOnHand;
        this.updatedAt = new Date();
        this.loaded = true;
        this.loading = false;
        if (this.supplierOrderOpen && !this.draftDirty && this.activeDraftId == null) this.prepareSupplierOrder();
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
      if (this.filter === 'to-order' && row.toOrder === 0) return false;
      if (this.filter === 'awaiting' && row.awaitingSupplier === 0) return false;
      if (this.filter === 'ready' && row.readyToHandOver === 0) return false;
      if (this.filter === 'surplus' && row.surplus === 0) return false;
      if (this.filter === 'untracked' && row.variant.stockTracked) return false;
      return !search || [row.product.name, row.variant.label, row.variant.sku]
        .some(value => value?.toLocaleLowerCase('fr').includes(search));
    });
  }

  get toOrderCount(): number { return this.rows.reduce((sum, row) => sum + row.toOrder, 0); }
  get awaitingCount(): number { return this.rows.reduce((sum, row) => sum + row.awaitingSupplier, 0); }
  get readyCount(): number { return this.rows.reduce((sum, row) => sum + row.readyToHandOver, 0); }
  get surplusCount(): number { return this.rows.reduce((sum, row) => sum + row.surplus, 0); }
  get reservedCount(): number { return this.rows.reduce((sum, row) => sum + row.variant.stockReserved, 0); }
  get toPrepareCount(): number { return this.rows.reduce((sum, row) => sum + row.toPrepare, 0); }
  get purchaseLineCount(): number { return this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0).length; }
  get orderedSupplierOrders(): ShopSupplierOrderDto[] {
    return this.supplierOrders.filter(order => order.status === 'ORDERED');
  }
  supplierOrderTotalCents(order: ShopSupplierOrderDto): number | null {
    if (order.lines.some(line => line.lineTotalAmountInCents == null)) return null;
    return order.lines.reduce((sum, line) => sum + (line.lineTotalAmountInCents ?? 0), 0);
  }
  get invalidPurchaseCount(): number { return this.rows.filter(row => this.isPurchaseInvalid(row.variant.id)).length; }
  get invalidCostCount(): number { return this.rows.filter(row => this.isCostInvalid(row.variant.id)).length; }
  get missingCostCount(): number {
    return this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0
      && this.purchaseCostDrafts[row.variant.id] == null).length;
  }
  get unapprovedExtraCount(): number {
    return this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > row.toOrder
      && !this.purchaseExtraDrafts[row.variant.id]).length;
  }
  get purchaseTotalCents(): number | null {
    if (this.missingCostCount || this.invalidCostCount || this.invalidPurchaseCount) return null;
    return this.rows.reduce((sum, row) => sum + this.validPurchaseQuantity(row.variant.id)
      * (this.purchaseCostDrafts[row.variant.id] ?? 0), 0);
  }
  get purchaseRows(): StockRow[] { return this.rows.filter(row => row.variant.stockTracked); }

  openSupplierOrder(): void {
    if (!this.draftDirty && this.activeDraftId == null && this.loaded && !this.loading) this.prepareSupplierOrder();
    this.supplierOrderOpen = true;
    if (!this.supplierOrderDialog.nativeElement.open) this.supplierOrderDialog.nativeElement.showModal();
  }

  closeSupplierOrder(): void {
    if (this.creatingSupplierOrder || this.placingSupplierOrder) return;
    if (this.supplierOrderDialog.nativeElement.open) this.supplierOrderDialog.nativeElement.close();
    this.supplierOrderOpen = false;
  }

  setPurchaseQuantity(id: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.purchaseDrafts[id] = input.value === '' ? null : input.valueAsNumber;
    const row = this.rows.find(candidate => candidate.variant.id === id);
    if (row && this.validPurchaseQuantity(id) <= row.toOrder) this.purchaseExtraDrafts[id] = false;
    this.draftDirty = true;
  }

  setExtraApproved(id: number, event: Event): void {
    this.purchaseExtraDrafts[id] = (event.target as HTMLInputElement).checked;
    this.draftDirty = true;
  }

  setPurchaseCost(id: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const euros = input.valueAsNumber;
    const cents = euros * 100;
    this.purchaseCostDrafts[id] = input.value === '' ? null
      : Number.isFinite(cents) && Math.abs(cents - Math.round(cents)) < 0.000001
        ? Math.round(cents) : NaN;
    this.draftDirty = true;
  }

  isCostInvalid(id: number): boolean {
    const cents = this.purchaseCostDrafts[id];
    return cents != null && (!Number.isSafeInteger(cents) || cents < 0);
  }

  purchaseCostEuros(id: number): number | '' {
    const cents = this.purchaseCostDrafts[id];
    return cents == null || !Number.isFinite(cents) ? '' : cents / 100;
  }

  prepareSupplierOrder(): void {
    this.clearPurchaseList();
    for (const row of this.rows) {
      if (row.toOrder > 0 && row.variant.stockTracked) this.purchaseDrafts[row.variant.id] = row.toOrder;
    }
    this.activeDraftId = null;
    this.supplierOrderError = '';
    this.draftDirty = true;
  }

  resumeDraft(order: ShopSupplierOrderDto): void {
    if (order.status !== 'DRAFT') return;
    this.clearPurchaseList();
    for (const line of order.lines) {
      this.purchaseDrafts[line.variantId] = line.quantity;
      this.purchaseCostDrafts[line.variantId] = line.unitCostAmountInCents ?? null;
      this.purchaseExtraDrafts[line.variantId] = line.extraApproved ?? false;
    }
    this.activeDraftId = order.id;
    this.supplierName = order.supplierName;
    this.supplierReference = order.reference ?? '';
    this.supplierOrderError = '';
    this.draftDirty = false;
    this.openSupplierOrder();
  }

  isPurchaseInvalid(id: number): boolean {
    const quantity = this.purchaseDrafts[id];
    return quantity != null && (!Number.isSafeInteger(quantity) || quantity < 0);
  }

  statusLabel(status: StockStatus): string {
    return { 'to-order': 'À commander', awaiting: 'En attente fournisseur', ready: 'Prêt à remettre',
      surplus: 'Surplus disponible', settled: 'Aucun besoin', inactive: 'Hors vente',
      untracked: 'Stock non suivi' }[status];
  }

  orderStatusLabel(status: ShopAdminOrderDto['status']): string {
    return {
      DRAFT: 'Brouillon', PENDING_PAYMENT: 'En attente de paiement', PAID: 'Payée',
      PROCESSING: 'En préparation', COMPLETED: 'Terminée', CANCELLED: 'Annulée',
      EXPIRED: 'Expirée', REFUNDED: 'Remboursée'
    }[status];
  }

  itemStatusLabel(status: ShopAdminOrderItemStatus): string {
    return { PENDING: 'En attente', PROCESSING: 'En préparation',
      COMPLETED: 'Terminé', CANCELLED: 'Annulé' }[status];
  }

  stockEffectLabel(effect: 'reserved' | 'deducted' | 'none'): string {
    return { reserved: 'Réservé', deducted: 'Sorti du stock', none: 'Aucun retrait' }[effect];
  }

  toggleRow(row: StockRow): void {
    if (this.expandedVariantIds.has(row.variant.id)) this.expandedVariantIds.delete(row.variant.id);
    else this.expandedVariantIds.add(row.variant.id);
  }

  onRowClick(row: StockRow, event: Event): void {
    const target = event.target as HTMLElement;
    if (target.closest('input, button, select, textarea, form, a')) return;
    this.toggleRow(row);
  }

  saveStock(row: StockRow): void {
    const id = row.variant.id;
    const quantity = this.stockDrafts[id];
    delete this.rowErrors[id];
    this.message = '';
    if (!Number.isSafeInteger(quantity) || quantity! < 0) {
      this.rowErrors[id] = 'Saisissez un entier positif ou zéro.';
      return;
    }
    if (quantity === row.variant.stockOnHand || this.savingVariantIds.has(id)) return;
    this.savingVariantIds.add(id);
    this.api.updateVariantStock(id, quantity!, row.variant.version).pipe(timeout({ first: 10_000 })).subscribe({
      next: variant => {
        Object.assign(row.variant, variant);
        row.available = (variant.stockOnHand ?? 0) - variant.stockReserved;
        const presentForPaid = Math.max(0, (variant.stockOnHand ?? 0) + row.toPrepare - variant.stockReserved);
        row.readyToHandOver = Math.min(row.toPrepare, presentForPaid);
        row.toOrder = Math.max(0, row.toPrepare - presentForPaid - row.awaitingSupplier);
        row.surplus = Math.max(0, presentForPaid - row.toPrepare);
        row.status = !row.product.active || !variant.active ? 'inactive'
          : row.toOrder > 0 ? 'to-order'
          : row.toPrepare > row.readyToHandOver && row.awaitingSupplier > 0 ? 'awaiting'
          : row.readyToHandOver > 0 ? 'ready'
          : row.surplus > 0 ? 'surplus' : 'settled';
        this.stockDrafts[id] = variant.stockOnHand;
        this.rows.sort((a, b) => {
          const priority: Record<StockStatus, number> = {
            'to-order': 0, awaiting: 1, ready: 2, surplus: 3, settled: 4, untracked: 5, inactive: 6
          };
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
    for (const id of Object.keys(this.purchaseCostDrafts)) delete this.purchaseCostDrafts[Number(id)];
    for (const id of Object.keys(this.purchaseExtraDrafts)) delete this.purchaseExtraDrafts[Number(id)];
    this.draftDirty = true;
  }

  createSupplierOrder(): void {
    this.supplierOrderError = '';
    const lines = this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0)
      .map(row => ({ variantId: row.variant.id, quantity: this.validPurchaseQuantity(row.variant.id),
        unitCostAmountInCents: this.purchaseCostDrafts[row.variant.id] ?? null,
        extraApproved: this.purchaseExtraDrafts[row.variant.id] ?? false }));
    if (!this.supplierName.trim() || !lines.length || this.invalidPurchaseCount || this.invalidCostCount
        || this.creatingSupplierOrder) {
      this.supplierOrderError = 'Indiquez un fournisseur et au moins une quantité entière positive.';
      return;
    }
    this.creatingSupplierOrder = true;
    const request = { supplierName: this.supplierName.trim(),
      reference: this.supplierReference.trim() || null, lines };
    const save = this.activeDraftId == null ? this.api.createSupplierOrderDraft(request)
      : this.api.updateSupplierOrderDraft(this.activeDraftId, request);
    save.pipe(timeout({ first: 10_000 })).subscribe({
      next: order => {
        this.creatingSupplierOrder = false;
        this.activeDraftId = order.id;
        this.draftDirty = false;
        this.message = `Brouillon fournisseur n°${order.id} enregistré. Il ne compte pas encore dans les quantités attendues.`;
        this.load();
      },
      error: response => {
        this.creatingSupplierOrder = false;
        this.supplierOrderError = response?.error?.detail || 'La commande fournisseur n’a pas pu être enregistrée.';
      }
    });
  }

  placeSupplierOrder(): void {
    if (this.activeDraftId == null || this.purchaseLineCount === 0 || this.draftDirty || this.missingCostCount
        || this.invalidCostCount || this.unapprovedExtraCount || this.placingSupplierOrder) return;
    this.placingSupplierOrder = true;
    this.supplierOrderError = '';
    this.api.placeSupplierOrderDraft(this.activeDraftId).pipe(timeout({ first: 10_000 })).subscribe({
      next: order => {
        this.placingSupplierOrder = false;
        this.activeDraftId = null;
        this.supplierName = '';
        this.supplierReference = '';
        this.clearPurchaseList();
        this.draftDirty = false;
        this.closeSupplierOrder();
        this.message = `Commande fournisseur n°${order.id} passée. Les quantités sont désormais attendues.`;
        this.load();
      },
      error: response => {
        this.placingSupplierOrder = false;
        this.supplierOrderError = response?.error?.detail
          || 'La commande n’a pas pu être passée. Vérifiez les besoins et enregistrez le brouillon à nouveau.';
        if (response?.status === 409) this.load();
      }
    });
  }

  receiveSupplierOrder(order: ShopSupplierOrderDto): void {
    if (order.status !== 'ORDERED' || this.receivingOrderIds.has(order.id)) return;
    this.supplierOrderError = '';
    this.receivingOrderIds.add(order.id);
    this.api.receiveSupplierOrder(order.id).pipe(timeout({ first: 10_000 })).subscribe({
      next: () => {
        this.receivingOrderIds.delete(order.id);
        this.message = `Commande fournisseur n°${order.id} réceptionnée. Le stock a été ajouté.`;
        this.load();
      },
      error: response => {
        this.receivingOrderIds.delete(order.id);
        this.supplierOrderError = response?.error?.detail || 'La réception n’a pas pu être enregistrée.';
        if (response?.status === 409) this.load();
      }
    });
  }

  exportPurchaseList(): void {
    if (this.invalidPurchaseCount || this.invalidCostCount || this.missingCostCount) return;
    const selected = this.rows.filter(row => this.validPurchaseQuantity(row.variant.id) > 0);
    if (!selected.length) return;
    const columns = ['Produit', 'Variante', 'SKU', 'Quantité achat', 'Prix achat unitaire EUR',
      'Montant ligne EUR', 'À servir', 'Attendu fournisseur', 'À commander'];
    const lines = selected.map(row => [row.product.name, row.variant.label || '', row.variant.sku,
      this.validPurchaseQuantity(row.variant.id), this.euros(this.purchaseCostDrafts[row.variant.id]!),
      this.euros(this.validPurchaseQuantity(row.variant.id) * this.purchaseCostDrafts[row.variant.id]!),
      row.toPrepare, row.awaitingSupplier, row.toOrder]);
    const total = ['', '', 'Total', '', '', this.euros(this.purchaseTotalCents!), '', '', ''];
    const csv = '\uFEFF' + [columns, ...lines, total].map(line => line.map(value => this.csvCell(value)).join(';')).join('\r\n');
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

  euros(cents: number): string { return (cents / 100).toFixed(2).replace('.', ','); }

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
