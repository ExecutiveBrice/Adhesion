import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';
import { ShopApiService } from '../services/shop-api.service';
import { ShopOrderDetailComponent } from './shop-order-detail.component';
import { ShopOrdersComponent } from './shop-orders.component';

describe('URL de retour du paiement sans fragment', () => {
  const order = { orderNumber: 'CMD-2026-000001', status: 'PENDING_PAYMENT' } as ShopOrderDto;

  ['https://asso.example/', 'https://asso.example/adhesion/'].forEach(baseURI => {
    ['liste', 'détail'].forEach(page => {
      it(`construit les URL depuis la racine de déploiement pour ${page} (${baseURI})`, async () => {
        const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['order', 'createPaymentSession']);
        api.order.and.returnValue(of(order));
        // Stop before leaving the test page for the payment provider.
        api.createPaymentSession.and.returnValue(throwError(() => new Error('Paiement simulé')));
        TestBed.configureTestingModule({ providers: [
          { provide: DOCUMENT, useValue: { baseURI } },
          { provide: ShopApiService, useValue: api },
          { provide: Router, useValue: {} },
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['orderNumber', order.orderNumber]]) } } }
        ] });

        if (page === 'liste') {
          const component = TestBed.runInInjectionContext(() => new ShopOrdersComponent());
          await component.pay(order);
        } else {
          const component = TestBed.runInInjectionContext(() => new ShopOrderDetailComponent());
          component.ngOnInit();
          await component.pay();
        }

        expect(api.createPaymentSession).toHaveBeenCalledOnceWith(
          order.orderNumber,
          jasmine.any(String),
          `${baseURI}boutique/commandes/${order.orderNumber}/paiement`,
          `${baseURI}boutique/commandes/${order.orderNumber}/paiement-echoue`
        );
      });
    });
  });
});
