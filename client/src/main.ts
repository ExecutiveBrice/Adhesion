import { enableProdMode } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { environment } from './environments/environment';
import { restoredShopPaymentUrl } from './app/shop/shop-payment-return';

const paymentReturnUrl = restoredShopPaymentUrl(window.location.href);
if (paymentReturnUrl) window.history.replaceState(window.history.state, '', paymentReturnUrl);

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, appConfig)
  .catch(err => console.error(err));
