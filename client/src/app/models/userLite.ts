import { Role } from "./role";

export class UserLite {

  id!: number;
  adherent!: string;
  username!: string;
  roles!: Role[];


  constructor() {

    this.roles = [];
  }
}
