export class Document {

  id: number;
  nom: string;
  file: string;
  type: string;
  dateDepot: Date | undefined;

  constructor(){
    this.id = 0;
    this.nom = "";
    this.file= "";
    this.type= "";
  }
}
