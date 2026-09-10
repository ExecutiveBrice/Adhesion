import { ApplicationConfig, importProvidersFrom, LOCALE_ID } from '@angular/core';
import { DatePipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withHashLocation } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { NgbModal, NgbModalConfig, NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { routes } from './app.routes';
import { apiChangeDetectionInterceptor, authInterceptorProviders } from './_helpers/auth.interceptor';
import { environment } from '../environments/environment';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(
      NgbModule,
    ),
    provideHttpClient(
      withInterceptors([apiChangeDetectionInterceptor]),
      withInterceptorsFromDi()
    ),
    provideAnimations(),
    provideRouter(routes, withHashLocation()),
    provideServiceWorker('ngsw-worker.js', {
      enabled: environment.production && isMobileDevice(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    authInterceptorProviders,
    NgbModalConfig,
    NgbModal,
    DatePipe,
  ],
};

function isMobileDevice(): boolean {
  if (typeof navigator === 'undefined') {
    return false;
  }

  return /Android|iPhone|iPad|iPod|IEMobile|Opera Mini/i.test(navigator.userAgent)
    || (navigator.maxTouchPoints > 1 && window.matchMedia('(pointer: coarse)').matches);
}
