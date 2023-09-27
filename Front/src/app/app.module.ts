import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import {NgbModule, NgbDatepickerModule, NgbCollapseModule, NgbTooltipModule, NgbModalConfig, NgbModal, NgbCalendar } from '@ng-bootstrap/ng-bootstrap';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './page/login/login.component';
import { ResetPasswordComponent } from './page/resetPassword/resetpassword.component';

import { BoardAdminComponent } from './page/board-admin/board-admin.component';
import { AdherentsComponent } from './page/adherents/adherents.component';
import { ActivitesComponent } from './page/activites/activites.component';
import { BoardUserComponent } from './page/board-user/board-user.component';
import { AdhesionsComponent } from './page/adhesions/adhesions.component';
import { ComptaComponent } from './page/compta/compta.component';
import { ContactComponent } from './page/contact/contact.component';
import { ModalComponent } from './page/modal/modal.component';
import { PdfViewerModule } from 'ng2-pdf-viewer';



import { OrderByPipe} from "./_helpers/sort.pipe";
import { FilterByPipe} from "./_helpers/filter.pipe";
import { FilterAdhesionByPipe} from "./_helpers/filterAdhesion.pipe";

import { authInterceptorProviders } from './_helpers/auth.interceptor';
import { NgxEditorModule } from 'ngx-editor';
import { MaillingComponent } from './page/mailling/mailling.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { MaintenanceComponent } from './page/maintenance/maintenance.component';
import { ParamTransmissionService } from './_helpers/transmission.service';
import { ReportingComponent } from './page/reporting/reporting.component';
import { DatePipe } from '@angular/common';
import { ProfsComponent } from './page/profs/profs.component';
import { ExcelService } from './_services/excel.service';


@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    ResetPasswordComponent,
    BoardAdminComponent,
    AdherentsComponent,
    ActivitesComponent,
    BoardUserComponent,
    MaillingComponent,
    AdhesionsComponent,
    MaintenanceComponent,
    ContactComponent,
    ReportingComponent,
    ProfsComponent,
    ComptaComponent,
    ModalComponent,
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
    ParamTransmissionService,
    FilterAdhesionByPipe,
    ExcelService,
    DatePipe,
    FilterByPipe
    
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
