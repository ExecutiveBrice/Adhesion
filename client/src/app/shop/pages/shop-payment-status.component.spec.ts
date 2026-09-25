import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { ShopPaymentStatusComponent } from './shop-payment-status.component';

describe('ShopPaymentStatusComponent', () => {
  it('revérifie le paiement auprès du serveur tant que la commande est en attente', fakeAsync(() => {
    const order = { orderNumber: 'CMD-2026-000007', status: 'PENDING_PAYMENT' } as ShopOrderDto;
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['verifyPayment', 'order']);
    api.verifyPayment.and.returnValue(of(order));

    TestBed.configureTestingModule({
      imports: [ShopPaymentStatusComponent],
      providers: [
        provideRouter([]),
        { provide: ShopApiService, useValue: api },
        { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: convertToParamMap({ orderNumber: order.orderNumber }), routeConfig: { path: 'paiement' } }
        } }
      ]
    });
    TestBed.overrideComponent(ShopPaymentStatusComponent, { set: { template: '' } });

    const fixture = TestBed.createComponent(ShopPaymentStatusComponent);
    fixture.detectChanges();
    tick();
    expect(api.verifyPayment).toHaveBeenCalledTimes(1);

    tick(5000);
    expect(api.verifyPayment).toHaveBeenCalledTimes(2);
    expect(api.verifyPayment).toHaveBeenCalledWith(order.orderNumber);
    expect(api.order).not.toHaveBeenCalled();

    fixture.destroy();
  }));

  it('arrête les vérifications automatiques après une erreur 502 tout en permettant un nouvel essai manuel', fakeAsync(() => {
    const order = { orderNumber: 'CMD-2026-000008', status: 'PENDING_PAYMENT' } as ShopOrderDto;
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['verifyPayment', 'order']);
    api.verifyPayment.and.returnValue(throwError(() => new HttpErrorResponse({ status: 502 })));
    api.order.and.returnValue(of(order));

    TestBed.configureTestingModule({
      imports: [ShopPaymentStatusComponent],
      providers: [
        provideRouter([]),
        { provide: ShopApiService, useValue: api },
        { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: convertToParamMap({ orderNumber: order.orderNumber }), routeConfig: { path: 'paiement' } }
        } }
      ]
    });
    TestBed.overrideComponent(ShopPaymentStatusComponent, { set: { template: '' } });

    const fixture = TestBed.createComponent(ShopPaymentStatusComponent);
    fixture.detectChanges();
    tick();
    expect(api.verifyPayment).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.error()).toContain('Vérifier maintenant');

    tick(5000);
    expect(api.verifyPayment).toHaveBeenCalledTimes(1);
    void fixture.componentInstance.verify();
    tick();
    expect(api.verifyPayment).toHaveBeenCalledTimes(2);

    fixture.destroy();
  }));
});
