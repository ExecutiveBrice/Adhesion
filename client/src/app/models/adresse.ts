import { Adherent } from "./adherent";

export class Adresse {



  id: number;
  adresse: string;
  codepostal: number;
  adherents: number[];


  constructor(){
    this.id = 0;
    this.adresse = "";
    this.codepostal = 0;
    this.adherents = [];
  }
}
