export class AdhesionExcel {


    nomActivite:  string 
    nomAdherent:  string 
    emailAdherent:  string 

    statutActuel: String;

    tarif: number;
    typeReglement: String;
    validPaiementSecretariat: boolean;

    validDocumentSecretariat: boolean;

    remarqueSecretariat: String;
    flag: boolean;

    constructor() {

      this.tarif = 0;
      this.typeReglement = "";
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
  