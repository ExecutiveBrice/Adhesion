import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import {
  faBuildingColumns,
  faCalculator,
  faChalkboardUser,
  faKeyboard,
  faUserCheck,
  faUserShield,
  faUsers, faUsersGear, faUsersLine, faStreetView, faUserTie, faUserGear, faPeopleRoof, faStore
} from '@fortawesome/free-solid-svg-icons';
import { ERole } from '../models/eRole';

export interface RoleIcon {
  icon: IconDefinition;
  label: string;
}

export const ROLE_ICONS: Partial<Record<ERole, RoleIcon>> = {
  [ERole.ROLE_ADMIN]: { icon: faUserShield, label: 'Administrateur du site' },
  [ERole.ROLE_SECRETAIRE]: { icon: faUserGear, label: 'Secrétariat' },
  [ERole.ROLE_BUREAU]: { icon: faUsersLine, label: 'Bureau' },
  [ERole.ROLE_MEMBRECA]: { icon: faPeopleRoof, label: 'Membre du CA' },
  [ERole.ROLE_COMPTABLE]: { icon: faCalculator, label: 'Comptable' },
  [ERole.ROLE_ENCADRANT]: { icon: faUserTie, label: 'Encadrant' },
  [ERole.ROLE_REFERENT]: { icon: faUserCheck, label: 'Référent' },
  [ERole.ROLE_RESPONSABLE_BOUTIQUE]: { icon: faStore, label: 'Responsable boutique' }
};

export function roleIconFor(role: ERole): RoleIcon | undefined {
  return ROLE_ICONS[role];
}

export function displayedRolesFor(roles?: readonly ERole[] | null): ERole[] {
  if (!roles?.length) return [];
  return (Object.keys(ROLE_ICONS) as ERole[]).filter(role => roles.includes(role));
}
