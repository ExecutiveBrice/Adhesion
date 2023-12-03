import { Accord } from "./accord";
import { Activite } from "./activite";
import { Adhesion } from "./adhesion";
import { Document } from "./document";
import { Notification } from "./notification";
import { Tribu } from "./tribu";
import { User } from "./user";

export class Adherent {
  id: number;
  prenom: string;
  nom: string;
  genre: string;
  email: string;
  telephone: string;
  naissance: Date | undefined;
  lieuNaissance: string;
  referent: boolean;
  adresse: string;
  adresseReferent: boolean;
  telephoneReferent: boolean;
  emailReferent: boolean;
  mineur: boolean;
  nomLegal: string;
  prenomLegal: string;
  legalReferent: boolean;
  completReferent: boolean;
  completAdhesion: boolean;

  accords: Accord[];
  documents: Document[];
  derniereModifs : Notification[];
  derniereVisites : Notification[];
  tribu: Tribu;
  adhesions: Adhesion[]
  cours: Activite[];
  user: User;

  
  
  tribuId: number
  editMail: boolean
  rgpdDate: Date | undefined;
  certifDate: Date | undefined;
  edit:boolean

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
    this.user = new User;
    this.documents = []
    this.edit = false
    this.derniereVisites = []
    this.derniereModifs = []
  }



}
