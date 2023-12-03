import { Adherent } from "./adherent";
import { Role } from "./role";

export class User {

  id: number;
  username: string;
  password: string;
  roles: Role[];
  emailValid: boolean;
  adherent!: Adherent;


  constructor(){
    this.id = 0;
    this.username = "";
    this.password = "";
    this.roles = [];
    this.emailValid = true;

  }
}
