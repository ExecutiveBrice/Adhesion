export class AdhesionExcel {


    nomActivite:  string 
    nomAdherent:  string 
    emailAdherent:  string 

    statutActuel: String;

    dateReglement1: String;
    dateReglement2: String;
    dateReglement3: String;
    paiement1: number;
    paiement2: number;
    paiement3: number;
    typeReglement1: String;
    typeReglement2: String;
    typeReglement3: String;

    validPaiementSecretariat: boolean;

    validDocumentSecretariat: boolean;

    remarqueSecretariat: String;
    flag: boolean;

    constructor() {

      this.dateReglement1 = "";
      this.dateReglement2 = "";
      this.dateReglement3 = "";
      this.paiement1 = 0;
      this.paiement2 = 0;
      this.paiement3 = 0;
      this.typeReglement1 = "";
      this.typeReglement2 = "";
      this.typeReglement3 = "";

      this.statutActuel = "";
      this.remarqueSecretariat = "";
      this.nomActivite = "";
      this.nomAdherent = "";
      this.emailAdherent = "";

      this.flag = false;
      this.validPaiementSecretariat = false;
      this.validDocumentSecretariat = false;

    }
  }
  