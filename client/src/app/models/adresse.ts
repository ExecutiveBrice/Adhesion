import { Adherent } from "./adherent";

export class Adresse {



  id!: number;
  adresse!: string;
  codepostal!: number;
  adherents!: number[];


  constructor(){

    this.adherents = [];
  }
}
