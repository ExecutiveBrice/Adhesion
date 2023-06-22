import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { ParamText } from '../models/paramText';


@Injectable()
export class ParamTransmissionService {

  // Observable string sources
  private dataSource = new Subject<ParamText[]>();

  // Observable string streams
  dataStream = this.dataSource.asObservable();

  // Service message commands
  dataTransmission(params: ParamText[]) {
    this.dataSource.next(params);
  }

}