
import { Accord } from "./accord";
import { Activite } from "./activite";
import { Adherent } from "./adherent";
import { Notification } from "./notification";
import { Paiement } from "./paiement";

export class Adhesion {

  id!: number;
  tarif!: number;
  activite!: Activite | undefined
  adherent!: Adherent
  paiements!: Paiement[];
  typeReglement!: String;
  dateReglement!: String;
  statutActuel!: String;
  remarqueSecretariat!: String;
  inscrit!: boolean;
  flag!: boolean;
  validPaiementSecretariat!: boolean;
  validDocumentSecretariat!: boolean;
  blocage!: boolean;
  accords!: Accord[];
  dateChangementStatut!: String;
  derniereModifs!: Notification[];
  derniereVisites!: Notification[];

  constructor() {

    this.paiements = [];

    this.accords = [];

    this.derniereVisites = []
    this.derniereModifs = []
  }
}
