import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ActiviteDropDown } from 'src/app/models';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
@Component({
  selector: 'modal',
  templateUrl: './modal.activite.html',
  styleUrls: ['./modal.activite.css']
})
export class ModalActivite {
	activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt=faExternalLinkSquareAlt;
  @Input()
  activites!: ActiviteDropDown[];



  opennewTab(page : string){

    window.open(page, '_blank');
  }


}

