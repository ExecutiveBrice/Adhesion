export class Paiement {

  id: number;
  montant: number;
  typeReglement: String;
  dateReglement: String;

  constructor(){
    this.id = 0;
    this.montant = 0;
    this.typeReglement = "";
    this.dateReglement = "";
  }
}
