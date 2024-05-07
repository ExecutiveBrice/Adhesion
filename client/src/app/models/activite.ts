import { Adherent } from "./adherent";
import { Adhesion } from "./adhesion";

export class Activite {

  id!: number;
  nom!: string;
  groupe!: string;
  groupeFiltre!: string;
  tarif!: number;

  ageMin!: number;
  ageMax!: number;
  genre!: string;
  horaire!: string;
  lien!: string;
  salle!: string;
  profs!: Adherent[];
  nbPlaces!: number;
  nbAdhesionsEnCours!: number;
  nbAdhesionsCompletes!: number;
  nbAdhesionsAttente!: number;
  montantCollecte!: number;
  adhesions!: Adhesion[];
  priseEnCharge!: boolean;
  autorisationParentale!: boolean;
  certificatMedical!: boolean;
  vieClub!: boolean;
  complete!: boolean;
  reinscription!: boolean;

  dureeVieCertif!: number;

  constructor() {

    this.profs = [];
    this.adhesions = [];

    this.nom = 'nouvelle activité'
    this.groupeFiltre = 'Amicaliste'
    this.horaire = 'dimanche matin'
    this.groupe = 'ALOD_G'
    this.tarif = 200
    
    this.nbPlaces = 10;
    this.ageMin = 16;
    this.ageMax = 99;
    this.genre = 'Non genrée';
    this.priseEnCharge = false;
    this.autorisationParentale = false;
    this.certificatMedical = false;
    this.vieClub = false;
    this.complete = false;
    this.reinscription = false;

  }
}
