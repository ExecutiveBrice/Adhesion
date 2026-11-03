export interface Zonage {
  id: number;
  nom: string;
  couleur: string;
}

export interface Materiel {
  id: number;
  nom: string;
  zonage: Zonage;
  quantiteTotale?: number;
  quantiteHorsService?: number;
  quantiteEmpruntee?: number;
  quantiteDisponible?: number;
  emprunts?: EmpruntMateriel[];
  emprunteurId: number | null;
  emprunteurNom: string | null;
  dateEmprunt: string | null;
  horsService: boolean;
  probleme?: string | null;
}

export interface MembreCAMateriel { id: number; nom: string; }

export interface EmpruntMateriel {
  quantite?: number;
  quantiteRetournee?: number;
  retours?: { quantite: number; dateRetour: string; rapporteParNom: string }[];
  id: number;
  emprunteurId: number | null;
  emprunteurNom: string | null;
  horsService?: boolean | null;
  commentaire?: string | null;
  dateEmprunt: string;
  dateRetour: string | null;
  enregistreParNom: string | null;
  rapporteParNom: string | null;
}
