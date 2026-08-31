import { SalleConfiguration } from './salle';

export class Seance {
  id!: number;
  etatSeance!: 'PROGRAMMEE' | 'REALISEE' | 'ANNULEE' | 'MODIFIEE';
  causeAnnulation!: string;
  debut!: string;
  fin!: string;
  commentaire!: string;
  salle?: SalleConfiguration;
  dateEdition!: string;
  heureEdition!: string;
}

export interface SeanceDuJour {
  id: number;
  activite: string;
  debut: string;
  fin: string;
  lieu: string | null;
  adresse: string | null;
  commentaire: string | null;
  etatSeance: 'PROGRAMMEE' | 'REALISEE' | 'ANNULEE' | 'MODIFIEE';
  nombreParticipants: number;
}

export interface PresenceSeance {
  id: number;
  adherentId: number;
  nom: string;
  prenom: string;
  email: string;
  presence: boolean;
  paiementValide: boolean;
  documentsValides: boolean;
  statutAdhesion: string;
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
