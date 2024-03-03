import { Injectable, inject } from "@angular/core";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ModalComponent } from "../template/modal/modal.component";
import { ModalPDFComponent } from "../template/modal-pdf/modal-pdf.component";
import { Activite, ActiviteDropDown, Document } from "../models";
import { ModalActivite } from "../template/modal-activite/modal.activite";

@Injectable({
  providedIn: 'root'
})
export class UtilService {

  private modalService = inject(NgbModal);

  public openModal(body: string, title: string, accepter: boolean, refuser: boolean, consultation: boolean, size: string): Promise<any> {
    const modalRef = this.modalService.open(ModalComponent, {
      animation: true,
      size: size,
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.body = body;
    modalRef.componentInstance.title = title;
    modalRef.componentInstance.accepter = accepter;
    modalRef.componentInstance.refuser = refuser;
    modalRef.componentInstance.consultation = consultation;
    return modalRef.result;
  }

  openModalPDF(documentName: string | undefined, adherentId:number): Promise<any> {
    const modalRef = this.modalService.open(ModalPDFComponent, {
      animation: true,
      size: 'xl',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.documentName = documentName;
    modalRef.componentInstance.adherentId = adherentId;
    return modalRef.result;
  }


  openModalActivite(activites : ActiviteDropDown[]): Promise<any> {
    const modalRef = this.modalService.open(ModalActivite, {
      animation: true,
      size: 'md',
      centered: true,
      backdrop: 'static',
      scrollable: true
    });
    modalRef.componentInstance.activites = activites;

    return modalRef.result;
  }
}