import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./page/login/login.component').then((m) => m.LoginComponent) },
  { path: 'resetPassword/:token', loadComponent: () => import('./page/resetPassword/resetpassword.component').then((m) => m.ResetPasswordComponent) },
  { path: 'inscription/:tribuUuid', loadComponent: () => import('./page/board-user/board-user.component').then((m) => m.BoardUserComponent) },
  { path: 'inscription', loadComponent: () => import('./page/board-user/board-user.component').then((m) => m.BoardUserComponent) },
  { path: 'adhesions', loadComponent: () => import('./page/adhesions/adhesions.component').then((m) => m.AdhesionsComponent) },
  { path: 'adherents', loadComponent: () => import('./page/adherents/adherents.component').then((m) => m.AdherentsComponent) },
  { path: 'activites', loadComponent: () => import('./page/activites/activites.component').then((m) => m.ActivitesComponent) },
  { path: 'admin', loadComponent: () => import('./page/board-admin/board-admin.component').then((m) => m.BoardAdminComponent) },
  { path: 'maintenance', loadComponent: () => import('./page/maintenance/maintenance.component').then((m) => m.MaintenanceComponent) },
  { path: 'reporting', loadComponent: () => import('./page/reporting/reporting.component').then((m) => m.ReportingComponent) },
  { path: 'compta', loadComponent: () => import('./page/compta/compta.component').then((m) => m.ComptaComponent) },
  { path: 'profs', loadComponent: () => import('./page/profs/profs.component').then((m) => m.ProfsComponent) },
  { path: 'seances', loadComponent: () => import('./page/seances/seances.component').then((m) => m.SeancesComponent) },
  { path: 'mail/:adherentId', loadComponent: () => import('./page/mailling/mailling.component').then((m) => m.MaillingComponent) },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
