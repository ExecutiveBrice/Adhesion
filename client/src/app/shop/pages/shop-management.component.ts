import { Component, OnDestroy, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, map, Observable, of, switchMap, timeout } from 'rxjs';
import {
  ShopAdminCategoryDto, ShopAdminCategoryRequest, ShopAdminOrderDto, ShopAdminProductDto, ShopAdminProductRequest,
  ShopAdminVariantCreateRequest, ShopAdminVariantDto, ShopAdminVariantRequest
} from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopAdminOrderCardComponent } from '../components/shop-admin-order-card.component';
import { ShopStockOverviewComponent } from '../components/shop-stock-overview.component';
import { ShopImageCropperComponent } from '../components/shop-image-cropper.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { buildOrderAvailability, ShopItemAvailability } from '../models/shop-order-availability';
import { formatShopOrderStatus } from '../shop-format';

type VariantDraft = ShopAdminVariantCreateRequest;

@Component({
  selector: 'app-shop-management',
  imports: [FormsModule, RouterLink, ShopAdminOrderCardComponent, ShopStockOverviewComponent, ShopImageCropperComponent],
  templateUrl: './shop-management.component.html',
  styleUrl: './shop-management.component.css'
})
export class ShopManagementComponent implements OnDestroy {
  @ViewChild(ShopStockOverviewComponent) stockOverview?: ShopStockOverviewComponent;
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly api = inject(ShopAdminApiService);
  products: ShopAdminProductDto[] = [];
  categories: ShopAdminCategoryDto[] = [];
  activeTab: 'stocks' | 'catalogue' | 'orders' = 'stocks';
  ordersInProgress: ShopAdminOrderDto[] = [];
  completedOrders: ShopAdminOrderDto[] = [];
  ordersLoading = false;
  ordersLoaded = false;
  ordersError = '';
  orderAvailability: ReadonlyMap<number, ShopItemAvailability> = new Map();
  loading = false;
  catalogLoaded = false;
  saving = false;
  error = '';
  catalogConflict = false;
  message = '';
  imageUploadError = '';
  imageCrop: { file: File; productId: number | null; additionalIndex?: number; variantKey?: string } | null = null;
  readonly additionalImageSlots = [0, 1];
  private readonly additionalImageDrafts = new Map<string, { file: File; preview: string }>();
  private readonly variantImageDrafts = new Map<string, { file: File; preview: string }>();
  selectedCategoryIds = new Set<number>();
  readonly variantDrafts: Record<number, VariantDraft> = {};
  private newProductImageFile: File | null = null;
  newProductImagePreview: string | null = null;
  private readonly productImageFiles: Record<number, File | undefined> = {};
  readonly productImagePreviews: Record<number, string | undefined> = {};

  newCategory: ShopAdminCategoryRequest = { name: '', slug: '', description: null, active: true, displayOrder: 0 };
  newProduct: ShopAdminProductRequest = {
    name: '', slug: '', description: null, imageUrl: null, active: false, displayOrder: 0, categoryIds: []
  };

  ngOnDestroy(): void {
    this.revokePreview(this.newProductImagePreview);
    Object.values(this.productImagePreviews).forEach(preview => this.revokePreview(preview));
    this.additionalImageDrafts.forEach(draft => this.revokePreview(draft.preview));
    this.variantImageDrafts.forEach(draft => this.revokePreview(draft.preview));
  }

  selectTab(tab: 'stocks' | 'catalogue' | 'orders'): void {
    const previous = this.activeTab;
    this.activeTab = tab;
    if (tab === 'stocks' && previous !== 'stocks') this.stockOverview?.load();
    if (tab === 'catalogue' && !this.catalogLoaded && !this.loading) this.load();
    if (tab === 'orders' && previous !== 'orders' && !this.ordersLoading) this.loadOrders();
  }

  loadOrders(): void {
    this.ordersLoading = true;
    this.ordersError = '';
    forkJoin({ orders: this.api.orders(), products: this.api.products() }).pipe(timeout({ first: 10_000 })).subscribe({
      next: ({ orders: response, products }) => {
        const orders = this.collection<ShopAdminOrderDto>(response);
        this.orderAvailability = buildOrderAvailability(this.collection<ShopAdminProductDto>(products), orders);
        this.ordersInProgress = orders.filter(order => !this.isCompletedOrder(order));
        this.completedOrders = orders.filter(order => this.isCompletedOrder(order));
        this.ordersLoaded = true;
        this.ordersLoading = false;
      },
      error: () => {
        this.ordersError = 'Impossible de charger les commandes. Réessayez.';
        this.ordersLoading = false;
      }
    });
  }

  readonly orderStatus = formatShopOrderStatus;

