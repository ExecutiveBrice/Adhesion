
import { Accord } from "./accord";
import { ActiviteLite } from "./activiteLite";
import { AdherentLite } from "./adherentLite";
import { Paiement } from "./paiement";

export class AdhesionLite {

  id: number;
  tarif: number;
  activite: ActiviteLite | undefined
  adherentNom: AdherentLite | undefined
  paiements: Paiement[];
  typeReglement: String;
  dateReglement: String;
  dateChangementStatut: string
  statutActuel: String;
  remarqueSecretariat: String;
  inscrit: boolean;
  flag: boolean;
  mineur: boolean;
  validPaiementSecretariat: boolean;
  validDocumentSecretariat: boolean;
  blocage: boolean;
  accords: Accord[];
  
  constructor() {
    this.id = 0;
    this.tarif = 0;
    this.paiements = [];
    this.typeReglement = "";
    this.dateReglement = "";
    this.statutActuel = "";
    this.dateChangementStatut = ""
    this.remarqueSecretariat = "";
    this.inscrit = false;
    this.flag = false;
    this.mineur = false;
    this.validPaiementSecretariat = false;
    this.validDocumentSecretariat = false;
    this.blocage = false;
    this.accords = [];
  }
}
