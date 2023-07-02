import { Accord } from "./accord";
import { AdhesionLite } from "./adhesionLite";

export class AdherentLite {
  id: number;
  prenom: string;
  nom: string;
  prenomNom:string;
  genre: string;
  adresse: string;
  email: string;
  editMail: boolean;
  telephone: string;
  referent: boolean;
  nomLegal: string;
  prenomLegal: string;
  accords: Accord[];
  adhesions: AdhesionLite[]
  lieuNaissance:string;
  naissance:string;
  tribuId: number;
  tribuSize: number;
  constructor(id: number) {
    this.id = id;
    this.prenom = "";
    this.nom = "";
    this.prenomNom = "";
    this.genre = "";
    this.adresse = "";
    this.email = "";
    this.editMail = false;
    this.telephone = "";
    this.nomLegal = "";
    this.prenomLegal = "";
    this.lieuNaissance = ""
    this.naissance = ""
    this.tribuId = 0;
    this.tribuSize = 0;

    this.accords = [];
    this.referent = false;
    this.adhesions = []
  }



}
