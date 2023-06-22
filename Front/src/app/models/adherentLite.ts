import { Accord } from "./accord";

export class AdherentLite {
  id: number;
  prenom: string;
  nom: string;

  email: string;
  telephone: string;

  nomLegal: string;
  prenomLegal: string;
  telLegal: string;
  accords: Accord[];

  constructor() {
    this.id = 0;
    this.prenom = "";
    this.nom = "";
    this.email = "";
    this.telephone = "";
    this.nomLegal = "";
    this.prenomLegal = "";
    this.telLegal = "";
    this.accords = [];

  }



}
