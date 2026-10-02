export interface SectionConfiguration {
  id?: number;
  nom: string;
  type: 'COMPETITION' | 'NON_COMPETITIVE';
}

export interface SectionChatRolePermission {
  role: string;
  ecriture: boolean;
}

export interface SectionChatConfiguration {
  id?: number;
  nom: string;
  cible: 'SECTION' | 'ACTIVITE' | 'ASSOCIATION';
  cibleId?: number;
  cibleNom?: string;
  permissions: SectionChatRolePermission[];
}
