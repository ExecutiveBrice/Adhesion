import { Adherent } from "./adherent";
export class Tribu {

  id: number;
  uuid: string;
  adherents: Adherent[];
visible:boolean
  constructor() {
    this.id = 0;
this.uuid = "";
    this.adherents = [];
    this.visible= true
  }
}
