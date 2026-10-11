export interface Zonage {
  id: number;
  nom: string;
  couleur: string;
}

export interface Materiel {
  id: number;
  nom: string;
  zonage: Zonage;
  emprunteurId: number | null;
  emprunteurNom: string | null;
  dateEmprunt: string | null;
  horsService: boolean;
}

export interface MembreCAMateriel { id: number; nom: string; }

export interface EmpruntMateriel {
  id: number;
  emprunteurId: number | null;
  emprunteurNom: string | null;
  horsService?: boolean | null;
  dateEmprunt: string;
  dateRetour: string | null;
  enregistreParNom: string | null;
  rapporteParNom: string | null;
}
