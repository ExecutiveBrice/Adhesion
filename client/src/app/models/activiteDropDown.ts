
export class ActiviteDropDown {

  nom: string;
  horaires: HoraireDropDown[];
  
  constructor(){
    this.nom = "";
    this.horaires = [];

  }
}


export class HoraireDropDown {

  id: number;
  nom: string;
  complete: boolean
  constructor(){
    this.id = 0;
    this.nom = "";
    this.complete = false
  }
}

