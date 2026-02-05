import {Component, OnDestroy, OnInit} from '@angular/core';
import {MailService} from '../../_services/mail.service';
import {DomSanitizer} from '@angular/platform-browser';
import {Activite, Email, Groupe} from '../../models';
import {Router, ActivatedRoute} from '@angular/router';

import {ActiviteService} from 'src/app/_services/activite.service';
import {ParamService} from 'src/app/_services/param.service';
import {ToastrService} from 'ngx-toastr';
import {Historique} from "../../models/historique";

import {FormArray, FormBuilder, FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-mailling',
  templateUrl: './mailling.component.html',
  styleUrls: ['./mailling.component.scss']
})

export class MaillingComponent implements OnInit {

  inProgress: boolean = false;
  maillingListe!: FormArray;
  subject: string = "";


  template: number = 0;
  historiques: Historique[] = []

  content = '<p>Bonjour 👋</p>';

  editorConfig = {
    minHeight: '400px',
    editable: true,
    spellcheck: true,
    placeholder: 'Écris ton mail ici…',
    imageResizeSensitivity: 10,
    toolbar: [
      'bold',
      'italic',
      'underline',
      'strikeThrough',
      'orderedList',
      'unorderedList',
      'link',
      'image', // 👈 bouton image
      'html'
    ],


  };

  constructor(
    private toastr: ToastrService,
    public fb: FormBuilder,
    public activiteService: ActiviteService,
    public router: Router,
    public mailService: MailService,
    public paramService: ParamService) {
  }



  ngOnInit(): void {
    this.maillingListe = this.fb.array([])
    const horairesForm: FormArray = this.fb.array([])
    const adherentForm: FormGroup = this.fb.group({
      ordre: [1, []],
      id: [1, []],
      nom: ['adherent', []],
      text: ['Tous les adherents', []],
      bold: [false, []],
      indent: [false, []],
      checked: [false, []]
    });
    horairesForm.push(adherentForm)

    const bureauForm: FormGroup = this.fb.group({
      ordre: [2, []],
      id: [4, []],
      nom: ['bureau', []],
      text: ['Les membres du bureau', []],
      bold: [false, []],
      indent: [false, []],
      checked: [false, []]
    });
    horairesForm.push(bureauForm)
    const caForm: FormGroup = this.fb.group({
      ordre: [3, []],
      id: [5, []],
      nom: ['conseilAdministration', []],
      text: ['Les membres du conseil d\'administration', []],
      bold: [false, []],
      indent: [false, []],
      checked: [false, []]
    });
    horairesForm.push(caForm)
    const profForm: FormGroup = this.fb.group({
      ordre: [4, []],
      id: [3, []],
      nom: ['prof', []],
      text: ['Les profs et encadrants des sections', []],
      bold: [false, []],
      indent: [false, []],
      checked: [false, []]
    });
    horairesForm.push(profForm)

    const activiteForm: FormGroup = this.fb.group({
      ordre: [0, []],
      id: [0, []],
      nom: ['role', []],
      text: ['Role', []],
      bold: [true, []],
      indent: [false, []],
      checked: [false, []],
      horaires: horairesForm
    });
    this.maillingListe.push(activiteForm)

    this.getActivites()
    this.getHistorique()
  }

  selectGroupe(formGroup: FormGroup) {
    if (formGroup.get('checked')?.value) {
      const horaires: FormArray<FormGroup> = formGroup.get('horaires') as FormArray<FormGroup>
      horaires.controls.forEach(value => value.get('checked')?.setValue(true))

    } else {
      const horaires: FormArray<FormGroup> = formGroup.get('horaires') as FormArray<FormGroup>
      horaires.controls.forEach(value => value.get('checked')?.setValue(false))

    }
  }

  evaluateMailing(formGroup: FormGroup, controle: FormGroup) {
    if (!controle.get('checked')?.value) {
      formGroup.get('checked')?.setValue(false)
    }else{
      const horaires = formGroup.get('horaires') as FormArray<FormGroup>
      if(horaires.controls.filter(value => !value.get('checked')?.value).length == 0) {
        formGroup.get('checked')?.setValue(true)
      }
    }
  }

  getControl(formGroup: any) {
    return formGroup.get('checked') as FormControl;
  }

  getActivites() {
    this.activiteService.getAll().subscribe(
      data => {
        const listeActivite = data.reduce<Record<string, Activite[]>>((acc, item) => {
          (acc[item.nom] ??= []).push(item);
          return acc;
        }, {});
        let ordre = 5
        for (let listeActiviteKey in listeActivite) {

          const horairesForm: FormArray = this.fb.array([])
          listeActivite[listeActiviteKey].forEach(act => {

            const horaireForm: FormGroup = this.fb.group({
              ordre: [ordre, []],
              id: [act.id, []],
              nom: [act.horaire, []],
              text: [act.horaire, []],
              bold: [false, []],
              indent: [true, []],
              checked: [false, []]
            });
            horairesForm.push(horaireForm)

            ordre++;
          });
          const activiteForm: FormGroup = this.fb.group({
            ordre: [ordre, []],
            id: [0, []],
            nom: [listeActiviteKey, []],
            text: [listeActiviteKey, []],
            bold: [true, []],
            indent: [false, []],
            checked: [false, []],
            horaires: horairesForm
          });
          this.maillingListe.push(activiteForm)
        }
      },
      err => {
        console.log(err)
      }
    );
  }

  getHistorique() {
    this.mailService.getHistorique()
      .subscribe({
        next: (data) => {
          this.historiques = data
        },
        error: (error) => {
          console.log(error)
        }
      });
  }

  envoiTemplate() {
    console.log(this.maillingListe.getRawValue())
    this.inProgress = true;
    this.showWarning("Envoi de mail en cours, veuillez patienter")
    this.mailService.sendTemplate(this.maillingListe.getRawValue(), this.template)
      .subscribe({
        next: (newHistorique) => {
          this.historiques.push(newHistorique);
          this.showWarning("Votre message est à bien été envoyé")

          this.inProgress = false;
        },
        error: (error) => {

          this.inProgress = false;
          this.showError("Il y a eu un problème lors de l'envoie du message: "+error.message)

        }
      });
  }

  envoiMail(subject: string) {

    if (subject == undefined || subject.length < 5 || this.content == undefined || this.content.length < 20) {
      this.showWarning("Le mail n'est pas complet l'objet doit avoir au moins 5 lettres et le corps du message au moins 20 lettres")
    } else {
      this.inProgress = true;
      this.showWarning("Envoi de mail en cours, veuillez patienter")
      let email = new Email();
      email.subject = subject;
      email.text = this.content;
      email.diffusion = this.maillingListe.getRawValue() as Groupe[];
      this.mailService.sendMail(email)
        .subscribe({
          next: (newHistorique) => {
            this.historiques.push(newHistorique);
            this.showSucces("Votre message est à bien été envoyé")
            this.inProgress = false;
          },
          error: (error) => {
            this.inProgress = false;
            this.showError("Il y a eu un problème lors de l'envoie du message: "+error.message)
          }
        });
    }
  }




  showSucces(message: string) {
    this.toastr.success(message, 'Bravo!');
  }

  showWarning(message: string) {
    this.toastr.warning(message, 'Attention!');
  }

  showError(message: string) {
    this.toastr.error("Une erreur est survenue, recharger la page et recommencez. si le problème persiste contactez l'administrateur<br />" + message, 'Erreur');
  }
}
