import { Adherent } from "./adherent";
export class Tribu {

  id: number;
  adherents: Adherent[];
visible:boolean
  constructor() {
    this.id = 0;
    this.adherents = [];
    this.visible= true
  }
}
