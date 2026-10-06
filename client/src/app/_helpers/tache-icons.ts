import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import {
  faClipboardCheck, faClipboardList, faBoxOpen, faScrewdriverWrench, faBroom, faPeopleGroup,
  faHandshake, faCar, faVanShuttle, faBasketball, faFutbol, faVolleyball, faDumbbell, faMusic,
  faUtensils, faMugHot, faKey, faDoorOpen, faKitMedical, faBullhorn, faCamera, faFlag,
  faStopwatch, faTrophy, faTable, faChair, faShirt, faEuroSign
} from '@fortawesome/free-solid-svg-icons';

export const ICONE_TACHE_PAR_DEFAUT = 'clipboard-check';
export const COULEUR_TACHE_PAR_DEFAUT = '#176B4A';

export const ICONES_TACHES: { nom: string; libelle: string; icone: IconDefinition }[] = [
  { nom: 'clipboard-check', libelle: 'Organisation', icone: faClipboardCheck },
  { nom: 'clipboard-list', libelle: 'Liste de tâches', icone: faClipboardList },
  { nom: 'box-open', libelle: 'Matériel', icone: faBoxOpen },
  { nom: 'screwdriver-wrench', libelle: 'Installation', icone: faScrewdriverWrench },
  { nom: 'broom', libelle: 'Nettoyage', icone: faBroom },
  { nom: 'people-group', libelle: 'Équipe', icone: faPeopleGroup },
  { nom: 'handshake', libelle: 'Accueil', icone: faHandshake },
  { nom: 'car', libelle: 'Covoiturage', icone: faCar },
  { nom: 'van-shuttle', libelle: 'Transport', icone: faVanShuttle },
  { nom: 'basketball', libelle: 'Basket', icone: faBasketball },
  { nom: 'futbol', libelle: 'Football', icone: faFutbol },
  { nom: 'volleyball', libelle: 'Volley', icone: faVolleyball },
  { nom: 'dumbbell', libelle: 'Entraînement', icone: faDumbbell },
  { nom: 'music', libelle: 'Musique', icone: faMusic },
  { nom: 'utensils', libelle: 'Repas', icone: faUtensils },
  { nom: 'mug-hot', libelle: 'Buvette', icone: faMugHot },
  { nom: 'key', libelle: 'Clés', icone: faKey },
  { nom: 'door-open', libelle: 'Ouverture de salle', icone: faDoorOpen },
  { nom: 'kit-medical', libelle: 'Premiers secours', icone: faKitMedical },
  { nom: 'bullhorn', libelle: 'Communication', icone: faBullhorn },
  { nom: 'camera', libelle: 'Photos', icone: faCamera },
  { nom: 'flag', libelle: 'Arbitrage', icone: faFlag },
  { nom: 'stopwatch', libelle: 'Chronométrage', icone: faStopwatch },
  { nom: 'trophy', libelle: 'Compétition', icone: faTrophy },
  { nom: 'table', libelle: 'Table', icone: faTable },
  { nom: 'chair', libelle: 'Chaises', icone: faChair },
  { nom: 'shirt', libelle: 'Tenues', icone: faShirt },
  { nom: 'euro-sign', libelle: 'Caisse', icone: faEuroSign }
];

export function iconeTache(nom?: string): IconDefinition {
  return ICONES_TACHES.find(icone => icone.nom === nom)?.icone ?? faClipboardCheck;
}

export function couleurTache(couleur?: string): string {
  return couleur && /^#[0-9a-f]{6}$/i.test(couleur) ? couleur : COULEUR_TACHE_PAR_DEFAUT;
}
