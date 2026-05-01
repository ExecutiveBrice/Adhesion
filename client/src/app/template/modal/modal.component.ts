import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal'

@Component({
  selector: 'modal',
  standalone: false,
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
	activeModal = inject(NgbActiveModal);

	@Input()
  title!: string;

  @Input()
  body!: string;

  @Input()
  accepter!: boolean;
  
	@Input()
  refuser!: boolean;

	@Input()
  consultation!: boolean;
  


}

