import { Adherent, User } from '../../models';
import { UserComponent } from './user.component';

describe('Création d’un adhérent sans e-mail', () => {
  let adherent: Adherent;

  beforeEach(() => {
    adherent = new Adherent();
    adherent.user = new User();
    adherent.telephone = '0600000000';
    adherent.adresse = '1 rue du Test';
  });

  it('permet d’enregistrer un nouvel adhérent sans e-mail', () => {
    expect(UserComponent.prototype.isAdherentComplet(adherent)).toBeTruthy();
  });

  it('conserve les autres coordonnées obligatoires', () => {
    adherent.telephone = '';
    expect(UserComponent.prototype.isAdherentComplet(adherent)).toBeFalsy();
  });

  it('accepte le profil enregistré avec son identifiant technique', () => {
    adherent.id = 42;
    adherent.user.username = 'adherent-123@sans-email.invalid';
    expect(UserComponent.prototype.isAdherentComplet(adherent)).toBeTruthy();
  });
});
