export class AdhesionExcel {


    nomActivite!:  string 
    nomAdherent!:  string 
    prenomAdherent!:  string 
    emailAdherent!:  string 

    statutActuel!: String;

    dateReglement1!: String;
    dateReglement2!: String;
    dateReglement3!: String;
    paiement1!: number;
    paiement2!: number;
    paiement3!: number;
    typeReglement1!: String;
    typeReglement2!: String;
    typeReglement3!: String;

    validPaiementSecretariat!: boolean;

    validDocumentSecretariat!: boolean;

    remarqueSecretariat!: String;
    flag!: boolean;

    constructor() {


    }
  }
  