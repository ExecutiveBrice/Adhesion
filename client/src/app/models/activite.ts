import { Adherent } from "./adherent";
import { Adhesion } from "./adhesion";

export class Activite {

  id!: number;
  nom!: string;
  groupe!: string;
  groupeFiltre!: string;
  tarif!: number;
  horaire!: string;
  lien!: string;
  salle!: string;
  profs!: Adherent[];
  nbPlaces!: number;
  nbAdhesionsEnCours!:number;
  nbAdhesionsCompletes!:number;
  nbAdhesionsAttente!:number;
  montantCollecte!:number;
  adhesions!: Adhesion[];
  priseEnCharge!: boolean;
  autorisationParentale!: boolean;
  pourEnfant!: boolean|null;
  certificatMedical!: boolean;
  vieClub!: boolean;
  complete!: boolean;
  dureeVieCertif!:number;
  
  constructor(){

    this.profs = [];

    this.adhesions = [];

  }
}
