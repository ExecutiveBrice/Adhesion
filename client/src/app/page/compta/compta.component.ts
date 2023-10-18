import { Component, OnInit } from '@angular/core';
import { TokenStorageService } from '../../_services/token-storage.service';

import { ReportingService } from 'src/app/_services/reporting.service';
import { ComptaService } from 'src/app/_services/compta.service';
import { ComptaActivite } from 'src/app/models/comptaActivite';
import { DatePipe } from '@angular/common';
import { ParamService } from 'src/app/_services/param.service';



@Component({
  selector: 'app-compta',
  templateUrl: './compta.component.html',
  styleUrls: ['./compta.component.css']
})
export class ComptaComponent implements OnInit {
  currentUser: any;
  dataCompta: ComptaActivite[] = []
  debutPlage: string | null = "";
  finPlage: string | null = "";

  jourDebutPlage: number | null = 0;
  jourFinPlage: number | null = 0;
  totalHA: number = 0
  totalHA3X: number = 0
  totalC: number = 0
  totalC3X: number = 0
  totalPS: number = 0
  totalE: number = 0
  totalA: number = 0

  constructor(private datePipe: DatePipe, private comptaService: ComptaService, private paramService: ParamService) { }

  ngOnInit(): void {

    this.paramService.getAllNumber().subscribe({
      next: (data) => {
        this.jourDebutPlage = data.filter(param => param.paramName == "Jour_Debut_Plage_Compta")[0].paramValue;
        this.jourFinPlage = data.filter(param => param.paramName == "Jour_Fin_Plage_Compta")[0].paramValue;

        let datedebut: Date = new Date
        datedebut.setFullYear(datedebut.getFullYear(), datedebut.getMonth() - 1, this.jourDebutPlage ? this.jourDebutPlage : 1)
        this.debutPlage = this.datePipe.transform(datedebut, "yyyy-MM-dd");

        let dateFin: Date = new Date
        dateFin.setFullYear(dateFin.getFullYear(), dateFin.getMonth(), this.jourFinPlage ? this.jourFinPlage : 1)
        this.finPlage = this.datePipe.transform(dateFin, "yyyy-MM-dd");

        if (this.debutPlage && this.finPlage) {
          this.getCompta(this.debutPlage, this.finPlage)
        }
      },
      error: (error) => {
        this.jourDebutPlage = 4;
        this.jourFinPlage = 3;
      }
    });
  }


  updateDate(dateDebut: string | null, dateFin: string | null) {


    this.debutPlage = this.datePipe.transform(dateDebut, "yyyy-MM-dd");


    this.finPlage = this.datePipe.transform(dateFin, "yyyy-MM-dd");

    if (this.debutPlage && this.finPlage) {
      this.getCompta(this.debutPlage, this.finPlage)
    }


  }


  getCompta(dateDebut: string, dateFin: string) {

    let debutPlage: Date = new Date(dateDebut)
    let finPlage: Date = new Date(dateFin)

    this.totalHA = 0
    this.totalHA3X = 0
    this.totalC = 0
    this.totalC3X = 0
    this.totalPS = 0
    this.totalE = 0
    this.totalA = 0

    this.comptaService.getAll(debutPlage, finPlage)
      .subscribe({
        next: (data) => {
          this.dataCompta = data

          data.forEach(report => {
            this.totalHA += report.helloAsso
            this.totalHA3X += report.helloAsso3x
            this.totalC += report.cheque
            this.totalC3X += report.cheque3x
            this.totalPS += report.passport
            this.totalE += report.espece
            this.totalA += report.autre
          })

        },
        error: (error) => {

        }
      });



  }


}
