export interface ChatRolePermission {
  role: string;
  ecriture: boolean;
}

export interface ChatConfiguration {
  id?: number;
  nom: string;
  cible: 'SECTION' | 'ACTIVITE' | 'ASSOCIATION';
  cibleId?: number;
  cibleNom?: string;
  permissions: ChatRolePermission[];
}