  reclassifyOrders(): void {
    const orders = [...this.ordersInProgress, ...this.completedOrders];
    this.ordersInProgress = orders.filter(order => !this.isCompletedOrder(order));
    this.completedOrders = orders.filter(order => this.isCompletedOrder(order));
  }

  private isCompletedOrder(order: ShopAdminOrderDto): boolean {
    return ['COMPLETED', 'CANCELLED', 'EXPIRED', 'REFUNDED'].includes(order.status);
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      products: this.api.products().pipe(timeout({ first: 10_000 })),
      categories: this.api.categories().pipe(timeout({ first: 10_000 }))
    }).subscribe({
      next: ({ products, categories }) => {
        this.products = this.collection<ShopAdminProductDto>(products);
        this.categories = this.collection<ShopAdminCategoryDto>(categories);
        this.catalogLoaded = true;
        this.loading = false;
      },
      error: error => this.fail(error)
    });
  }

  toggleNewProductCategory(categoryId: number, selected: boolean): void {
    if (selected) this.selectedCategoryIds.add(categoryId); else this.selectedCategoryIds.delete(categoryId);
    this.newProduct.categoryIds = [...this.selectedCategoryIds];
  }

  createCategory(): void {
    this.run(this.api.createCategory(this.cleanCategory(this.newCategory)), () => {
      this.newCategory = { name: '', slug: '', description: null, active: true, displayOrder: this.categories.length };
      this.message = 'Catégorie ajoutée.';
      this.load();
    });
  }

  updateCategory(category: ShopAdminCategoryDto): void {
    this.run(this.api.updateCategory(category.id, this.cleanCategory(category)), () => {
      this.message = 'Catégorie mise à jour.';
      this.load();
    });
  }

  deleteCategory(category: ShopAdminCategoryDto): void {
    if (!window.confirm(`Retirer la catégorie « ${category.name} » ?`)) return;
    this.run(this.api.deleteCategory(category.id), () => {
      this.message = 'Catégorie retirée.';
      this.load();
    });
  }

  createProduct(): void {
    if (this.saving || this.imageCrop) return;
    this.run(this.requestWithNewProductImage().pipe(switchMap(request => this.api.createProduct(request))), () => {
      this.newProduct = { name: '', slug: '', description: null, imageUrl: null, active: false, displayOrder: this.products.length, categoryIds: [] };
      this.clearNewProductImage();
      this.clearAdditionalImageDrafts(null);
      this.selectedCategoryIds.clear();
      this.message = 'Produit ajouté.';
      this.load();
    });
  }

  updateProduct(product: ShopAdminProductDto): void {
    if (this.saving || this.imageCrop) return;
    const request = this.cleanProduct({ ...product, categoryIds: product.categories.map(category => category.id) });
    this.run(this.requestWithProductImage(product.id, request).pipe(
      switchMap(productRequest => this.api.updateProduct(product.id, productRequest))
    ), () => {
      this.clearProductImage(product.id);
      this.clearAdditionalImageDrafts(product.id);
      this.message = 'Produit mis à jour.';
      this.load();
    });
  }

  deleteProduct(product: ShopAdminProductDto): void {
    if (!window.confirm(`Supprimer définitivement « ${product.name} » et ses variantes ?`)) return;
    this.run(this.api.deleteProduct(product.id), () => {
      this.clearProductImage(product.id);
      this.clearAdditionalImageDrafts(product.id);
      this.message = 'Produit retiré.';
      this.load();
    });
  }

  draftFor(productId: number): VariantDraft {
    return this.variantDrafts[productId] ??= {
      sku: '', label: null, priceAmountInCents: 0, currency: 'EUR', active: true,
      displayOrder: 0
    };
  }

  createVariant(product: ShopAdminProductDto): void {
    if (this.saving || this.imageCrop) return;
    const key = this.newVariantImageKey(product.id);
    this.run(this.requestWithVariantImage(key, this.cleanVariant(this.draftFor(product.id))).pipe(
      switchMap(request => this.api.createVariant(product.id, request))
    ), () => {
      this.clearVariantImage(key);
      delete this.variantDrafts[product.id];
      this.message = 'Variante ajoutée.';
      this.load();
    });
  }

  updateVariant(variant: ShopAdminVariantDto): void {
    if (this.saving || this.imageCrop) return;
    const request: ShopAdminVariantRequest = {
      ...this.cleanVariant(variant), stockTracked: true, expectedVersion: variant.version
    };
    const key = this.variantImageKey(variant.id);
    this.run(this.requestWithVariantImage(key, request).pipe(
      switchMap(variantRequest => this.api.updateVariant(variant.id, variantRequest))
    ), () => {
      this.clearVariantImage(key);
      this.message = 'Variante mise à jour.';
      this.load();
    });
  }

  deleteVariant(variantId: number): void {
    if (!window.confirm('Retirer cette variante ?')) return;
    this.run(this.api.deleteVariant(variantId), () => {
      this.clearVariantImage(this.variantImageKey(variantId));
      this.message = 'Variante retirée.';
      this.load();
    });
  }

  isCategoryOnProduct(product: ShopAdminProductDto, categoryId: number): boolean {
    return product.categories.some(category => category.id === categoryId);
  }

  toggleProductCategory(product: ShopAdminProductDto, category: ShopAdminCategoryDto, selected: boolean): void {
    product.categories = selected
      ? [...product.categories, category]
      : product.categories.filter(item => item.id !== category.id);
  }

  onNewProductImageSelected(event: Event): void {
    const file = this.fileFrom(event);
    if (!file || this.saving || this.imageCrop) return;
    if (!this.validateImage(file)) return;
    this.imageCrop = { file, productId: null };
  }

  onProductImageSelected(productId: number, event: Event): void {
    const file = this.fileFrom(event);
    if (!file || this.saving || this.imageCrop) return;
    if (!this.validateImage(file)) return;
    this.imageCrop = { file, productId };
  }

  applyImageCrop(file: File): void {
    if (!this.imageCrop || !this.validateImage(file)) return;
    const { productId, additionalIndex, variantKey } = this.imageCrop;
    if (variantKey !== undefined) {
      this.clearVariantImage(variantKey);
      this.variantImageDrafts.set(variantKey, { file, preview: URL.createObjectURL(file) });
    } else if (additionalIndex !== undefined) {
      const key = this.additionalImageKey(productId, additionalIndex);
      this.revokePreview(this.additionalImageDrafts.get(key)?.preview);
      this.additionalImageDrafts.set(key, { file, preview: URL.createObjectURL(file) });
    } else if (productId === null) {
      this.clearNewProductImage();
      this.newProductImageFile = file;
      this.newProductImagePreview = URL.createObjectURL(file);
    } else {
      this.clearProductImage(productId);
      this.productImageFiles[productId] = file;
      this.productImagePreviews[productId] = URL.createObjectURL(file);
    }
    this.imageCrop = null;
  }

  onAdditionalImageSelected(productId: number | null, index: number, event: Event): void {
    const file = this.fileFrom(event);
    if (!file || this.saving || this.imageCrop || !this.additionalImageSlots.includes(index)) return;
    if (this.validateImage(file)) this.imageCrop = { file, productId, additionalIndex: index };
  }

  additionalImagePreview(product: ShopAdminProductDto | null, index: number): string | null {
    return this.additionalImageDrafts.get(this.additionalImageKey(product?.id ?? null, index))?.preview
      ?? (product ?? this.newProduct).additionalImageUrls?.[index] ?? null;
  }

  removeAdditionalImage(product: ShopAdminProductDto | null, index: number): void {
    const key = this.additionalImageKey(product?.id ?? null, index);
    this.revokePreview(this.additionalImageDrafts.get(key)?.preview);
    this.additionalImageDrafts.delete(key);
    const target = product ?? this.newProduct;
    const urls = [...(target.additionalImageUrls ?? [])];
    // Conserve les emplacements des imports en attente jusqu’à l’enregistrement.
    urls[index] = '';
    target.additionalImageUrls = urls;
  }

  private additionalImageKey(productId: number | null, index: number): string {
    return `${productId ?? 'new'}:${index}`;
  }

  private clearAdditionalImageDrafts(productId: number | null): void {
    for (const index of this.additionalImageSlots) {
      const key = this.additionalImageKey(productId, index);
      this.revokePreview(this.additionalImageDrafts.get(key)?.preview);
      this.additionalImageDrafts.delete(key);
    }
  }

  variantImageKey(variantId: number): string { return `variant:${variantId}`; }
  newVariantImageKey(productId: number): string { return `new-variant:${productId}`; }

  onVariantImageSelected(key: string, event: Event): void {
    const file = this.fileFrom(event);
    if (!file || this.saving || this.imageCrop) return;
    if (this.validateImage(file)) this.imageCrop = { file, productId: null, variantKey: key };
  }

  variantImagePreview(key: string, variant: VariantDraft): string | null {
    return this.variantImageDrafts.get(key)?.preview ?? variant.imageUrl ?? null;
  }

  removeVariantImage(key: string, variant: VariantDraft): void {
    this.clearVariantImage(key);
    variant.imageUrl = null;
  }

  private clearVariantImage(key: string): void {
    this.revokePreview(this.variantImageDrafts.get(key)?.preview);
    this.variantImageDrafts.delete(key);
  }

  private requestWithVariantImage<T extends VariantDraft>(key: string, request: T): Observable<T> {
    const image = this.variantImageDrafts.get(key);
    return image ? this.api.uploadProductImage(image.file).pipe(map(imageUrl => ({ ...request, imageUrl }))) : of(request);
  }

  previewFor(product: ShopAdminProductDto): string | null {
    return this.productImagePreviews[product.id] ?? product.imageUrl;
  }

  clearNewProductImage(): void {
    this.revokePreview(this.newProductImagePreview);
    this.newProductImageFile = null;
    this.newProductImagePreview = null;
  }

  removeNewProductImage(): void {
    this.clearNewProductImage();
    this.newProduct.imageUrl = null;
  }

  removeProductImage(product: ShopAdminProductDto): void {
    this.clearProductImage(product.id);
    product.imageUrl = null;
  }

  private run(request: Observable<unknown>, success: () => void): void {
    this.saving = true;
    this.error = '';
    this.catalogConflict = false;
    this.message = '';
    request.subscribe({ next: success, error: error => this.fail(error), complete: () => this.saving = false });
  }

  private fail(error: any): void {
    this.loading = false;
    this.saving = false;
    this.catalogConflict = error?.status === 409;
    this.error = error?.error?.detail || 'La modification n’a pas pu être enregistrée.';
  }

  private cleanCategory(category: ShopAdminCategoryRequest | ShopAdminCategoryDto): ShopAdminCategoryRequest {
    return { name: category.name.trim(), slug: category.slug.trim(), description: this.optional(category.description), active: category.active, displayOrder: Number(category.displayOrder) || 0 };
  }

  private cleanProduct(product: ShopAdminProductRequest): ShopAdminProductRequest {
    return { ...product, name: product.name.trim(), slug: product.slug.trim(), description: this.optional(product.description), imageUrl: this.optional(product.imageUrl), displayOrder: Number(product.displayOrder) || 0 };
  }

  private requestWithNewProductImage(): Observable<ShopAdminProductRequest> {
    return this.requestWithImages(null, this.cleanProduct(this.newProduct), this.newProductImageFile);
  }

  private requestWithProductImage(productId: number, request: ShopAdminProductRequest): Observable<ShopAdminProductRequest> {
    return this.requestWithImages(productId, request, this.productImageFiles[productId]);
  }

  private requestWithImages(productId: number | null, request: ShopAdminProductRequest,
                            mainImage: File | null | undefined): Observable<ShopAdminProductRequest> {
    const additionalImages = this.additionalImageSlots.map(index => {
      const draft = this.additionalImageDrafts.get(this.additionalImageKey(productId, index));
      return draft ? this.api.uploadProductImage(draft.file) : of(request.additionalImageUrls?.[index] ?? '');
    });
    return forkJoin({
      imageUrl: mainImage ? this.api.uploadProductImage(mainImage) : of(request.imageUrl),
      additionalImageUrls: forkJoin(additionalImages)
    }).pipe(map(images => ({ ...request, ...images, additionalImageUrls: images.additionalImageUrls.filter(Boolean) })));
  }

  private fileFrom(event: Event): File | null {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    input.value = '';
    return file;
  }

  private validateImage(file: File): boolean {
    this.imageUploadError = '';
    if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
      this.imageUploadError = 'Choisissez une image PNG, JPEG, GIF ou WebP.';
      return false;
    }
    return true;
  }

  private clearProductImage(productId: number): void {
    this.revokePreview(this.productImagePreviews[productId]);
    delete this.productImageFiles[productId];
    delete this.productImagePreviews[productId];
  }

  private revokePreview(preview: string | null | undefined): void {
    if (preview?.startsWith('blob:')) URL.revokeObjectURL(preview);
  }

  private cleanVariant(variant: VariantDraft): VariantDraft {
    return { ...(variant.imageUrl !== undefined ? { imageUrl: this.optional(variant.imageUrl) } : {}), sku: variant.sku.trim(), label: this.optional(variant.label), currency: variant.currency.trim().toUpperCase(), priceAmountInCents: Number(variant.priceAmountInCents) || 0, displayOrder: Number(variant.displayOrder) || 0, active: variant.active };
  }

  private optional(value: string | null): string | null { return value?.trim() || null; }

  /** Accepte aussi une réponse Spring paginée pendant la transition des anciennes API. */
  private collection<T>(response: unknown): T[] {
    if (Array.isArray(response)) return response as T[];
    if (response && typeof response === 'object' && Array.isArray((response as { content?: unknown }).content)) {
      return (response as { content: T[] }).content;
    }
    return [];
  }
}
