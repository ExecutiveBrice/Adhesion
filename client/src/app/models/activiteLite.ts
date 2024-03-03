import { AdherentLite } from "./adherentLite";

export class ActiviteLite {

  id!: number;
  nom!: string;

  horaire!: string;
  lien!: string;
  salle!: string;
  adherents!: AdherentLite[];

  constructor(){

    this.adherents = [];
   
  }
}