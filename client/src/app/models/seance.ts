import { Activite } from './activite';
import { PresenceSeance } from './presenceSeance';

export class Seance {
  id!: number;
  activite!: Activite;
  etatSeance!: string;
  causeAnnulation?: string;
  debut!: string;
  fin!: string;
  commentaire?: string;
  presences: PresenceSeance[] = [];
}
