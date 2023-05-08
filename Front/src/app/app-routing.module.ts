import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { RegisterComponent } from './page/register/register.component';
import { LoginComponent } from './page/login/login.component';
import { ResetPasswordComponent } from './page/resetPassword/resetpassword.component';


import { BoardUserComponent } from './page/board-user/board-user.component';
import { AdherentsComponent } from './page/adherents/adherents.component';

import { ActivitesComponent } from './page/activites/activites.component';

import { BoardAdminComponent } from './page/board-admin/board-admin.component';
import { MaillingComponent } from './page/mailling/mailling.component';
import { AdhesionsComponent } from './page/adhesions/adhesions.component';
import { MaintenanceComponent } from './page/maintenance/maintenance.component';
import { ContactComponent } from './page/contact/contact.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'resetPassword/:token', component: ResetPasswordComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'inscription/:userEmail', component: BoardUserComponent },
  { path: 'adhesions', component: AdhesionsComponent },
  { path: 'adherents', component: AdherentsComponent },
  { path: 'activites', component: ActivitesComponent },
  { path: 'admin', component: BoardAdminComponent },
  { path: 'maintenance', component: MaintenanceComponent },
  { path: 'mail/:adherentId', component: MaillingComponent },

  { path: '', redirectTo: 'login', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
