import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ShopAdminCategoryDto, ShopAdminCategoryRequest, ShopAdminProductDto, ShopAdminProductRequest,
  ShopAdminVariantDto, ShopAdminVariantRequest
} from '../models/shop.models';

@Injectable({ providedIn: 'root' })
export class ShopAdminApiService {
  private readonly apiUrl = `${environment.server.replace(/\/$/, '')}/shop/admin`;

  constructor(private readonly http: HttpClient) {}

  products(): Observable<ShopAdminProductDto[]> { return this.http.get<ShopAdminProductDto[]>(`${this.apiUrl}/products`); }
  createProduct(request: ShopAdminProductRequest): Observable<ShopAdminProductDto> { return this.http.post<ShopAdminProductDto>(`${this.apiUrl}/products`, request); }
  updateProduct(id: number, request: ShopAdminProductRequest): Observable<ShopAdminProductDto> { return this.http.put<ShopAdminProductDto>(`${this.apiUrl}/products/${id}`, request); }
  deleteProduct(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/products/${id}`); }

  createVariant(productId: number, request: ShopAdminVariantRequest): Observable<ShopAdminVariantDto> { return this.http.post<ShopAdminVariantDto>(`${this.apiUrl}/products/${productId}/variants`, request); }
  updateVariant(id: number, request: ShopAdminVariantRequest): Observable<ShopAdminVariantDto> { return this.http.put<ShopAdminVariantDto>(`${this.apiUrl}/variants/${id}`, request); }
  deleteVariant(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/variants/${id}`); }

  categories(): Observable<ShopAdminCategoryDto[]> { return this.http.get<ShopAdminCategoryDto[]>(`${this.apiUrl}/categories`); }
  createCategory(request: ShopAdminCategoryRequest): Observable<ShopAdminCategoryDto> { return this.http.post<ShopAdminCategoryDto>(`${this.apiUrl}/categories`, request); }
  updateCategory(id: number, request: ShopAdminCategoryRequest): Observable<ShopAdminCategoryDto> { return this.http.put<ShopAdminCategoryDto>(`${this.apiUrl}/categories/${id}`, request); }
  deleteCategory(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/categories/${id}`); }
}
