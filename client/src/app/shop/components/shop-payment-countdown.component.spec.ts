import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ShopPaymentCountdownComponent } from './shop-payment-countdown.component';

describe('ShopPaymentCountdownComponent', () => {
  beforeAll(() => registerLocaleData(localeFr));

  it('affiche le temps restant puis signale que le délai est écoulé', fakeAsync(() => {
    TestBed.configureTestingModule({ imports: [ShopPaymentCountdownComponent] });
    const fixture = TestBed.createComponent(ShopPaymentCountdownComponent);
    fixture.componentInstance.paymentExpiresAt = new Date(Date.now() + 2500).toISOString();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Temps restant pour payer');
    expect(fixture.componentInstance.isExpired()).toBeFalse();

    tick(3000);
    fixture.detectChanges();
    expect(fixture.componentInstance.isExpired()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Délai de paiement écoulé');
  }));

  it('masque le chrono pour une commande déjà réglée', () => {
    TestBed.configureTestingModule({ imports: [ShopPaymentCountdownComponent] });
    const fixture = TestBed.createComponent(ShopPaymentCountdownComponent);
    fixture.componentInstance.paymentExpiresAt = new Date(Date.now() + 60_000).toISOString();
    fixture.componentInstance.status = 'PAID';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });
});
