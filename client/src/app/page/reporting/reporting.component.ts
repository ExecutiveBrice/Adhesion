import { Component, OnInit } from '@angular/core';
import { TokenStorageService } from '../../_services/token-storage.service';
import Chart from 'chart.js/auto';
import { ReportingService } from 'src/app/_services/reporting.service';
import { ReportingActivite } from 'src/app/models';


@Component({
  selector: 'app-reporting',
  templateUrl: './reporting.component.html',
  styleUrls: ['./reporting.component.css']
})
export class ReportingComponent implements OnInit {
  currentUser: any;
  chart: any = []
  dataBasket: ReportingActivite[] = []
  dataGeneral: ReportingActivite[] = []

  totalIniteeB: number = 0
  totalPayeeB: number = 0
  totalValideeB: number = 0
  totalFB: number = 0
  totalMB: number = 0
  totalCotisB: number = 0

  totalIniteeG: number = 0
  totalPayeeG: number = 0
  totalValideeG: number = 0
  totalFG: number = 0
  totalMG: number = 0
  totalCotisG: number = 0


  constructor(private reportingService: ReportingService) { }

  ngOnInit(): void {

    this.reportingService.getAllBasket()
      .subscribe({
        next: (data) => {

          this.dataBasket = data
          data.forEach(report => {
            this.totalCotisB += report.cotisations
            this.totalIniteeB += report.nbInitee
            this.totalPayeeB += report.nbPayee
            this.totalValideeB += report.nbValidee
            this.totalFB += report.nbF
            this.totalMB += report.nbM
          })
        },
        error: (error) => {

        }
      });

    this.reportingService.getAllGeneral()
      .subscribe({
        next: (data) => {

          this.dataGeneral = data
          data.forEach(report => {
            this.totalCotisG += report.cotisations
            this.totalIniteeG += report.nbInitee
            this.totalPayeeG += report.nbPayee
            this.totalValideeG += report.nbValidee
            this.totalFG += report.nbF
            this.totalMG += report.nbM
          })



        },
        error: (error) => {

        }
      });



    this.reportingService.getAllAdhesions()
      .subscribe({
        next: (data) => {

          data.forEach(adh => {
            this.initiee.push(adh.nbInitiee)

            this.payee.push(adh.nbPayee)
            this.validee.push(adh.nbValidee)
            this.label.push(adh.x)
          })

          this.chart = new Chart('canvas', {
            type: 'line',
            data: {
              labels: this.label,
              datasets: [
                {
                  label: "Initiées",
                  data: this.initiee
                },
                {
                  label: "Payées",
                  data: this.payee
                },
                {
                  label: "Validées",
                  data: this.validee
                }
              ],
            },

          });

        },
        error: (error) => {

        }
      });




  }
  label: string[] = []
  initiee: number[] = []
  payee: number[] = []
  validee: number[] = []
}
