import { Role } from "./role";

export class UserLite {

  id!: number;
  prenom!: string;
  nom!: string;
  email!: string;
  roles!: Role[];


  constructor() {

    this.roles = [];
  }
}
