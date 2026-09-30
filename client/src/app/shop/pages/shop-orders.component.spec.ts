import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { ShopOrdersComponent } from './shop-orders.component';

describe('ShopOrdersComponent', () => {
  it('annule une commande en attente de paiement depuis la liste', () => {
    const pendingOrder = {
      orderNumber: 'CMD-2026-000001', status: 'PENDING_PAYMENT'
    } as ShopOrderDto;
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['orders', 'cancelOrder']);
    api.orders.and.returnValue(of([pendingOrder]));
    api.cancelOrder.and.returnValue(of({ ...pendingOrder, status: 'CANCELLED' }));
    spyOn(window, 'confirm').and.returnValue(true);

    TestBed.configureTestingModule({
      imports: [ShopOrdersComponent],
      providers: [provideRouter([]), { provide: ShopApiService, useValue: api }]
    });
    TestBed.overrideComponent(ShopOrdersComponent, { set: { template: '' } });
    const fixture = TestBed.createComponent(ShopOrdersComponent);
    fixture.detectChanges();

    fixture.componentInstance.cancel(pendingOrder);

    expect(api.cancelOrder).toHaveBeenCalledWith('CMD-2026-000001');
    expect(fixture.componentInstance.orders()[0].status).toBe('CANCELLED');
  });

  it('ne propose aucune annulation locale pour une commande déjà payée', () => {
    const paidOrder = { orderNumber: 'CMD-2026-000002', status: 'PAID' } as ShopOrderDto;
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['orders', 'cancelOrder']);
    api.orders.and.returnValue(of([paidOrder]));

    TestBed.configureTestingModule({
      imports: [ShopOrdersComponent],
      providers: [provideRouter([]), { provide: ShopApiService, useValue: api }]
    });
    TestBed.overrideComponent(ShopOrdersComponent, { set: { template: '' } });
    const fixture = TestBed.createComponent(ShopOrdersComponent);
    fixture.detectChanges();

    fixture.componentInstance.cancel(paidOrder);

    expect(api.cancelOrder).not.toHaveBeenCalled();
  });
});
