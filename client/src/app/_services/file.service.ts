import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { environment } from 'src/environments/environment';

@Injectable()
export class FileService {
  apiUrl = environment.server + '/files';

  constructor(
    private http: HttpClient
  ) { }

  update(adherentId: number, fileName: string, fileContent: any) {
    let params = new HttpParams().set('adherentId', '' + adherentId + '').set('fileName', '' + fileName + '');
    const fd = new FormData();
    const file = new File([fileContent], fileName);
    fd.append('image', file)
    return this.http.post(this.apiUrl + '/', fileContent, { params, responseType: 'text' });
  }

  get(adherentId: number, fileName: string) {
    let params = new HttpParams().set('adherentId', '' + adherentId + '').set('fileName', '' + fileName + '');
    return this.http.get(this.apiUrl + '/', { params, responseType: 'text' });
  }

  getAllFilesName(adherentId: number) {
    let params = new HttpParams().set('adherentId', '' + adherentId + '');
    return this.http.get<string[]>(this.apiUrl + '/allFilesName', { params, responseType: 'json' });
  }


  delete(adherentId: number, fileName: string) {
    let params = new HttpParams().set('adherentId', '' + adherentId + '').set('fileName', '' + fileName + '');
    return this.http.delete(this.apiUrl + '/', { params, responseType: 'text' });
  }

}
