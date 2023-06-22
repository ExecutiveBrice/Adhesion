
import { Accord } from "./accord";
import { Activite } from "./activite";
import { Adherent } from "./adherent";
import { Tribu } from "./tribu";

export class Adhesion {

  id: number;
  tarif: number;
  activite: Activite | undefined
  adherent: Adherent | undefined
  typeReglement: String;
  dateReglement: Date | undefined;
  statutActuel: String;
  remarqueSecretariat: String;
  inscrit: boolean;
  flag: boolean;
  validPaiementSecretariat: boolean;
  validDocumentSecretariat: boolean;
  blocage: boolean;
  accords: Accord[];
  constructor() {
    this.id = 0;
    this.tarif = 0;
    this.typeReglement = "";
    this.statutActuel = "";
    this.remarqueSecretariat = "";
    this.inscrit = false;
    this.flag = false;
    this.validPaiementSecretariat = false;
    this.validDocumentSecretariat = false;
    this.blocage = false;
    this.accords = [];
  }
}
