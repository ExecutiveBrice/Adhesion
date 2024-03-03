import { Accord } from "./accord";
import { AdhesionLite } from "./adhesionLite";

export class AdherentLite {
  id!: number;
  prenom!: string;
  nom!: string;
  nomPrenom!: string;
  genre!: string;
  adresse!: string;
  email!: string;
  statut!: string;
  editMail!: boolean;
  telephone!: string;
  referent!: boolean;
  commentaire!: string;
  flag!: boolean;
  paiement!: boolean;
  mineur!: boolean;
  nomLegal!: string;
  prenomLegal!: string;

  emailLegal!: string;
  adresseLegal!: string;
  telLegal!: string;
  accords!: Accord[];
  adhesions!: AdhesionLite[]
  lieuNaissance!: string;
  naissance!: string;
  tribuId!: number;
  tribuSize!: number;
  constructor() {

    this.accords = [];

    this.adhesions = []
  }



}
