import { User } from "./user";

export class Notification {

  id: number;
  user: User;
  date: string;

  constructor(){
    this.id = 0;
    this.user = new User
    this.date = "";
  }
}
