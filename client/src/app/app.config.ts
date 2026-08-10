import { ApplicationConfig, importProvidersFrom, LOCALE_ID } from '@angular/core';
import { DatePipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withHashLocation } from '@angular/router';
import { NgbModal, NgbModalConfig, NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { routes } from './app.routes';
import { authInterceptorProviders } from './_helpers/auth.interceptor';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(
      NgbModule,
    ),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    provideRouter(routes, withHashLocation()),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    authInterceptorProviders,
    NgbModalConfig,
    NgbModal,
    DatePipe,
  ],
};
