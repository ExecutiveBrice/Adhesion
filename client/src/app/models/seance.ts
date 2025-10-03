import { PresenceSeance } from "./presenceSeance";


export class Seance {

  id!: number;

  activiteId!: number;

  numeroSeance!: number;

  dateSeance!: string;

  commentaire!: string;

  presenceSeance: PresenceSeance[]=[];
}
