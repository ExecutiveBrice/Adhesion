import { Adherent } from "./adherent";

export class PresenceSeance {

  id!: number;

  seanceId!: number;

  adherent!: Adherent;

  presence!: boolean

  dateModification!: string
}
