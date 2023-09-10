export class ReportingActivite {


  nomActivite: string;
  groupe: string;
  nbInitee: number;
  nbPayee: number;
  nbValidee: number;
  nbF: number;
  nbM: number;
  cotisations: number;



  constructor() {

    this.nomActivite = "";
    this.groupe = "";
    this.nbInitee = 0;
    this.nbPayee = 0;
    this.nbValidee = 0;
    this.nbF = 0;
    this.nbM = 0;
    this.cotisations = 0;
  }
}
