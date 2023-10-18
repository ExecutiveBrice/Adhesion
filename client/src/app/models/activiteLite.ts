import { AdherentLite } from "./adherentLite";

export class ActiviteLite {

  id: number;
  nom: string;

  horaire: string;
  lien: string;
  salle: string;
  adherents: AdherentLite[];

  constructor(){
    this.id = 0;
    this.nom = "";
       this.horaire = "";
    this.lien = "";
    this.salle = "";
    this.adherents = [];
   
  }
}