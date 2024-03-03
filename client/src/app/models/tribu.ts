import { Adherent } from "./adherent";
export class Tribu {

  id!: number;
  uuid!: string;
  adherents!: Adherent[];
  visible!: boolean
  constructor() {

    this.adherents = [];
  }
}
