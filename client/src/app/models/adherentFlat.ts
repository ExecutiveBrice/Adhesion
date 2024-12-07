import { Accord } from "./accord";


export class AdherentFlat {
  id!: number;
  prenom!: string;
  nom!: string;
  nomPrenom!: string;

  adresse!: string;
  email!: string;
  telephone!: string;


  accords!: Accord[];
  adhesions!: string
  lieuNaissance!: string;
  naissance!: string;
  tribuId!: number;
 
  constructor() {

    this.accords = [];

  }



}
