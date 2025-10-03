import {Pipe, PipeTransform} from '@angular/core'

@Pipe({
  standalone: true,
  name: 'orderSimpleBy'
})
export class OrderSimplePipe implements PipeTransform {
  transform(values: object[], key: string, reverse: boolean) {
    if (!Array.isArray(values) || values.length <= 0) {
      return null;
    }

    const array: any[] = values.sort((a: any, b: any): number => {
        return a[key].toLowerCase() > b[key].toLowerCase() ? 1 : -1;
    });

    if (reverse) {
      return array.reverse();
    }

    return array;
  }


}
