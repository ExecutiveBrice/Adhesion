export interface SectionConfiguration {
  id?: number;
  nom: string;
  type: 'COMPETITION' | 'NON_COMPETITIVE';
  couleur?: string;
  referentUserIds?: number[];
}
