import { Component, inject, Input, OnInit } from '@angular/core'
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'
import { Activite, ActiviteDropDown, Adherent, AdherentLite } from 'src/app/models';
import { faExternalLinkSquareAlt } from '@fortawesome/free-solid-svg-icons';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ActiviteService } from 'src/app/_services/activite.service';
import { faCircleQuestion, faEnvelope, faCircleXmark, faCloudDownloadAlt, faBook, faScaleBalanced, faPencilSquare, faSquarePlus, faSquareMinus, faCircleCheck, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import {DayOfWeek} from "./dayOfWeek";

@Component({
  selector: 'modal',
  templateUrl: './modal.activite.html',
  styleUrls: ['./modal.activite.css']
})
export class ModalActivite implements OnInit {
  faCircleQuestion = faCircleQuestion
  faEnvelope = faEnvelope;
  faCircleXmark = faCircleXmark;
  faCloudDownloadAlt = faCloudDownloadAlt;
  faScaleBalanced = faScaleBalanced;
  faBook = faBook;
  faUserPlus = faUserPlus;
  faCircleCheck = faCircleCheck;
  faSquareMinus = faSquareMinus;
  faPencilSquare = faPencilSquare;
  faSquarePlus = faSquarePlus;


  activeModal = inject(NgbActiveModal);
  faExternalLinkSquareAlt = faExternalLinkSquareAlt;
  @Input()
  activite!: Activite;

  profs: AdherentLite[] = []

  constructor(
    private toastr: ToastrService,
    private adherentService: AdherentService,
    public activiteService: ActiviteService,
  ) { }

  ngOnInit(): void {

    this.getProfs();
  }


  enregistrer() {

    this.activiteService.save(this.activite).subscribe(
      data => {
        this.showSucces(data.nom + " " + data.horaire)
        this.activeModal.close('valider')
      },
      err => {
        this.showError(err.message)
      }
    );
  }


  ajouterProf(newActivite: Activite, adherent: Adherent) {
    newActivite.profs.push(adherent);
  }

  retirerProf(newActivite: Activite, adherent: Adherent) {
    newActivite.profs = newActivite.profs.filter(prof => prof.id != adherent.id);
  }


  getProfs() {
    this.adherentService.getByRole(3).subscribe(
      data => {
        this.profs = data;
      },
      err => {
        this.showError(err.message)
      }
    );

  }

  showSucces(message: string) {
    this.toastr.success(message, 'Enregistrement réussi pour l\'activité');
  }
  showWarning(message: string) {
    this.toastr.warning(message, 'Veuillez patienter,');
  }
  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }

   getEnumKeys<DayOfWeek>(): string[] {
    return Object.keys(DayOfWeek).filter(k => isNaN(Number(k)));
  }
  protected readonly DayOfWeek = DayOfWeek;
}

