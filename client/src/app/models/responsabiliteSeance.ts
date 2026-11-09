import type { TeamInvitation } from '../_services/team.service';

export interface TacheSeanceConfiguration {
  id?: number;
  nom: string;
  icone?: string;
  couleur?: string;
}

export interface AdherentResponsabilite {
  id: number;
  nom: string;
  prenom: string;
}

export interface ResponsabiliteSeance {
  tache: TacheSeanceConfiguration;
  adherent: AdherentResponsabilite;
  equipeId?: number | null;
  equipeNom?: string | null;
  equipes?: string[];
  invitation?: TeamInvitation | null;
}

export interface ResponsabiliteSeanceEdition {
  tacheId?: number;
  adherentId?: number;
}
