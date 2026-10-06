import { AgendaGoogleConfiguration } from './agendaGoogle';
import { ResponsabiliteSeance } from './responsabiliteSeance';
import { EvenementGoogleAgenda, PresencePrevue, SeanceCalendrier } from './seance';

/** Données de détail communes aux événements de l'accueil et de l'agenda. */
export interface Evenement {
  id: string;
  titre: string;
  source: 'SEANCE' | 'GOOGLE';
  debut: string;
  fin: string;
  journeeEntiere: boolean;
  lieu: string | null;
  adresseSalle?: string | null;
  couleurSalle?: string | null;
  commentaire?: string | null;
  lien?: string | null;
  agenda?: string;
  agendaSource?: string;
  seanceId?: number;
  activiteNom?: string;
  descriptif?: string | null;
  horaireActivite?: string;
  sectionId?: number;
  sectionNom?: string | null;
  sectionType?: string | null;
  couleurSection?: string | null;
  etatSeance?: SeanceCalendrier['etatSeance'];
  causeAnnulation?: string | null;
  personnelle?: boolean;
  responsabilites?: ResponsabiliteSeance[];
  presences?: PresencePrevue[];
  chargementPresences?: boolean;
  enregistrementPresence?: boolean;
  erreurPresence?: string;
}

export function evenementSeance(s: SeanceCalendrier, personnelle: boolean): Evenement {
  return {
    id: `seance-${s.id}`, source: 'SEANCE', seanceId: s.id, personnelle,
    titre: s.activiteNom, activiteNom: s.activiteNomCourt, descriptif: s.descriptif,
    horaireActivite: s.horaireActivite, debut: s.debut, fin: s.fin, journeeEntiere: false,
    lieu: s.salle, adresseSalle: s.adresseSalle, couleurSalle: s.couleurSalle,
    commentaire: s.commentaire, lien: s.lien, etatSeance: s.etatSeance, causeAnnulation: s.causeAnnulation,
    sectionId: s.sectionId ?? undefined, sectionNom: s.sectionNom, sectionType: s.sectionType,
    couleurSection: s.couleurSection, responsabilites: s.responsabilites ?? []
  };
}

export function evenementGoogle(e: EvenementGoogleAgenda, agendas: AgendaGoogleConfiguration[]): Evenement {
  return {
    ...e, id: `google-${e.agendaSource}-${e.id}-${e.debut}`, source: 'GOOGLE',
    agenda: agendas.find(a => a.source === e.agendaSource)?.nom || e.agenda
  };
}
