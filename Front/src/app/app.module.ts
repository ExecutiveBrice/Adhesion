import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import {NgbModule, NgbDatepickerModule, NgbCollapseModule, NgbTooltipModule, NgbModalConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './page/login/login.component';
import { ResetPasswordComponent } from './page/resetPassword/resetpassword.component';

import { RegisterComponent } from './page/register/register.component';
import { BoardAdminComponent } from './page/board-admin/board-admin.component';
import { AdherentsComponent } from './page/adherents/adherents.component';
import { ActivitesComponent } from './page/activites/activites.component';
import { BoardUserComponent } from './page/board-user/board-user.component';
import { AdhesionsComponent } from './page/adhesions/adhesions.component';
import { ContactComponent } from './page/contact/contact.component';
import { PdfViewerModule } from 'ng2-pdf-viewer';

import { OrderByPipe} from "./_helpers/sort.pipe";
import { FilterByPipe} from "./_helpers/filter.pipe";

import { authInterceptorProviders } from './_helpers/auth.interceptor';
import { NgxEditorModule } from 'ngx-editor';
import { MaillingComponent } from './page/mailling/mailling.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { MaintenanceComponent } from './page/maintenance/maintenance.component';
import { ParamTransmissionService } from './_helpers/transmission.service';


@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ResetPasswordComponent,
    RegisterComponent,
    BoardAdminComponent,
    AdherentsComponent,
    ActivitesComponent,
    BoardUserComponent,
    MaillingComponent,
    AdhesionsComponent,
    MaintenanceComponent,
    ContactComponent,
    OrderByPipe,
    FilterByPipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    NgbModule,
    NgbDatepickerModule,
    NgbCollapseModule,
    NgbTooltipModule,
    HttpClientModule,
    NgxEditorModule,
    
    PdfViewerModule,
    FontAwesomeModule
  ],
  providers: [authInterceptorProviders,
    NgbModalConfig,
    NgbModal,
    ParamTransmissionService  
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
