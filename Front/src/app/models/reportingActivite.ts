export class ReportingActivite {


  nomActivite: string;
  groupe: string;
  nbEC: number;
  nbV: number;
  nbF: number;
  nbM: number;
  cotisations: number;



  constructor() {

    this.nomActivite = "";
    this.groupe = "";
    this.nbEC = 0;
    this.nbV = 0;
    this.nbF = 0;
    this.nbM = 0;
    this.cotisations = 0;
  }
}
