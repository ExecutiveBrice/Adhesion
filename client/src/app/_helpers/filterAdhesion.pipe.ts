import { Pipe, PipeTransform } from '@angular/core'

@Pipe({
  name: 'filterAdhesionBy'
})
export class FilterAdhesionByPipe implements PipeTransform {



  transform(array: any[], query: Map<string, string>, excludeProps?: string | string[], dateFormat?: string) {

    if (query.size >= 1) {
      for (let [queryKey, value] of query) {

        array = array.filter(item => {

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

      }
    } 
    console.log(array)

    return array

  }



}