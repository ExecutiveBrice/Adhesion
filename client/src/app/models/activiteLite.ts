import { AdherentLite } from "./adherentLite";
import { SectionConfiguration } from './section';

export class ActiviteLite {

  id!: number;
  nom!: string;

  horaire!: string;
  lien!: string;
  salle!: string;
  section?: SectionConfiguration;
  adherents!: AdherentLite[];

  constructor(){

    this.adherents = [];
   
  }
}
