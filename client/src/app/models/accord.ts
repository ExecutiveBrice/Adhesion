export class Accord {

  id: number;
  nom: string;
  etat: boolean| undefined;
  datePassage: Date | undefined;

  constructor(){
    this.id = 0;
    this.nom = "";
  }
}
