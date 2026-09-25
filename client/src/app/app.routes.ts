import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { TokenStorageService } from './_services/token-storage.service';
import { shopManagerGuard } from './shop/shop-manager.guard';
import { shopAuthGuard } from './shop/shop-auth.guard';

const inscriptionTitle = () => inject(TokenStorageService).getUser().roles?.includes('ROLE_SECRETAIRE')
  ? 'Inscriptions manuelles'
  : 'Mes adhésions';

export const routes: Routes = [
  { path: 'boutique/gestion', title: 'Gestion de la boutique', canActivate: [shopAuthGuard, shopManagerGuard], loadComponent: () => import('./shop/pages/shop-management.component').then((m) => m.ShopManagementComponent) },
  { path: 'boutique', title: 'Boutique', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-catalog.component').then((m) => m.ShopCatalogComponent) },
  { path: 'boutique/panier', title: 'Panier', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-cart.component').then((m) => m.ShopCartComponent) },
  { path: 'boutique/checkout', title: 'Finaliser la commande', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-checkout.component').then((m) => m.ShopCheckoutComponent) },
  { path: 'boutique/produits/:id', title: 'Produit', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-product-detail.component').then((m) => m.ShopProductDetailComponent) },
  { path: 'boutique/commandes', title: 'Mes commandes', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-orders.component').then((m) => m.ShopOrdersComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement-reussi', title: 'Paiement réussi', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-success.component').then((m) => m.ShopPaymentSuccessComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement-echoue', title: 'Paiement échoué', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-status.component').then((m) => m.ShopPaymentStatusComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement', title: 'Vérification du paiement', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-status.component').then((m) => m.ShopPaymentStatusComponent) },
  { path: 'boutique/commandes/:orderNumber', title: 'Commande', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-order-detail.component').then((m) => m.ShopOrderDetailComponent) },
  { path: 'login', title: 'Connexion', loadComponent: () => import('./page/login/login.component').then((m) => m.LoginComponent) },
  { path: 'resetPassword/:token', title: 'Réinitialisation du mot de passe', loadComponent: () => import('./page/resetPassword/resetpassword.component').then((m) => m.ResetPasswordComponent) },
  { path: 'inscription/:tribuUuid', title: inscriptionTitle, loadComponent: () => import('./page/board-user/board-user.component').then((m) => m.BoardUserComponent) },
  { path: 'inscription', title: inscriptionTitle, loadComponent: () => import('./page/board-user/board-user.component').then((m) => m.BoardUserComponent) },
  { path: 'adhesions', title: 'Adhésions', loadComponent: () => import('./page/adhesions/adhesions.component').then((m) => m.AdhesionsComponent) },
  { path: 'adherents', title: 'Adhérents', loadComponent: () => import('./page/adherents/adherents.component').then((m) => m.AdherentsComponent) },
  { path: 'activites', title: 'Activités', loadComponent: () => import('./page/activites/activites.component').then((m) => m.ActivitesComponent) },
  { path: 'admin', title: 'Administration', loadComponent: () => import('./page/board-admin/board-admin.component').then((m) => m.BoardAdminComponent) },
  { path: 'maintenance', title: 'Maintenance', loadComponent: () => import('./page/maintenance/maintenance.component').then((m) => m.MaintenanceComponent) },
  { path: 'reporting', title: 'Reporting', loadComponent: () => import('./page/reporting/reporting.component').then((m) => m.ReportingComponent) },
  { path: 'compta', title: 'Compta', loadComponent: () => import('./page/compta/compta.component').then((m) => m.ComptaComponent) },
  { path: 'profs', title: 'Mes équipes', loadComponent: () => import('./page/profs/profs.component').then((m) => m.ProfsComponent) },
  { path: 'seances', title: 'Séances', loadComponent: () => import('./page/seances/seances.component').then((m) => m.SeancesComponent) },
  { path: 'seances-secretariat', title: 'Séances (secrétariat)', loadComponent: () => import('./page/seances-secretariat/seances-secretariat.component').then((m) => m.SeancesSecretariatComponent) },
  { path: 'mail/:adherentId', title: 'Mailing', loadComponent: () => import('./page/mailling/mailling.component').then((m) => m.MaillingComponent) },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
