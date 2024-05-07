import { Component, inject, Input } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { ActiviteDropDown } from 'src/app/models';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
@Component({
  selector: 'modal',
  templateUrl: './modal.choixactivite.html',
  styleUrls: ['./modal.choixactivite.css']
})
export class ModalChoixActivite {
	activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt=faExternalLinkSquareAlt;
  @Input()
  activites!: ActiviteDropDown[];
  @Input()
  secretaire!: boolean;
  @Input()
  admin!: boolean;
  
  opennewTab(page : string){

    window.open(page, '_blank');
  }


}

