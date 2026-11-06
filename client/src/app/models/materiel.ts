export interface Zonage {
  id: number;
  nom: string;
  couleur: string;
}

export interface Materiel {
  image?: string | null;
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

export interface EmpruntCalendrierMateriel {
  materielId: number;
  materielNom: string;
  zonage: Zonage;
  emprunt: EmpruntMateriel;
}

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
  dateDebut?: string | null;
  dateFinPrevue?: string | null;
  dateRetour: string | null;
  enregistreParNom: string | null;
  rapporteParNom: string | null;
}
