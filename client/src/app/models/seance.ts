import { SalleConfiguration } from './salle';
import { ResponsabiliteSeance } from './responsabiliteSeance';

export class Seance {
  id!: number;
  etatSeance!: 'PROGRAMMEE' | 'REALISEE' | 'ANNULEE' | 'MODIFIEE';
  causeAnnulation!: string;
  debut!: string;
  fin!: string;
  commentaire!: string;
  salle?: SalleConfiguration;
  responsabilites?: ResponsabiliteSeance[];
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
  presence: boolean | null;
  paiementValide: boolean;
  documentsValides: boolean;
  statutAdhesion: string;
}

export interface SeanceCalendrier {
  id: number;
  responsabilites?: ResponsabiliteSeance[];
  activiteId: number;
  sectionType?: string | null;
  sectionId?: number | null;
  sectionNom?: string | null;
  couleurSection?: string | null;
  activiteNom: string;
  activiteNomCourt?: string;
  descriptif?: string | null;
  causeAnnulation?: string | null;
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

export interface PresencePrevue {
  id: number;
  adherentId: number;
  nom: string;
  prenom: string;
  presencePrevue: boolean | null;
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
