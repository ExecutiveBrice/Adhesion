import {Horaire} from "./horaire";

export class Groupe {

  bold!: boolean;
  checked!: boolean;
  horaires!:Horaire[];
  id!:number;
  indent!:boolean;
  nom!: string;
  ordre!: number;
  text!:string;

}
