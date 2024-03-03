import { Adherent } from "./adherent";
import { Role } from "./role";

export class User {

  id!: number;
  username!: string;
  password!: string;
  roles!: Role[];
  emailValid!: boolean;
  adherent!: Adherent;


  constructor(){

    this.roles = [];

  }
}
