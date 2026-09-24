import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, Observable, timeout } from 'rxjs';
import {
  ShopAdminCategoryDto, ShopAdminCategoryRequest, ShopAdminProductDto, ShopAdminProductRequest,
  ShopAdminVariantRequest
} from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';

type VariantDraft = ShopAdminVariantRequest;

@Component({
  selector: 'app-shop-management',
  imports: [FormsModule, RouterLink],
  templateUrl: './shop-management.component.html',
  styleUrl: './shop-management.component.css'
})
export class ShopManagementComponent implements OnInit {
  private readonly apiViewRefresh = registerApiViewRefresh();
  private readonly api = inject(ShopAdminApiService);
  products: ShopAdminProductDto[] = [];
  categories: ShopAdminCategoryDto[] = [];
  loading = true;
  saving = false;
  error = '';
  message = '';
  selectedCategoryIds = new Set<number>();
  readonly variantDrafts: Record<number, VariantDraft> = {};

  newCategory: ShopAdminCategoryRequest = { name: '', slug: '', description: null, active: true, displayOrder: 0 };
  newProduct: ShopAdminProductRequest = {
    name: '', slug: '', description: null, imageUrl: null, active: false, displayOrder: 0, categoryIds: []
  };

  ngOnInit(): void { this.load(); }

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
    this.run(this.api.createProduct(this.cleanProduct(this.newProduct)), () => {
      this.newProduct = { name: '', slug: '', description: null, imageUrl: null, active: false, displayOrder: this.products.length, categoryIds: [] };
      this.selectedCategoryIds.clear();
      this.message = 'Produit ajouté.';
      this.load();
    });
  }

  updateProduct(product: ShopAdminProductDto): void {
    this.run(this.api.updateProduct(product.id, this.cleanProduct({
      ...product, categoryIds: product.categories.map(category => category.id)
    })), () => {
      this.message = 'Produit mis à jour.';
      this.load();
    });
  }

  deleteProduct(product: ShopAdminProductDto): void {
    if (!window.confirm(`Supprimer définitivement « ${product.name} » et ses variantes ?`)) return;
    this.run(this.api.deleteProduct(product.id), () => {
      this.message = 'Produit retiré.';
      this.load();
    });
  }

  draftFor(productId: number): VariantDraft {
    return this.variantDrafts[productId] ??= {
      sku: '', label: null, priceAmountInCents: 0, currency: 'EUR', active: true,
      displayOrder: 0, stockTracked: false, stockOnHand: null
    };
  }

  createVariant(product: ShopAdminProductDto): void {
    this.run(this.api.createVariant(product.id, this.cleanVariant(this.draftFor(product.id))), () => {
      delete this.variantDrafts[product.id];
      this.message = 'Variante ajoutée.';
      this.load();
    });
  }

  updateVariant(variant: VariantDraft & { id: number }): void {
    this.run(this.api.updateVariant(variant.id, this.cleanVariant(variant)), () => {
      this.message = 'Variante mise à jour.';
      this.load();
    });
  }

  deleteVariant(variantId: number): void {
    if (!window.confirm('Retirer cette variante ?')) return;
    this.run(this.api.deleteVariant(variantId), () => {
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

  private run(request: Observable<unknown>, success: () => void): void {
    this.saving = true;
    this.error = '';
    this.message = '';
    request.subscribe({ next: success, error: error => this.fail(error), complete: () => this.saving = false });
  }

  private fail(error: any): void {
    this.loading = false;
    this.saving = false;
    this.error = error?.error?.detail || 'La modification n’a pas pu être enregistrée.';
  }

  private cleanCategory(category: ShopAdminCategoryRequest | ShopAdminCategoryDto): ShopAdminCategoryRequest {
    return { name: category.name.trim(), slug: category.slug.trim(), description: this.optional(category.description), active: category.active, displayOrder: Number(category.displayOrder) || 0 };
  }

  private cleanProduct(product: ShopAdminProductRequest): ShopAdminProductRequest {
    return { ...product, name: product.name.trim(), slug: product.slug.trim(), description: this.optional(product.description), imageUrl: this.optional(product.imageUrl), displayOrder: Number(product.displayOrder) || 0 };
  }

  private cleanVariant(variant: VariantDraft): VariantDraft {
    return { ...variant, sku: variant.sku.trim(), label: this.optional(variant.label), currency: variant.currency.trim().toUpperCase(), priceAmountInCents: Number(variant.priceAmountInCents) || 0, displayOrder: Number(variant.displayOrder) || 0, stockOnHand: variant.stockTracked ? Number(variant.stockOnHand) || 0 : null };
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
