import { ERole } from "./eRole";

export class UserLite {

  id!: number;
  adherent!: string;
  username!: string;
  roles!: ERole[];


  constructor() {

    this.roles = [];
  }
}
