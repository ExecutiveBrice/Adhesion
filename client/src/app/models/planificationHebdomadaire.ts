import { SalleConfiguration } from './salle';
import { UtilisateurSelectionnable } from './utilisateurSelectionnable';

export interface PlanificationHebdomadaire {
  id?: number;
  jour: string;
  horaireDebut: string;
  duree: number | null;
  descriptif?: string;
  salle?: SalleConfiguration;
  profs?: UtilisateurSelectionnable[];
  referents?: UtilisateurSelectionnable[];
  nbSeancesTotal?: number;
  nbSeancesRealisees?: number;
}
