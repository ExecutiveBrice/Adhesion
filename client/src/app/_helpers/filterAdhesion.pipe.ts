import {Pipe, PipeTransform} from '@angular/core'
import {Adhesion} from "../models";

@Pipe({
  standalone: true,
  name: 'filterAdhesionBy'
})
export class FilterAdhesionByPipe implements PipeTransform {


  transform(adhesions: Adhesion[], validPaiementSecretariat: boolean | null, validDocumentSecretariat: boolean | null, statutActuel: string, flag: boolean | null) {

    return adhesions.filter(adhesion => {


      if (flag != null && validPaiementSecretariat != null && validDocumentSecretariat != null && statutActuel != '') {
        return adhesion.flag == flag && adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.validDocumentSecretariat == validDocumentSecretariat && adhesion.statutActuel == statutActuel;
      }

      if (flag != null && validPaiementSecretariat != null && validDocumentSecretariat != null) {
        return adhesion.flag == flag && adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.validDocumentSecretariat == validDocumentSecretariat;
      }
      if (flag != null && validPaiementSecretariat != null && statutActuel != '') {
        return adhesion.flag == flag && adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.statutActuel == statutActuel;
      }
      if (flag != null && validDocumentSecretariat != null && statutActuel != '') {
        return adhesion.flag == flag && adhesion.validDocumentSecretariat == validDocumentSecretariat && adhesion.statutActuel == statutActuel;
      }
      if (validPaiementSecretariat != null && validDocumentSecretariat != null && statutActuel != '') {
        return adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.validDocumentSecretariat == validDocumentSecretariat && adhesion.statutActuel == statutActuel;
      }

      if (flag != null && validPaiementSecretariat != null) {
        return adhesion.flag == flag && adhesion.validPaiementSecretariat == validPaiementSecretariat;
      }
      if (flag != null && validDocumentSecretariat != null) {
        return adhesion.flag == flag && adhesion.validDocumentSecretariat == validDocumentSecretariat;
      }
      if (flag != null && statutActuel != '') {
        return adhesion.flag == flag && adhesion.statutActuel == statutActuel;
      }

      if (validDocumentSecretariat != null && statutActuel != '') {
        return adhesion.validDocumentSecretariat == validDocumentSecretariat && adhesion.statutActuel == statutActuel;
      }
      if (validPaiementSecretariat != null && statutActuel != '') {
        return adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.statutActuel == statutActuel;
      }
      if (validPaiementSecretariat != null && validDocumentSecretariat != null) {
        return adhesion.validPaiementSecretariat == validPaiementSecretariat && adhesion.validDocumentSecretariat == validDocumentSecretariat;
      }

      if (flag != null) {
        return adhesion.flag == flag;
      }
      if (statutActuel != '') {
        return adhesion.statutActuel == statutActuel;
      }
      if (validDocumentSecretariat != null) {
        return adhesion.validDocumentSecretariat == validDocumentSecretariat;
      }
      if (validPaiementSecretariat != null) {
        return adhesion.validPaiementSecretariat == validPaiementSecretariat;
      }

      return true;
    })

  }
}
