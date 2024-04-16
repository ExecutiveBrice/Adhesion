
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MailService } from '../../_services/mail.service';
import { DomSanitizer } from '@angular/platform-browser';
import { Activite, ActiviteDropDown, Adherent, Email, HoraireDropDown } from '../../models';
import { Router, ActivatedRoute } from '@angular/router';
import { interval } from 'rxjs';
import { Editor, Toolbar, toHTML } from 'ngx-editor';
import { ActiviteService } from 'src/app/_services/activite.service';
import { AdherentService } from 'src/app/_services/adherent.service';
import { ParamService } from 'src/app/_services/param.service';

@Component({
  selector: 'app-mailling',
  templateUrl: './mailling.component.html',
  styleUrls: ['./mailling.component.scss']
})

export class MaillingComponent implements OnInit, OnDestroy {
  inProgress: boolean = false;
  errorMailingList: String[] = [];
  mailIncomplet: boolean = false;
  editor: Editor = new Editor;
  html: object = {}
  subject: string = "";
  timer: any;
  choixMailing: string = "";
  isFailed = false;
  errorMessage = '';
  activites: ActiviteDropDown[] = []


  toolbar: Toolbar = [
    ['bold', 'italic'],
    ['underline', 'strike'],
    ['code', 'blockquote'],
    ['ordered_list', 'bullet_list'],
    [{ heading: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'] }],
    ['link', 'image'],
    ['text_color', 'background_color'],
    ['align_left', 'align_center', 'align_right', 'align_justify'],
  ];


  selectedAdherent!: Adherent;
  constructor(
    private adherentService: AdherentService,
    public activiteService: ActiviteService,
    public route: ActivatedRoute,
    public router: Router,
    public mailService: MailService,
    public sanitizer: DomSanitizer,
    public paramService: ParamService) { }

  ngOnInit(): void {

    let adherentId = this.route.snapshot.paramMap.get('adherentId');

    if (adherentId && !isNaN(Number.parseInt(adherentId))) {
console.log(adherentId)
      this.adherentService.getById(Number.parseInt(adherentId)).subscribe(
        data => {
console.log(data)
          this.selectedAdherent = data;
          this.isFailed = false;
        },
        err => {
          this.isFailed = true;
          this.errorMessage = err.message

        }
      );
    }
    this.timer = setInterval(() => {
      this.onTimeOut();
    }, 1000);

    
    this.getActivites()
  }



  getActivites() {


    this.activiteService.getAll().subscribe(
      data => {
           data.forEach(act => {

              if (this.activites.filter(activiteDropDown => activiteDropDown.nom == act.nom).length > 0) {
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                this.activites.filter(activiteDropDown => activiteDropDown.nom == act.nom)[0].horaires.push(horaireDropDown)
              } else {
                let activiteDropDown = new ActiviteDropDown()
                activiteDropDown.nom = act.nom
                let horaireDropDown = new HoraireDropDown
                horaireDropDown.id = act.id
                horaireDropDown.nom = act.horaire
                activiteDropDown.horaires.push(horaireDropDown)
                this.activites.push(activiteDropDown)
              }

        });

      },
      err => {
        this.isFailed = true;
        this.errorMessage = err.message
      }
    );
  }

  // make sure to destory the editor
  ngOnDestroy(): void {
    clearInterval(this.timer);
    this.editor.destroy();
  }

  envoiMail(subject: string) {
    if (subject == undefined || subject.length < 5 || this.html == undefined || toHTML(this.html).length < 20) {
      this.mailIncomplet = true;
      setTimeout(() => {
        this.mailIncomplet = false;
      }, 5000);
    } else {
      let email = new Email();
      email.subject = subject;
      email.text = toHTML(this.html);
      email.diffusion = this.selectedAdherent.id != 0 ? this.selectedAdherent.user.username : this.choixMailing;
      this.mailService.sendMail(email)
        .subscribe({
          next: (data) => {

            this.isFailed = false;
          },
          error: (error) => {
            this.isFailed = true;
            this.errorMessage = error.message
          }
        });

    }

  }

  onTimeOut() {

    this.mailService.isInProgress()
      .subscribe({
        next: (response) => {
          this.inProgress = response;
          this.isFailed = false;
        },
        error: (error) => {
          this.isFailed = true;
          this.errorMessage = error.message
        }
      });

  }




}