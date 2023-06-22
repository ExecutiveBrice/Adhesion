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

  totalECB: number = 0
  totalVB: number = 0
  totalFB: number = 0
  totalMB: number = 0
  totalCotisB: number = 0

  totalECG: number = 0
  totalVG: number = 0
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
            this.totalECB += report.nbEC
            this.totalVB += report.nbV
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
            this.totalECG += report.nbEC
            this.totalVG += report.nbV
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
            this.encours.push(adh.nbEC)
            this.valide.push(adh.nbV)
            this.label.push(adh.x)
          })

          this.chart = new Chart('canvas', {
            type: 'line',
            data: {
              labels: this.label,
              datasets: [
                {
                  label: "En Cours",
                  data: this.encours
                },
                {
                  label: "Validée",
                  data: this.valide
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
  encours: number[] = []
  valide: number[] = []
}
