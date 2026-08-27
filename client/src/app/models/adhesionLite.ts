
import { Accord } from "./accord";
import { ActiviteLite } from "./activiteLite";
import { AdherentLite } from "./adherentLite";
import { Paiement } from "./paiement";
import { Notification } from "./notification";

export class AdhesionLite {

  id!: number;
  tarif!: number;
  activite!: ActiviteLite;
  adherent!: AdherentLite;
  paiements!: Paiement[];
  typeReglement!: string;
  dateReglement!: string;
  dateAjoutPanier?: string;
  dateChangementStatut!: string;
  statutActuel!: string;
  remarqueSecretariat!: string;
  inscrit!: boolean;
  flag!: boolean;
  dejaLicencie?: boolean;
  validPaiementSecretariat!: boolean;
  validDocumentSecretariat!: boolean;
  blocage!: boolean;
  accords!: Accord[];
  derniereModifs!: Notification[];
  derniereVisites!: Notification[];
  
  constructor() {

    this.paiements = [];

    this.accords = [];
    this.derniereModifs = [];
    this.derniereVisites = [];
  }
}
