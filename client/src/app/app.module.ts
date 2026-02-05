import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule, NgbDatepickerModule, NgbCollapseModule, NgbTooltipModule, NgbModalConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './page/login/login.component';
import { ResetPasswordComponent } from './page/resetPassword/resetpassword.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BoardAdminComponent } from './page/board-admin/board-admin.component';
import { AdherentsComponent } from './page/adherents/adherents.component';
import { ActivitesComponent } from './page/activites/activites.component';
import { BoardUserComponent } from './page/board-user/board-user.component';
import { AdhesionsComponent } from './page/adhesions/adhesions.component';
import { ComptaComponent } from './page/compta/compta.component';
import { LOCALE_ID } from '@angular/core';

import { ModalComponent } from './template/modal/modal.component';
import { ModalPDFComponent } from './template/modal-pdf/modal-pdf.component';
import { ModalChoixActivite } from './template/modal-choixactivite/modal.choixactivite';
import { ModalActivite } from './template/modal-activite/modal.activite';

import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { ngfModule } from "angular-file"

import { ToastrModule } from 'ngx-toastr';
import { UserComponent } from './template/user/user.component';
import { SwitchComponent } from './template/switch/swtch';

import { OrderByPipe} from "./_helpers/sort.pipe";
import { FilterByPipe} from "./_helpers/filter.pipe";
import { FilterAdhesionByPipe} from "./_helpers/filterAdhesion.pipe";

import { authInterceptorProviders } from './_helpers/auth.interceptor';

import { MaillingComponent } from './page/mailling/mailling.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { MaintenanceComponent } from './page/maintenance/maintenance.component';
import { ParamTransmissionService } from './_helpers/transmission.service';
import { ReportingComponent } from './page/reporting/reporting.component';
import { DatePipe, registerLocaleData } from '@angular/common';

import { AngularEditorModule } from '@wfpena/angular-wysiwyg';


import { ProfsComponent } from './page/profs/profs.component';
import { ExcelService } from './_services/excel.service';
import { UtilService } from './_services/util.service';
import { FileService } from './_services/file.service';
import { SimpleFilterPipe } from './_helpers/simpleFilter.pipe';
import {OrderSimplePipe} from "./_helpers/sort-simple.pipe";
import {OrderObjectByPipe} from "./_helpers/sortObject.pipe";
import localeFr from '@angular/common/locales/fr';
registerLocaleData(localeFr);

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
    ReportingComponent,
    ProfsComponent,
    ComptaComponent,
    ModalComponent,
    ModalPDFComponent,
    ModalChoixActivite,
    ModalActivite,
    UserComponent,
    SwitchComponent,
    OrderByPipe,
    FilterByPipe,

    SimpleFilterPipe
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

    BrowserAnimationsModule,
    NgxExtendedPdfViewerModule,
    FontAwesomeModule,
    ngfModule,
    AngularEditorModule,
    ToastrModule.forRoot({
      enableHtml: true,
      timeOut: 10000,
      closeButton: true,
      tapToDismiss: true,
      progressBar: true,
      positionClass: 'toast-top-left-wide',
      preventDuplicates: true,
    }),

    OrderSimplePipe,
    FilterAdhesionByPipe,
    ReactiveFormsModule,
    OrderObjectByPipe,
  ],
  providers: [{ provide: LOCALE_ID, useValue: "fr-FR" },
    authInterceptorProviders,
    NgbModalConfig,
    NgbModal,
    ParamTransmissionService,
    FilterAdhesionByPipe,
    ExcelService,
    UtilService,
    FileService,
    DatePipe,
    FilterByPipe

  ],
  bootstrap: [AppComponent]
})

export class AppModule { }
