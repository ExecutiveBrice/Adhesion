import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CartItemRequestDto, CartQuoteDto, PaymentSessionDto, ShopOrderDto, ShopProductDto } from '../models/shop.models';

@Injectable({ providedIn: 'root' })
export class ShopApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.server.replace(/\/$/, '')}/shop`;

  products(): Observable<ShopProductDto[]> {
    return this.http.get<ShopProductDto[]>(`${this.apiUrl}/products`);
  }

  product(productId: number): Observable<ShopProductDto> {
    return this.http.get<ShopProductDto>(`${this.apiUrl}/products/${productId}`);
  }

  quote(items: CartItemRequestDto[]): Observable<CartQuoteDto> {
    return this.http.post<CartQuoteDto>(`${this.apiUrl}/cart/quote`, { items });
  }

  createOrder(items: CartItemRequestDto[], idempotencyKey: string): Observable<ShopOrderDto> {
    return this.http.post<ShopOrderDto>(`${this.apiUrl}/orders`, { items }, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
    });
  }

  order(orderNumber: string): Observable<ShopOrderDto> {
    return this.http.get<ShopOrderDto>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}`);
  }

  createPaymentSession(orderNumber: string, idempotencyKey: string, returnUrl: string, cancelUrl: string): Observable<PaymentSessionDto> {
    return this.http.post<PaymentSessionDto>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/payment-session`,
      { returnUrl, cancelUrl }, { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) });
  }

  verifyPayment(orderNumber: string): Observable<ShopOrderDto> {
    return this.http.post<ShopOrderDto>(`${this.apiUrl}/orders/${encodeURIComponent(orderNumber)}/payment/verify`, {});
  }
}
