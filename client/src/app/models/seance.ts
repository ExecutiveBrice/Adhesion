export class Seance {
  id!: number;
  etatSeance!: 'PROGRAMMEE' | 'REALISEE' | 'ANNULEE' | 'MODIFIEE';
  causeAnnulation!: string;
  debut!: string;
  fin!: string;
  commentaire!: string;
  dateEdition!: string;
  heureEdition!: string;
}

export interface SeanceCalendrier {
  id: number;
  activiteId: number;
  activiteNom: string;
  horaireActivite: string;
  salle: string;
  adresseSalle: string | null;
  couleurSalle: string | null;
  commentaire: string | null;
  lien: string | null;
  debut: string;
  fin: string;
  etatSeance: 'PROGRAMMEE' | 'REALISEE' | 'ANNULEE' | 'MODIFIEE';
}

export interface EvenementGoogleAgenda {
  id: string;
  titre: string;
  lieu: string | null;
  commentaire: string | null;
  debut: string;
  fin: string;
  journeeEntiere: boolean;
  agenda: string;
  agendaSource: string;
}

export interface CalendrierGoogle {
  evenements: EvenementGoogleAgenda[];
  erreurs: string[];
}
