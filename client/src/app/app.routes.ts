import { inject } from '@angular/core';
import { memberAccessGuard } from './_helpers/member-access.guard';
import { Routes } from '@angular/router';
import { shopManagerGuard } from './shop/shop-manager.guard';
import { shopAuthGuard } from './shop/shop-auth.guard';
import { chatAuthGuard } from './page/chat/chat-auth.guard';

export const routes: Routes = [
  { path: 'profil/nouveau', canActivate: [memberAccessGuard], data: { roles: [], nouveau: true }, title: 'Ajouter un adhérent', loadComponent: () => import('./page/profil/profil.component').then((m) => m.ProfilComponent) },
  { path: 'profil/:tribuUuid', canActivate: [memberAccessGuard], data: { roles: [] }, title: 'Profil adhérent', loadComponent: () => import('./page/profil/profil.component').then((m) => m.ProfilComponent) },
  { path: 'profil', canActivate: [memberAccessGuard], data: { roles: [] }, title: 'Mon profil', loadComponent: () => import('./page/profil/profil.component').then((m) => m.ProfilComponent) },
  { path: 'agenda', canActivate: [memberAccessGuard], data: { roles: [] }, title: 'Agenda', loadComponent: () => import('./page/agenda/agenda.component').then((m) => m.AgendaComponent) },
  { path: 'accueil', canActivate: [memberAccessGuard], data: { roles: [] }, title: 'Accueil', loadComponent: () => import('./page/accueil/accueil.component').then((m) => m.AccueilComponent) },
  { path: 'chat', title: 'Chat', canActivate: [chatAuthGuard], loadComponent: () => import('./page/chat/chat.component').then(m => m.ChatComponent) },
  { path: 'boutique/gestion', title: 'Gestion de la boutique', canActivate: [shopAuthGuard, shopManagerGuard], loadComponent: () => import('./shop/pages/shop-management.component').then((m) => m.ShopManagementComponent) },
  { path: 'boutique', title: 'Boutique', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-catalog.component').then((m) => m.ShopCatalogComponent) },
  { path: 'boutique/panier', title: 'Panier', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-cart.component').then((m) => m.ShopCartComponent) },
  { path: 'boutique/checkout', title: 'Finaliser la commande', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-checkout.component').then((m) => m.ShopCheckoutComponent) },
  { path: 'boutique/produits/:id', title: 'Produit', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-product-detail.component').then((m) => m.ShopProductDetailComponent) },
  { path: 'boutique/commandes', title: 'Mes commandes', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-orders.component').then((m) => m.ShopOrdersComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement-reussi', title: 'Paiement réussi', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-success.component').then((m) => m.ShopPaymentSuccessComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement-echoue', title: 'Paiement échoué', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-status.component').then((m) => m.ShopPaymentStatusComponent) },
  { path: 'boutique/commandes/:orderNumber/paiement', title: 'Vérification du paiement', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-payment-status.component').then((m) => m.ShopPaymentStatusComponent) },
  { path: 'boutique/commandes/:orderNumber/messages', title: 'Demande de remboursement', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-order-conversation.component').then((m) => m.ShopOrderConversationComponent) },
  { path: 'boutique/commandes/:orderNumber', title: 'Commande', canActivate: [shopAuthGuard], loadComponent: () => import('./shop/pages/shop-order-detail.component').then((m) => m.ShopOrderDetailComponent) },
  { path: 'login', title: 'Connexion', loadComponent: () => import('./page/login/login.component').then((m) => m.LoginComponent) },
  { path: 'resetPassword/:token', title: 'Réinitialisation du mot de passe', loadComponent: () => import('./page/resetPassword/resetpassword.component').then((m) => m.ResetPasswordComponent) },
  { path: 'inscription/:tribuUuid', redirectTo: 'profil/:tribuUuid', pathMatch: 'full' },
  { path: 'inscription', redirectTo: 'profil', pathMatch: 'full' },
  { path: 'adhesions', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_SECRETAIRE'] }, title: 'Adhésions', loadComponent: () => import('./page/adhesions/adhesions.component').then((m) => m.AdhesionsComponent) },
  { path: 'adherents', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_SECRETAIRE'] }, title: 'Adhérents', loadComponent: () => import('./page/adherents/adherents.component').then((m) => m.AdherentsComponent) },
  { path: 'activites', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_SECRETAIRE'] }, title: 'Activités', loadComponent: () => import('./page/activites/activites.component').then((m) => m.ActivitesComponent) },
  { path: 'admin', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN'] }, title: 'Administration', loadComponent: () => import('./page/board-admin/board-admin.component').then((m) => m.BoardAdminComponent) },
  { path: 'maintenance', title: 'Maintenance', loadComponent: () => import('./page/maintenance/maintenance.component').then((m) => m.MaintenanceComponent) },
  { path: 'reporting', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_SECRETAIRE', 'ROLE_MEMBRECA', 'ROLE_BUREAU'] }, title: 'Reporting', loadComponent: () => import('./page/reporting/reporting.component').then((m) => m.ReportingComponent) },
  { path: 'profs', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ENCADRANT'] }, title: 'Mes équipes', loadComponent: () => import('./page/profs/profs.component').then((m) => m.ProfsComponent) },
  { path: 'seances', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ENCADRANT', 'ROLE_REFERENT_ACTIVITE'] }, title: 'Séances', loadComponent: () => import('./page/seances/seances.component').then((m) => m.SeancesComponent) },
  { path: 'seances-secretariat', canActivate: [memberAccessGuard], data: { roles: ['ROLE_SECRETAIRE'] }, title: 'Séances (secrétariat)', loadComponent: () => import('./page/seances-secretariat/seances-secretariat.component').then((m) => m.SeancesSecretariatComponent) },
  { path: 'mail/:adherentId', canActivate: [memberAccessGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_SECRETAIRE'] }, title: 'Mailing', loadComponent: () => import('./page/mailling/mailling.component').then((m) => m.MaillingComponent) },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
