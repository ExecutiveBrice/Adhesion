import { Accord } from "./accord";
import { Activite } from "./activite";
import { Adhesion } from "./adhesion";
import { Document } from "./document";
import { Tribu } from "./tribu";
import { User } from "./user";

export class Adherent {
  id: number;
  prenom: string;
  nom: string;
  genre: string;
  email: string;
  telephone: string;
  adresse: string;
  naissance: Date | undefined;
  lieuNaissance: string;
  referent: boolean;
  cours: Activite[];
  emailReferent: boolean;
  telephoneReferent: boolean;
  adresseReferent: boolean;
  nomLegal: string;
  prenomLegal: string;
  legalReferent: boolean;
  mineur: boolean;
  tribu: Tribu;
  tribuId: number
  editMail: boolean
  adhesions: Adhesion[]
  rgpdDate: Date | undefined;
  completReferent: boolean;
  completAdhesion: boolean;
  certifDate: Date | undefined;
  accords: Accord[];
  user: User | null;
  documents: Document[];

  constructor(id:number) {
    this.id = id;
    this.prenom = "";
    this.nom = "";
    this.genre = "";
    this.cours = [];
    this.email = "";
    this.telephone = "";
    this.adresse = "";
    this.lieuNaissance = "";
    this.referent = false;
    this.emailReferent = true;
    this.telephoneReferent = true;
    this.adresseReferent = true;
    this.nomLegal = "";
    this.prenomLegal = "";
    this.legalReferent = true;
    this.mineur = true;
    this.adhesions = [];
    this.completReferent = false;
    this.completAdhesion = false;
    this.accords = [];
    this.tribu = new Tribu
    this.tribuId = 0
    this.editMail = false
    this.user = null;
    this.documents = []
  }



}
