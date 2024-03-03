
export class ActiviteDropDown {

  nom!: string;
  horaires!: HoraireDropDown[];
  
  constructor(){

    this.horaires = [];

  }
}


export class HoraireDropDown {

  id!: number;
  nom!: string;
  complete!: boolean
  dejaInscrit!:boolean
  constructor(){

  }
}

