import {Groupe} from "./groupe";

export class Email {

  diffusion!: Groupe[];
  subject!:string;
  text!:string;

  constructor(){
    this.diffusion = [];
    this.subject = "";
    this.text = "";
  }
}
