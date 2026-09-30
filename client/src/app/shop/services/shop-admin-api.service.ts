import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ShopAdminCategoryDto, ShopAdminCategoryRequest, ShopAdminOrderDto, ShopAdminOrderItemDto,
  ShopAdminOrderItemStatus, ShopAdminProductDto, ShopAdminProductRequest,
  ShopAdminVariantDto, ShopAdminVariantRequest, ShopOrderMessageDto
} from '../models/shop.models';

@Injectable({ providedIn: 'root' })
export class ShopAdminApiService {
  private readonly apiUrl = `${environment.server.replace(/\/$/, '')}/shop/admin`;

  constructor(private readonly http: HttpClient) {}

  orders(): Observable<ShopAdminOrderDto[]> { return this.http.get<ShopAdminOrderDto[]>(`${this.apiUrl}/orders`); }
  updateOrderStatus(orderNumber: string, status: ShopAdminOrderDto['status']): Observable<ShopAdminOrderDto> {
    return this.http.put<ShopAdminOrderDto>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/status`, { status });
  }
  updateOrderItemStatus(orderNumber: string, itemId: number, status: ShopAdminOrderItemStatus): Observable<ShopAdminOrderItemDto> {
    return this.http.put<ShopAdminOrderItemDto>(
      `${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/items/${itemId}/status`, { status });
  }
  conversation(orderNumber: string): Observable<ShopOrderMessageDto[]> {
    return this.http.get<ShopOrderMessageDto[]>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/conversation`);
  }
  sendConversationMessage(orderNumber: string, content: string): Observable<ShopOrderMessageDto> {
    return this.http.post<ShopOrderMessageDto>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/conversation`, { content });
  }

  products(): Observable<ShopAdminProductDto[]> { return this.http.get<ShopAdminProductDto[]>(`${this.apiUrl}/products`); }
  uploadProductImage(file: File): Observable<string> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<{ fileName: string }>(`${this.apiUrl}/product-images`, data).pipe(
      map(response => `${environment.server.replace(/\/$/, '')}/shop/product-images/${encodeURIComponent(response.fileName)}`)
    );
  }
  createProduct(request: ShopAdminProductRequest): Observable<ShopAdminProductDto> { return this.http.post<ShopAdminProductDto>(`${this.apiUrl}/products`, request); }
  updateProduct(id: number, request: ShopAdminProductRequest): Observable<ShopAdminProductDto> { return this.http.put<ShopAdminProductDto>(`${this.apiUrl}/products/${id}`, request); }
  deleteProduct(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/products/${id}`); }

  createVariant(productId: number, request: ShopAdminVariantRequest): Observable<ShopAdminVariantDto> { return this.http.post<ShopAdminVariantDto>(`${this.apiUrl}/products/${productId}/variants`, request); }
  updateVariant(id: number, request: ShopAdminVariantRequest): Observable<ShopAdminVariantDto> { return this.http.put<ShopAdminVariantDto>(`${this.apiUrl}/variants/${id}`, request); }
  updateVariantStock(id: number, stockOnHand: number, expectedVersion: number): Observable<ShopAdminVariantDto> {
    return this.http.put<ShopAdminVariantDto>(`${this.apiUrl}/variants/${id}/stock`, { stockOnHand, expectedVersion });
  }
  deleteVariant(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/variants/${id}`); }

  categories(): Observable<ShopAdminCategoryDto[]> { return this.http.get<ShopAdminCategoryDto[]>(`${this.apiUrl}/categories`); }
  createCategory(request: ShopAdminCategoryRequest): Observable<ShopAdminCategoryDto> { return this.http.post<ShopAdminCategoryDto>(`${this.apiUrl}/categories`, request); }
  updateCategory(id: number, request: ShopAdminCategoryRequest): Observable<ShopAdminCategoryDto> { return this.http.put<ShopAdminCategoryDto>(`${this.apiUrl}/categories/${id}`, request); }
  deleteCategory(id: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/categories/${id}`); }
}
