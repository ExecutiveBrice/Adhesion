import { ComptaActivite } from './comptaActivite';

export interface PaiementCompta {
  id: number | null;
  dateReglement: string | null;
  typeReglement: string | null;
  montant: number | null;
  inclusDansPeriode: boolean;
}

export interface AdhesionCompta {
  id: number;
  nomActivite: string;
  nomAdherent: string | null;
  prenomAdherent: string | null;
  emailAdherent: string | null;
  statutActuel: string | null;
  rapprochement: boolean | null;
  paiements: PaiementCompta[];
  totalPeriode: number;
}

export interface ComptaPeriode {
  recap: ComptaActivite[];
  adhesions: AdhesionCompta[];
}
