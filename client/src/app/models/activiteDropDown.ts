
export class ActiviteDropDown {

  nom!: string;
  horaires!: HoraireDropDown[];
  selected!:boolean
  constructor(){

    this.horaires = [];

  }
}


export class HoraireDropDown {

  id!: number;
  nom!: string;
  complete!: boolean
  reinscription!: boolean
  dejaInscrit!:boolean
  constructor(){

  }
}

