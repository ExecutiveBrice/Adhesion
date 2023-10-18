import { Pipe, PipeTransform } from '@angular/core'
import { Activite } from '../models'

@Pipe({
  name: 'filterBy'
})
export class FilterByPipe implements PipeTransform {



  transform(array: any[], query: Map<string, string>, excludeProps?: string | string[], dateFormat?: string) {

    const operation = (list1: any[], list2: any[], isUnion = false) =>
      list1.filter(a => isUnion === list2.some(b => a.id == b.id));
    const inPool = (list1: any[], list2: any[]) => operation(list1, list2, false)
    const inBoth = (list1: any[], list2: any[]) => operation(list1, list2, true)
    let returnArray: any[] = []
    let lists: Map<string, any[]> = new Map<string, any[]>();


    if (query.size >= 1) {
      for (let [queryKey, value] of query) {
        console.log(queryKey.split(".")[0])
        let tempList = array.filter((item: any) => {

          console.log(item)

          return item[queryKey.split(".")[0]].toLowerCase().includes(value.toLowerCase())
        });

        returnArray = inBoth(returnArray, tempList)
      }



      return returnArray
    } else {
      return array
    }


  }



}