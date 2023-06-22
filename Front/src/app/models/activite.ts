import { Adherent } from "./adherent";
import { Adhesion } from "./adhesion";

export class Activite {

  id: number;
  nom: string;
  groupe: string;
  groupeFiltre: string;
  tarif: number;
  horaire: string;
  lien: string;
  salle: string;
  profs: Adherent[];
  nbPlaces: number;
  nbAdhesionsEnCours:number;
  nbAdhesionsCompletes:number;
  montantCollecte:number;
  adhesions: Adhesion[];
  priseEnCharge: boolean;
  autorisationParentale: boolean;
  pourEnfant: boolean|null;
  certificatMedical: boolean;
  vieClub: boolean;
  complete: boolean;
  dureeVieCertif:number;
  constructor(){
    this.id = 0;
    this.nom = "";
    this.groupe = "";
    this.groupeFiltre = "";
    this.horaire = "";
    this.lien = "";
    this.salle = "";
    this.profs = [];
    this.tarif = 0;
    this.nbPlaces = 0;
    this.nbAdhesionsEnCours = 0;
    this.nbAdhesionsCompletes = 0;
    this.montantCollecte = 0; 
    this.adhesions = [];
    this.priseEnCharge= false;
    this.autorisationParentale= false;
    this.pourEnfant= null;
    this.certificatMedical= false;
    this.vieClub= false;
    this.dureeVieCertif=0
    this.complete = false
  }
}
