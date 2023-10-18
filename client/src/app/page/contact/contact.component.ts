
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MailService } from '../../_services/mail.service';
import { DomSanitizer } from '@angular/platform-browser';
import { Router, ActivatedRoute } from '@angular/router';

import { Editor, Toolbar, toHTML } from 'ngx-editor';
import { Email } from 'src/app/models';
import { ParamService } from 'src/app/_services/param.service';


@Component({
  selector: 'app-mailling',
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})

export class ContactComponent implements OnInit, OnDestroy {
  inProgress: boolean = false;
  errorMailingList: String[] = [];
  mailIncomplet: boolean = false;
  editor: Editor = new Editor;
  html: object = {}
  subject: string = "";
  timer: any;
  choixMailing: string = "";
  isFailed = false;
  succes = false;
  errorMessage = '';
  nomMailling: string[] = []
  message:string = ""

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



  constructor(
    public paramService: ParamService,
    public route: ActivatedRoute,
    public router: Router,
    public mailService: MailService,
    public sanitizer: DomSanitizer) { }

  ngOnInit(): void {

    this.paramService.getAllText().subscribe({
      next: (data) => {
        
        this.message = data.filter(param => param.paramName == "Text_Contact")[0].paramValue;
      },
      error: (error) => {
      }
    });

    this.timer = setInterval(() => {
      this.onTimeOut();
    }, 1000);

  }

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
      email.diffusion = "alod.amicale@gmail.com";
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