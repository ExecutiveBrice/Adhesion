import { Adherent } from "./adherent";
import { Adhesion } from "./adhesion";
import { SalleConfiguration } from './salle';
import { SectionConfiguration } from './section';
import { SeanceCalendrier } from './seance';

export class Activite {


  equipes?: { id: number; nom: string; membres: number }[];
  evenementsSemaineProchaine?: SeanceCalendrier[];
  id!: number;
  nom!: string;
  section?: SectionConfiguration;
  tarif!: number;
  ageMin!: number;
  ageMax!: number;
  genre!: string;
  /** Champ de compatibilité pour les exports des années précédentes. */
  horaire?: string;
  lien!: string;
  salle?: SalleConfiguration;
  profs!: Adherent[];
  referents!: Adherent[];
  nbPlaces!: number;
  nbAdhesionsEnCours!: number;
  nbAdhesionsCompletes!: number;
  nbAdhesionsAttente!: number;
  nbEvenementsRealises!: number;
  nbEvenementsTotal!: number;
  montantCollecte!: number;
  adhesions!: Adhesion[];
  priseEnCharge!: boolean;
  autorisationParentale!: boolean;
  certificatMedical!: boolean;
  vieClub!: boolean;
  charteAmicale!: boolean;
  complete!: boolean;
  majoration!: boolean;
  reinscription!: boolean;
  globaleSpecifique!: boolean;

  dureeVieCertif!: number;

  constructor() {

    this.profs = [];
    this.referents = [];
    this.adhesions = [];

    this.nom = 'nouvelle activité'
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
    this.reinscription= false;
    this.globaleSpecifique = false;
  }
}
