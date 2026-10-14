import { Component, inject, ViewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ZonagesComponent } from '../zonages/zonages.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({
  selector: 'app-zonages-modal',
  imports: [ZonagesComponent],
  templateUrl: './zonages-modal.component.html'
})
export class ZonagesModalComponent {
  private readonly apiViewRefresh = registerApiViewRefresh();
  readonly activeModal = inject(NgbActiveModal);
  @ViewChild(ZonagesComponent) zonages?: ZonagesComponent;

  get enregistrement(): boolean { return this.zonages?.enregistrement ?? false; }
}
