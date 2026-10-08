export interface ReportingAdhesionExport {
  nomActivite: string;
  nomAdherent: string;
  prenomAdherent: string;
  emailAdherent: string | null;
  statutActuel: string | null;
  dateReglement1: string | null;
  dateReglement2: string | null;
  dateReglement3: string | null;
  paiement1: number | null;
  paiement2: number | null;
  paiement3: number | null;
  typeReglement1: string | null;
  typeReglement2: string | null;
  typeReglement3: string | null;
  remarqueSecretariat: string | null;
}
