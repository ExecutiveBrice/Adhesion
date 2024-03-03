import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ActiviteDropDown } from 'src/app/models';

@Component({
  selector: 'modal',
  templateUrl: './modal.activite.html',
  styleUrls: ['./modal.activite.css']
})
export class ModalActivite {
	activeModal = inject(NgbActiveModal);

  @Input()
  activites!: ActiviteDropDown[];

  @Input()
  mineur!: boolean;
  
}

