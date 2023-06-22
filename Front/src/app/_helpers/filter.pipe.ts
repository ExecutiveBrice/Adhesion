import { Pipe, PipeTransform } from '@angular/core'
import { Activite } from '../models'

@Pipe({
  name: 'filterBy'
})
export class FilterByPipe implements PipeTransform {


  transform(array: any[], query: string, excludeProps?: string | string[], dateFormat?: string) {
    return array.filter(item => {
      for (let key in item) {

        if (typeof query == 'number' && typeof item[key] == 'number' && item[key] == query) {
          return item
        }
        if (typeof query == 'string' && typeof item[key] == 'string' && item[key].toLowerCase().includes(query.toLowerCase())) {
          return item
        }
        if (key == "adhesions" && item[key].length > 0 ) {

          for (let i in item[key]) {
            if (item[key][i].activite && item[key][i].activite.nom.toLowerCase().includes(query.toLowerCase() || item[key][i].activite.horaire.toLowerCase().includes(query.toLowerCase()))) {
              return item
            }
          }
        }

        if (key == "activite") {
          for (let i in item[key]) {
            if (item[key][i] && typeof query == 'string' && typeof item[key][i] == 'string' &&  item[key][i].toLowerCase().includes(query.toLowerCase())) {
              return item
            }
          }
        }

        if (key == "adherent") {
          for (let i in item[key]) {
            console.log(item[key][i])
            if (item[key][i] && typeof query == 'string' && typeof item[key][i] == 'string' &&  item[key][i].toLowerCase().includes(query.toLowerCase())) {
              return item
            }
          }
        }
      }
    }
    )
  }


  getObject(theObject: any, query: string): any {
    var result = false;
    if (theObject instanceof Array) {
      for (var i = 0; i < theObject.length; i++) {
        result = this.getObject(theObject[i], query);
        if (result) {
          break;
        }
      }
    }
    else {
      for (var prop in theObject) {


        if (theObject[prop].includes(query)) {
          return true;
        }

        if (theObject[prop] instanceof Object || theObject[prop] instanceof Array) {
          result = this.getObject(theObject[prop], query);
          if (result) {
            break;
          }
        }
      }
    }
    return result;
  }

}