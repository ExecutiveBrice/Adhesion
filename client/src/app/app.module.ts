import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
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
import { ContactComponent } from './page/contact/contact.component';
import { ModalComponent } from './template/modal/modal.component';
import { ModalPDFComponent } from './template/modal-pdf/modal-pdf.component';
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
import { NgxEditorModule } from 'ngx-editor';
import { MaillingComponent } from './page/mailling/mailling.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { MaintenanceComponent } from './page/maintenance/maintenance.component';
import { ParamTransmissionService } from './_helpers/transmission.service';
import { ReportingComponent } from './page/reporting/reporting.component';
import { DatePipe } from '@angular/common';
import { ProfsComponent } from './page/profs/profs.component';
import { ExcelService } from './_services/excel.service';
import { UtilService } from './_services/util.service';
import { FileService } from './_services/file.service';
import { SimpleFilterPipe } from './_helpers/simpleFilter.pipe';



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
    ModalPDFComponent,
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
    NgxEditorModule,
    BrowserAnimationsModule,
    NgxExtendedPdfViewerModule,
    FontAwesomeModule,
    ngfModule,
    ToastrModule.forRoot({
      enableHtml: true,
      timeOut: 5000,
      closeButton:true,
      tapToDismiss:true,
      progressBar: true,
      positionClass: 'toast-top-left-wide',
      preventDuplicates: true,
    }),
  ],
  providers: [authInterceptorProviders,
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
