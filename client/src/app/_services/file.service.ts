import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from "@angular/common/http";
import { environment } from 'src/environments/environment';

@Injectable({ providedIn: 'root' })
export class FileService {
  private http = inject(HttpClient);

  apiUrl = environment.server + '/files';

  update(adherentId: number, file: File) {
    const params = new HttpParams().set('adherentId', adherentId);
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post(this.apiUrl + '/', formData, { params, responseType: 'text' });
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
