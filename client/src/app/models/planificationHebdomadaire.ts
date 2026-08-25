import { SalleConfiguration } from './salle';

export interface PlanificationHebdomadaire {
  id?: number;
  jour: string;
  horaireDebut: string;
  duree: number | null;
  descriptif?: string;
  salle?: SalleConfiguration;
}
