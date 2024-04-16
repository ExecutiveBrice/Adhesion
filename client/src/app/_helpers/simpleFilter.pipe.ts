import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'simplefilter'
})
export class SimpleFilterPipe implements PipeTransform {
    transform(items: any[], key:string, values:string[]): any {
        if (!key || !values) {
            return items;
        }
        // filter items array, items which match and return true will be
        // kept, false will be filtered out
        return items.filter(item => values.filter(value => value == item[key]).length == 0);
    }
}