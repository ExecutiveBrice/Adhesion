import { Pipe, PipeTransform } from '@angular/core'
import { Activite } from '../models'

@Pipe({
  name: 'filterAdhesionBy'
})
export class FilterAdhesionByPipe implements PipeTransform {



  transform(array: any[], query: Map<string, string>, excludeProps?: string | string[], dateFormat?: string) {

    const operation = (list1: any[], list2: any[], isUnion = false) =>
      list1.filter(a => isUnion === list2.some(b => a.id == b.id));
      const inPool = (list1: any[], list2: any[]) => operation(list1, list2, false)
    const inBoth = (list1: any[], list2: any[]) => operation(list1, list2, true)
    let returnArray: any[] = []
    let lists: Map<string, any[]> = new Map<string, any[]>();


    if (query.size >= 1) {
      for (let [queryKey, value] of query) {

        let tempList = array.filter(item => {

          if (queryKey.split(".").length == 1) {
            if (typeof item[queryKey.split(".")[0]] == 'string' && item[queryKey.split(".")[0]].toLowerCase().includes(value.toLowerCase())) {
              return item
            }

            if (typeof item[queryKey.split(".")[0]] == 'boolean' && item[queryKey.split(".")[0]] == value) {
              return item
            }
          } else {
           
            if (typeof item[queryKey.split(".")[0]][queryKey.split(".")[1]] == 'string' && item[queryKey.split(".")[0]][queryKey.split(".")[1]].toLowerCase().includes(value.toLowerCase())) {
              return item
            }
          }
        })

        let lastList = lists.get(queryKey.split(".")[0]);

        if (lastList != undefined) {
          lists.delete(queryKey.split(".")[0])
          tempList.forEach(e => lastList?.push(e))
          lists.set(queryKey.split(".")[0], lastList)
        } else {
          lists.set(queryKey.split(".")[0], tempList)
        }
      }


      for (let [key, list] of lists) {
        if (returnArray.length == 0) {
          returnArray = list
        } else {
          returnArray = inBoth(returnArray, list)
        }
      }

      return returnArray
    } else {
      return array
    }


  }



}