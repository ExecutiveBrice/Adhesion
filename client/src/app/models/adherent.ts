import { Accord } from "./accord";
import { Activite } from "./activite";
import { ActivitesNm1 } from "./activitesNm1";
import { Adhesion } from "./adhesion";
import { Document } from "./document";
import { Notification } from "./notification";
import { Tribu } from "./tribu";
import { User } from "./user";

export class Adherent {
  id!: number;
  prenom!: string;
  nom!: string;
  nomPrenom!: string;
  genre!: string;
  email!: string;
  telephone!: string;
  naissance!: Date;
  lieuNaissance!: string;
  adresse!: string;
  codePostal!: string;
  ville!: string;
  representant!: Adherent;
  adresseRepresentant!: boolean;
  telephoneRepresentant!: boolean;
  emailRepresentant!: boolean;

  mineur!: boolean;
  completAdhesion!: boolean;

  accords!: Accord[];
  documents: string[];
  derniereModifs!: Notification[];
  derniereVisites!: Notification[];
  tribu!: Tribu;
  adhesions!: Adhesion[]
  activitesNm1!: ActivitesNm1[]
  cours!: Activite[];
  user!: User;
  
  tribuId!: number
  editMail!: boolean
  rgpdDate!: Date;
  certifDate!: Date;
  edit!:boolean

  constructor() {

    this.cours = [];
    this.adhesions = [];
    this.accords = [];

    this.documents = []

    this.derniereVisites = []
    this.derniereModifs = []
  }



}
