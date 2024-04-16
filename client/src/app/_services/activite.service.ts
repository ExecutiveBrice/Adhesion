import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Activite, Adherent, ActiviteDropDown, HoraireDropDown } from '../models';

const API_URL = environment.server + '/activite/';

@Injectable({
  providedIn: 'root'
})
export class ActiviteService {
  constructor(private http: HttpClient) { }

  getAll(): Observable<Activite[]> {
    return this.http.get<Activite[]>(API_URL + 'all', { responseType: 'json' });
  }

  addReferent(activiteId: number, adherentId: string): Observable<Activite[]> {
    let params = new HttpParams().set('activiteId', '' + activiteId + '').set('adherentId', '' + adherentId + '');
    return this.http.get<Activite[]>(API_URL + "addReferent", { params, responseType: 'json' });
  }

  save(activite: Activite): Observable<Activite> {
    return this.http.post<Activite>(API_URL + 'save', activite, { responseType: 'json' });
  }





  fillObjects(activites: Activite[], activitesListe: ActiviteDropDown[], adherent?: Adherent) {
    let age =0
    if (adherent != undefined){
    let anneeRef = new Date().getFullYear()
    let anneeAdh = new Date(adherent.naissance).getFullYear()
    age = anneeRef - anneeAdh
  }

    this.getAll().subscribe(
      data => {
        console.log(data)

        data.forEach(act => {
          activites.push(act)

            if (adherent == undefined || (act.ageMax >= age && act.ageMin <= age && (act.genre == "Non genrée" || act.genre.startsWith(adherent.genre)))) {

              let activiteDropDown: ActiviteDropDown;
              if (activitesListe.filter(activiteDropDown => activiteDropDown.nom == act.nom).length > 0) {
                activiteDropDown = activitesListe.filter(activiteDropDown => activiteDropDown.nom == act.nom)[0]
              } else {
                activiteDropDown = new ActiviteDropDown()
                activiteDropDown.nom = act.nom
                activitesListe.push(activiteDropDown)
              }

              activitesListe.forEach(activite => activite.horaires.forEach(horaire => {
                adherent?.adhesions.forEach(adhesion => {
                  if (adhesion.activite?.id == horaire.id) {
                    horaire.dejaInscrit = true;
                  }
                })
              }))
              
              let horaireDropDown = new HoraireDropDown
              horaireDropDown.id = act.id
              horaireDropDown.nom = act.horaire
              if(!act.reinscription){
                horaireDropDown.reinscription = act.reinscription;
              }else if(adherent != undefined ){
                horaireDropDown.reinscription =  !(adherent.activitesNm1.find(activiteNm1 => activiteNm1.nom == activiteDropDown.nom) != undefined)
              }
              horaireDropDown.complete = act.complete
              activiteDropDown.horaires.push(horaireDropDown)

            }
         
        });

      },
      error => {
        console.log(error)
      }
    );
  }
}


