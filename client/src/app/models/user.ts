import { Adherent } from "./adherent";
import { ERole } from "./eRole";

export class User {

  id!: number;
  username!: string;
  password!: string;
  roles!: ERole[];
  emailValid!: boolean;
  adherent!: Adherent;


  constructor(){

    this.roles = [];

  }
}
