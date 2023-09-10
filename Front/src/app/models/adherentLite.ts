import { Accord } from "./accord";
import { AdhesionLite } from "./adhesionLite";

export class AdherentLite {
  id: number;
  prenom: string;
  nom: string;
  nomPrenom: string;
  genre: string;
  adresse: string;
  email: string;
  statut: string;
  editMail: boolean;
  telephone: string;
  referent: boolean;
  commentaire: string;
  flag: boolean;
  paiement: boolean;
  mineur: boolean;
  nomLegal: string;
  prenomLegal: string;
  telLegal: string;
  accords: Accord[];
  adhesions: AdhesionLite[]
  lieuNaissance: string;
  naissance: string;
  tribuId: number;
  tribuSize: number;
  constructor(id: number) {
    this.id = id;
    this.prenom = "";
    this.nom = "";
    this.nomPrenom = "";
    this.genre = "";
    this.adresse = "";
    this.email = "";
    this.statut = "";
    this.editMail = false;
    this.telephone = "";
    this.nomLegal = "";
    this.prenomLegal = "";
    this.lieuNaissance = ""
    this.naissance = ""
    this.tribuId = 0;
    this.tribuSize = 0;
    this.telLegal = ""
    this.commentaire = "";
    this.flag = false;
    this.paiement = false;
    this.accords = [];
    this.referent = false;
    this.mineur= false;
    this.adhesions = []
  }



}
